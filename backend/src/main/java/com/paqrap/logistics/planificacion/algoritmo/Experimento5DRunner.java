package com.paqrap.logistics.planificacion.algoritmo;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.almacen.model.AlmacenCentral;
import com.paqrap.logistics.almacen.model.AlmacenIntermedio;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.TipoVehiculo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.planificacion.model.ParadaRuta;
import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.redvial.model.Bloqueo;
import com.paqrap.logistics.redvial.model.RedVial;
import com.paqrap.logistics.redvial.model.Ubicacion;
import com.paqrap.logistics.simulacion.model.CargadorArchivos;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Ejecutor automatizado por lotes para el Diseño de Experimentos 5D (ACO vs ALNS).
 * Ejecuta de forma pareada las 6 combinaciones (Volumen x Disrupción) con n réplicas
 * usando semillas estocásticas idénticas y escribe la plantilla oficial de resultados CSV.
 */
public class Experimento5DRunner {

    public enum NivelVolumen {
        BAJO("bajo", "202601", 76),
        MEDIO("medio", "202605", 369),
        ALTO("alto", "202609", 609);

        public final String etiqueta;
        public final String mesArchivo;
        public final int maxPedidos5D;

        NivelVolumen(String etiqueta, String mesArchivo, int maxPedidos5D) {
            this.etiqueta = etiqueta;
            this.mesArchivo = mesArchivo;
            this.maxPedidos5D = maxPedidos5D;
        }
    }

    public enum NivelDisrupcion {
        BAJO("bajo", false),
        ALTO("alto", true);

        public final String etiqueta;
        public final boolean conAverias;

        NivelDisrupcion(String etiqueta, boolean conAverias) {
            this.etiqueta = etiqueta;
            this.conAverias = conAverias;
        }
    }

    public static class ResultadoFila {
        public String volumen;
        public String disrupcion;
        public String algoritmo;
        public int replica;
        public long semilla;
        public int pedidosTotales;
        public int pedidosAtendidos;
        public int pedidosATiempo;
        public double pctCumplimientoGlobal;
        public double costoOperativoTotal;
        public double saturacionAlmacenesPct;
        public long tiempoEjecucionMs;

        public String toCsvLine() {
            return String.format(Locale.US,
                    "%s,%s,%s,%d,%d,%d,%d,%d,%.2f,%.2f,%.2f,%d",
                    volumen, disrupcion, algoritmo, replica, semilla,
                    pedidosTotales, pedidosAtendidos, pedidosATiempo,
                    pctCumplimientoGlobal, costoOperativoTotal,
                    saturacionAlmacenesPct, tiempoEjecucionMs);
        }
    }

    public static void main(String[] args) {
        int replicas = 30;
        String archivoSalida = "plantilla_resultados_5d.csv";
        boolean modoPiloto = false;
        boolean ventanaFija = true; // Por defecto: condiciones idénticas días 1 al 5

        for (String arg : args) {
            if (arg.startsWith("--replicas=")) {
                try {
                    replicas = Integer.parseInt(arg.substring(11).trim());
                } catch (Exception ignored) {}
            } else if (arg.startsWith("--output=") || arg.startsWith("--csv=")) {
                archivoSalida = arg.substring(arg.indexOf('=') + 1).trim();
            } else if ("--piloto".equalsIgnoreCase(arg) || "piloto".equalsIgnoreCase(arg)) {
                replicas = 3;
                modoPiloto = true;
            } else if ("--deslizante".equalsIgnoreCase(arg) || "deslizante".equalsIgnoreCase(arg)) {
                ventanaFija = false;
            } else if ("--fija".equalsIgnoreCase(arg) || "fija".equalsIgnoreCase(arg)) {
                ventanaFija = true;
            }
        }

        System.out.println("================================================================================");
        System.out.println("     PAQRAP: EJECUTOR DEL DISEÑO DE EXPERIMENTOS (SIMULACIÓN 5D)              ");
        System.out.println("     Metaheurísticas evaluadas: ACO vs. ALNS (Diseño Pareado)                 ");
        System.out.println("================================================================================");
        System.out.println("Modo de ventana 5D       : " + (ventanaFija ? "Condiciones Iguales (Días 1 al 5 Fijos)" : "Ventanas Deslizantes"));
        System.out.println("Réplicas por combinación : " + replicas);
        System.out.println("Combinaciones a evaluar  : 6 (3 Volúmenes x 2 Disrupciones)");
        System.out.println("Total de instancias      : " + (6 * replicas));
        System.out.println("Total corridas (filas)   : " + (6 * replicas * 2));
        System.out.println("Archivo CSV de salida    : " + archivoSalida);
        System.out.println("================================================================================\n");

        File dirDatos = resolverDirectorioDatos();
        CargadorArchivos cargador = new CargadorArchivos();

        List<ResultadoFila> resultados = Collections.synchronizedList(new ArrayList<>());
        long tInicioTotal = System.currentTimeMillis();

        NivelVolumen[] volumnes = NivelVolumen.values();
        NivelDisrupcion[] disrupciones = NivelDisrupcion.values();

        int combIdx = 0;
        for (NivelVolumen vol : volumnes) {
            for (NivelDisrupcion dis : disrupciones) {
                combIdx++;
                System.out.printf("[%d/6] INICIANDO BLOQUE: Volumen=%s (%d pedidos) | Disrupción=%s (averías=%b)%n",
                        combIdx, vol.etiqueta.toUpperCase(), vol.maxPedidos5D, dis.etiqueta.toUpperCase(), dis.conAverias);

                // Pre-cargar pedidos y bloqueos del mes en memoria
                List<Pedido> pedidosMesCompleto = cargarPedidosMes(dirDatos, vol.mesArchivo, cargador);

                String archivoBloqueo = dis == NivelDisrupcion.ALTO
                        ? "bloqueo.2610.txt" // Pico de bloqueos (alta disrupción)
                        : "bloqueo." + vol.mesArchivo.substring(2) + ".txt"; // Bloqueos nominales del mes

                List<Bloqueo> bloqueosMesCompleto = cargarBloqueosMes(dirDatos, archivoBloqueo, cargador);

                for (int rep = 1; rep <= replicas; rep++) {
                    long semilla = rep; // Semillas deterministas 1..replicas

                    // Ventana de 5 días: condiciones idénticas fijas (días 1 al 5) o deslizante
                    int diaInicio = ventanaFija ? 1 : (1 + ((rep - 1) % 25));
                    int horaInicio = (ventanaFija || rep <= 25) ? 0 : 12;

                    List<Pedido> pedidosInstancia = filtrarPedidosVentana(pedidosMesCompleto, vol.mesArchivo, diaInicio, horaInicio, vol.maxPedidos5D);
                    List<Bloqueo> bloqueosInstancia = filtrarBloqueosVentana(bloqueosMesCompleto, vol.mesArchivo, diaInicio, horaInicio);

                    // 1. Ejecutar ACO
                    ResultadoFila rAco = ejecutarAlgoritmo("aco", vol, dis, rep, semilla,
                            pedidosInstancia, bloqueosInstancia, dis.conAverias);
                    resultados.add(rAco);

                    // 2. Ejecutar ALNS sobre exactamente la misma instancia y semilla
                    ResultadoFila rAlns = ejecutarAlgoritmo("alns", vol, dis, rep, semilla,
                            pedidosInstancia, bloqueosInstancia, dis.conAverias);
                    resultados.add(rAlns);

                    System.out.printf("   [Rép %02d/%02d | Sem %d | Ventana d%02d+%02dh | %d ped] ACO: Cumpl=%5.1f%%, Costo=S/%7.2f, T=%4dms | ALNS: Cumpl=%5.1f%%, Costo=S/%7.2f, T=%4dms%n",
                            rep, replicas, semilla, diaInicio, horaInicio, pedidosInstancia.size(),
                            rAco.pctCumplimientoGlobal, rAco.costoOperativoTotal, rAco.tiempoEjecucionMs,
                            rAlns.pctCumplimientoGlobal, rAlns.costoOperativoTotal, rAlns.tiempoEjecucionMs);
                }
                System.out.println();
            }
        }

        // Guardar archivo CSV
        escribirResultadosCsv(archivoSalida, resultados);

        long tFinTotal = System.currentTimeMillis();
        double duracionSeg = (tFinTotal - tInicioTotal) / 1000.0;

        System.out.println("================================================================================");
        System.out.printf("EXPERIMENTACIÓN COMPLETADA con éxito en %.1f segundos (%d filas generadas).%n",
                duracionSeg, resultados.size());
        System.out.println("Resultados guardados en: " + new File(archivoSalida).getAbsolutePath());
        System.out.println("================================================================================");
    }

    private static ResultadoFila ejecutarAlgoritmo(String tipoAlgoritmo,
                                                  NivelVolumen vol,
                                                  NivelDisrupcion dis,
                                                  int replica,
                                                  long semilla,
                                                  List<Pedido> pedidosBase,
                                                  List<Bloqueo> bloqueosBase,
                                                  boolean conAverias) {
        // Red vial independiente
        RedVial red = new RedVial();
        red.inicializarRed();
        for (Bloqueo b : bloqueosBase) {
            red.aplicarBloqueo(b);
        }

        // Flota base
        List<UnidadTransporte> flota = crearFlota(conAverias);

        // Clones de pedidos para evitar colisiones
        List<Pedido> pedidos = new ArrayList<>(pedidosBase);

        ResultadoFila fila = new ResultadoFila();
        fila.volumen = vol.etiqueta;
        fila.disrupcion = dis.etiqueta;
        fila.algoritmo = tipoAlgoritmo;
        fila.replica = replica;
        fila.semilla = semilla;
        fila.pedidosTotales = pedidos.size();

        long tInicio = System.currentTimeMillis();
        List<Ruta> rutas;

        if ("aco".equalsIgnoreCase(tipoAlgoritmo)) {
            AlgoritmoACO aco = new AlgoritmoACO();
            aco.setSemilla(semilla);
            rutas = aco.construirSolucion(pedidos, flota, red);
        } else {
            AlgoritmoALNS alns = new AlgoritmoALNS();
            alns.setSemilla(semilla);
            rutas = alns.construirSolucion(pedidos, flota, red);
        }
        long tFin = System.currentTimeMillis();
        fila.tiempoEjecucionMs = (tFin - tInicio);

        // Cálculo de métricas
        int pedidosAtendidos = 0;
        int aTiempo = 0;
        double costoTotal = 0.0;
        int demandaAlmacenesIntermedios = 0;

        if (rutas != null) {
            for (Ruta r : rutas) {
                costoTotal += r.getCostoTotal();
                if (r.getAlmacenOrigen() instanceof AlmacenIntermedio) {
                    demandaAlmacenesIntermedios += r.getParadas().stream()
                            .mapToInt(p -> p.getPedido() != null ? p.getPedido().getCantidadUnidades() : 0).sum();
                }
                for (ParadaRuta p : r.getParadas()) {
                    if (p.getPedido() != null) {
                        pedidosAtendidos++;
                        if (p.getHoraEstimadaLlegada() != null && p.getPedido().getPlazoLimiteEntrega() != null) {
                            if (!p.getHoraEstimadaLlegada().isAfter(p.getPedido().getPlazoLimiteEntrega())) {
                                aTiempo++;
                            }
                        } else {
                            aTiempo++;
                        }
                    }
                }
            }
        }

        fila.pedidosAtendidos = pedidosAtendidos;
        fila.pedidosATiempo = aTiempo;
        // Definición estricta de SLA (Sección 3.4): aTiempo / pedidosTotales
        fila.pctCumplimientoGlobal = fila.pedidosTotales > 0
                ? (aTiempo * 100.0) / fila.pedidosTotales
                : 100.0;
        fila.costoOperativoTotal = Math.round(costoTotal * 100.0) / 100.0;
        // Capacidad total de almacenes intermedios = 2 x 1000 = 2000 unidades
        fila.saturacionAlmacenesPct = Math.round((demandaAlmacenesIntermedios * 100.0 / 2000.0) * 100.0) / 100.0;

        return fila;
    }

    private static List<UnidadTransporte> crearFlota(boolean conAverias) {
        List<UnidadTransporte> flota = new ArrayList<>();
        TipoVehiculo auto = TipoVehiculo.builder().id(1L).nombre("Auto").capacidadMaxima(24).velocidadPromedioKmH(20.0).costoPorKm(8.0).build();
        TipoVehiculo moto = TipoVehiculo.builder().id(2L).nombre("Moto").capacidadMaxima(8).velocidadPromedioKmH(40.0).costoPorKm(6.0).build();
        TipoVehiculo bici = TipoVehiculo.builder().id(3L).nombre("Bicicleta").capacidadMaxima(4).velocidadPromedioKmH(14.0).costoPorKm(3.0).build();

        long id = 1;
        // 4 Autos
        for (int i = 1; i <= 4; i++) {
            String codigo = String.format("TA%02d", i);
            boolean averiado = conAverias && "TA02".equals(codigo);
            flota.add(UnidadTransporte.builder()
                    .id(id++)
                    .codigo(codigo)
                    .tipo(auto)
                    .estadoOperativo(averiado ? EstadoOperativo.AVERIADA : EstadoOperativo.DISPONIBLE)
                    .ubicacionActual(new Ubicacion(27, 14))
                    .activo(!averiado)
                    .build());
        }

        // 3 Motos
        for (int i = 1; i <= 3; i++) {
            String codigo = String.format("TM%02d", i);
            boolean averiada = conAverias && "TM01".equals(codigo);
            flota.add(UnidadTransporte.builder()
                    .id(id++)
                    .codigo(codigo)
                    .tipo(moto)
                    .estadoOperativo(averiada ? EstadoOperativo.AVERIADA : EstadoOperativo.DISPONIBLE)
                    .ubicacionActual(new Ubicacion(27, 14))
                    .activo(!averiada)
                    .build());
        }

        // 3 Bicis
        for (int i = 1; i <= 3; i++) {
            String codigo = String.format("TB%02d", i);
            flota.add(UnidadTransporte.builder()
                    .id(id++)
                    .codigo(codigo)
                    .tipo(bici)
                    .estadoOperativo(EstadoOperativo.DISPONIBLE)
                    .ubicacionActual(new Ubicacion(27, 14))
                    .activo(true)
                    .build());
        }

        return flota;
    }

    private static List<Pedido> cargarPedidosMes(File dirDatos, String mes, CargadorArchivos cargador) {
        File vFile = buscarArchivo(dirDatos, "ventas." + mes + ".txt");
        if (vFile == null) {
            vFile = new File(dirDatos, "ventas.v20260909/ventas." + mes + ".txt");
        }
        return cargador.cargarPedidos(vFile.getAbsolutePath());
    }

    private static List<Bloqueo> cargarBloqueosMes(File dirDatos, String nombreBloqueo, CargadorArchivos cargador) {
        File bFile = buscarArchivo(dirDatos, nombreBloqueo);
        if (bFile == null) {
            bFile = new File(dirDatos, "bloqueos/" + nombreBloqueo);
        }
        if (!bFile.exists()) return new ArrayList<>();
        return cargador.cargarBloqueos(bFile.getAbsolutePath());
    }

    private static List<Pedido> filtrarPedidosVentana(List<Pedido> pedidosMes, String mes, int diaInicio, int horaInicio, int maxPedidos) {
        int anio = Integer.parseInt(mes.substring(0, 4));
        int m = Integer.parseInt(mes.substring(4, 6));
        LocalDateTime inicioVentana = LocalDateTime.of(anio, m, diaInicio, horaInicio, 0);
        LocalDateTime finVentana = inicioVentana.plusDays(5);

        List<Pedido> filtrados = new ArrayList<>();
        for (Pedido p : pedidosMes) {
            if (p.getFechaHoraRegistro() == null) continue;
            if (!p.getFechaHoraRegistro().isBefore(inicioVentana) && p.getFechaHoraRegistro().isBefore(finVentana)) {
                filtrados.add(p);
                if (maxPedidos > 0 && filtrados.size() >= maxPedidos) break;
            }
        }
        return filtrados;
    }

    private static List<Bloqueo> filtrarBloqueosVentana(List<Bloqueo> bloqueosMes, String mes, int diaInicio, int horaInicio) {
        int anio = Integer.parseInt(mes.substring(0, 4));
        int m = Integer.parseInt(mes.substring(4, 6));
        LocalDateTime inicioVentana = LocalDateTime.of(anio, m, diaInicio, horaInicio, 0);
        LocalDateTime finVentana = inicioVentana.plusDays(5);

        List<Bloqueo> filtrados = new ArrayList<>();
        for (Bloqueo b : bloqueosMes) {
            if (b.getFechaHoraInicio() != null && b.getFechaHoraFin() != null) {
                if (b.getFechaHoraInicio().isBefore(finVentana) && b.getFechaHoraFin().isAfter(inicioVentana)) {
                    filtrados.add(b);
                }
            }
        }
        return filtrados;
    }

    private static void escribirResultadosCsv(String rutaSalida, List<ResultadoFila> filas) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(rutaSalida))) {
            pw.println("volumen,disrupcion,algoritmo,replica,semilla,pedidos_totales,pedidos_atendidos,pedidos_a_tiempo,pct_cumplimiento_global,costo_operativo_total,saturacion_almacenes_pct,tiempo_ejecucion_ms");
            for (ResultadoFila f : filas) {
                pw.println(f.toCsvLine());
            }
        } catch (Exception e) {
            System.err.println("Error al guardar CSV de resultados: " + e.getMessage());
        }
    }

    private static File resolverDirectorioDatos() {
        File dir = new File("datos");
        if (dir.exists()) return dir;
        dir = new File("/app/datos");
        if (dir.exists()) return dir;
        dir = new File("../datos");
        if (dir.exists()) return dir;
        return new File("datos");
    }

    private static File buscarArchivo(File dir, String nombre) {
        if (dir == null || !dir.exists()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isFile() && f.getName().equalsIgnoreCase(nombre)) {
                return f;
            } else if (f.isDirectory() && !f.getName().startsWith(".")) {
                File found = buscarArchivo(f, nombre);
                if (found != null) return found;
            }
        }
        return null;
    }
}
