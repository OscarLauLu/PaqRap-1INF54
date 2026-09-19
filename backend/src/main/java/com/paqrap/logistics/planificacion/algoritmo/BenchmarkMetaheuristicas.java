package com.paqrap.logistics.planificacion.algoritmo;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Runner ejecutable independiente para realizar benchmarks y pruebas comparativas
 * de AlgoritmoACO vs AlgoritmoALNS utilizando los datos reales de prueba (RF-06).
 */
public class BenchmarkMetaheuristicas {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("     BENCHMARK DE ALGORITMOS METAHEURÍSTICOS: ACO vs. ALNS (PaqRap)        ");
        System.out.println("================================================================================");

        File dirDatos = new File("datos");
        if (!dirDatos.exists()) {
            dirDatos = new File("/app/datos");
        }
        if (!dirDatos.exists()) {
            dirDatos = new File("../datos");
        }
        String datosDir = dirDatos.getAbsolutePath();

        // Búsqueda dinámica de mantenimiento
        File fMant = buscarArchivo(dirDatos, "mant.preventivo.09.10.txt");
        if (fMant == null) {
            File[] posibles = dirDatos.listFiles((d, n) -> n.toLowerCase().startsWith("mant") && n.endsWith(".txt"));
            if (posibles != null && posibles.length > 0) fMant = posibles[0];
        }
        String rutaMantenimiento = fMant != null ? fMant.getAbsolutePath() : datosDir + "/mant.preventivo.09.10.txt";

        String rutaVentas = "";
        String rutaBloqueos = "";
        int maxPedidos = 0;

        Integer customAutos = null;
        Integer customMotos = null;
        Integer customBicis = null;

        for (String arg : args) {
            if (arg.startsWith("--flota=")) {
                String[] fParts = arg.substring(8).split(",");
                if (fParts.length >= 3) {
                    try {
                        customAutos = Integer.parseInt(fParts[0].trim());
                        customMotos = Integer.parseInt(fParts[1].trim());
                        customBicis = Integer.parseInt(fParts[2].trim());
                    } catch (Exception ignored) {}
                }
            } else if (arg.startsWith("--mant=")) {
                rutaMantenimiento = arg.substring(7).trim();
            }
        }

        if (args.length > 0 && !args[0].trim().isEmpty() && !args[0].startsWith("--")) {
            if (new File(args[0]).exists()) {
                rutaVentas = args[0];
            } else if (args[0].matches("\\d{6}")) {
                String mesStr = args[0];
                File vFile = buscarArchivo(dirDatos, "ventas." + mesStr + ".txt");
                rutaVentas = vFile != null ? vFile.getAbsolutePath() : datosDir + "/ventas.v20260909/ventas." + mesStr + ".txt";

                String sufijoBloqueo = mesStr.substring(2);
                File bFile = buscarArchivo(dirDatos, "bloqueo." + sufijoBloqueo + ".txt");
                rutaBloqueos = bFile != null ? bFile.getAbsolutePath() : datosDir + "/bloqueos/bloqueo." + sufijoBloqueo + ".txt";
            }
        }
        if (rutaVentas.isEmpty()) {
            File vDefault = buscarArchivo(dirDatos, "ventas.202601.txt");
            rutaVentas = vDefault != null ? vDefault.getAbsolutePath() : datosDir + "/ventas.v20260909/ventas.202601.txt";
            File bDefault = buscarArchivo(dirDatos, "bloqueo.2601.txt");
            rutaBloqueos = bDefault != null ? bDefault.getAbsolutePath() : datosDir + "/bloqueos/bloqueo.2601.txt";
        }
        if (args.length > 1 && !args[1].trim().isEmpty() && !args[1].startsWith("--") && new File(args[1]).exists()) {
            rutaBloqueos = args[1];
        }
        int diaInicio = 1;
        int diasSimulacion = 0; // 0 = sin límite por días

        // 1. Extraer flags explícitas
        for (String a : args) {
            String alow = a.trim().toLowerCase();
            if (alow.startsWith("--inicio=") || alow.startsWith("--desde=") || alow.startsWith("--dia=")) {
                try {
                    diaInicio = Integer.parseInt(alow.substring(alow.indexOf('=') + 1).replaceAll("\\D", ""));
                } catch (Exception ignored) {}
            } else if (alow.matches("--dias=(\\d+)") || alow.matches("--(\\d+)d")) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(alow);
                if (m.find()) {
                    diasSimulacion = Integer.parseInt(m.group(1));
                    maxPedidos = 0;
                }
            } else if ("--5d".equalsIgnoreCase(a) || "5d".equalsIgnoreCase(a)) {
                diasSimulacion = 5;
                maxPedidos = 0;
            }
        }

        // 2. Extraer del argumento posicional de límite (args[2])
        if (args.length > 2 && !args[2].trim().isEmpty() && !args[2].startsWith("--")) {
            String val = args[2].trim().toLowerCase();
            // Rango: ej. "10-15d", "10..15", "10-15"
            if (val.matches("(\\d+)[-\\.\\.]+(\\d+)d?")) {
                java.util.regex.Matcher mr = java.util.regex.Pattern.compile("(\\d+)[-\\.\\.]+(\\d+)").matcher(val);
                if (mr.find()) {
                    int d1 = Integer.parseInt(mr.group(1));
                    int d2 = Integer.parseInt(mr.group(2));
                    diaInicio = Math.min(d1, d2);
                    diasSimulacion = Math.max(1, Math.abs(d2 - d1));
                    maxPedidos = 0;
                }
            // Formato inicio + duración: ej. "10+5d", "dia10+5"
            } else if (val.matches(".*(\\d+)\\+(\\d+)d?")) {
                java.util.regex.Matcher mp = java.util.regex.Pattern.compile("(\\d+)\\+(\\d+)").matcher(val);
                if (mp.find()) {
                    diaInicio = Integer.parseInt(mp.group(1));
                    diasSimulacion = Integer.parseInt(mp.group(2));
                    maxPedidos = 0;
                }
            // Formato duración simple: ej. "5d", "3d"
            } else if (val.matches("(\\d+)d(ias)?")) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)d").matcher(val);
                if (m.find()) {
                    diasSimulacion = Integer.parseInt(m.group(1));
                    maxPedidos = 0;
                }
            } else if ("todos".equals(val) || "all".equals(val) || "mes".equals(val)) {
                maxPedidos = 0;
            } else {
                try {
                    maxPedidos = Integer.parseInt(val);
                } catch (Exception ignored) {}
            }
        }

        // 3. Argumento posicional 4 opcional para día de inicio: ej. 202601 5d 10
        if (args.length > 3 && !args[3].trim().isEmpty() && !args[3].startsWith("--")) {
            if (args[3].matches("\\d+")) {
                try {
                    diaInicio = Integer.parseInt(args[3].trim());
                } catch (Exception ignored) {}
            }
        }

        String limiteStr = "Todos";
        if (diasSimulacion > 0) {
            int diaFin = diaInicio + diasSimulacion - 1;
            limiteStr = String.format("Simulación de %d Días (días %02d al %02d)", diasSimulacion, diaInicio, diaFin);
        } else if (maxPedidos > 0) {
            limiteStr = maxPedidos + " pedidos";
        }

        System.out.println("Archivo de ventas   : " + rutaVentas);
        System.out.println("Archivo de bloqueos : " + rutaBloqueos);
        System.out.println("Plan de mantenim.   : " + (new File(rutaMantenimiento).exists() ? rutaMantenimiento : "(No disponible)"));
        System.out.println("Horizonte / Límite  : " + limiteStr);

        // 1. Inicializar Red Vial
        RedVial red = new RedVial();
        red.inicializarRed();

        // 2. Cargar Bloqueos
        CargadorArchivos cargador = new CargadorArchivos();
        List<Bloqueo> bloqueos = cargador.cargarBloqueos(rutaBloqueos);
        System.out.println("Bloqueos cargados   : " + bloqueos.size());
        for (Bloqueo b : bloqueos) {
            red.aplicarBloqueo(b);
        }

        // 3. Cargar Pedidos
        List<Pedido> pedidosTodos = cargador.cargarPedidos(rutaVentas);
        System.out.println("Pedidos en archivo  : " + pedidosTodos.size());
        if (pedidosTodos.isEmpty()) {
            System.err.println("No se pudieron cargar pedidos. Verifique la ruta del archivo.");
            return;
        }

        List<Pedido> pedidosPrueba = new ArrayList<>();
        int count = 0;
        LocalDateTime fechaInicioFiltro = null;
        LocalDateTime fechaFinFiltro = null;

        for (Pedido p : pedidosTodos) {
            if (p.getFechaHoraRegistro() == null) continue;

            if (diasSimulacion > 0) {
                if (fechaInicioFiltro == null) {
                    int anio = p.getFechaHoraRegistro().getYear();
                    int mes = p.getFechaHoraRegistro().getMonthValue();
                    LocalDate base = LocalDate.of(anio, mes, 1);
                    int dIniVal = Math.max(1, Math.min(diaInicio, base.lengthOfMonth()));
                    fechaInicioFiltro = LocalDate.of(anio, mes, dIniVal).atStartOfDay();
                    fechaFinFiltro = fechaInicioFiltro.plusDays(diasSimulacion);
                }

                if (p.getFechaHoraRegistro().isBefore(fechaInicioFiltro)) {
                    continue; // Aún no llega al día de inicio configurado
                }
                if (!p.getFechaHoraRegistro().isBefore(fechaFinFiltro)) {
                    break; // Ya superó los N días de simulación
                }
            }

            pedidosPrueba.add(p);
            count++;
            if (maxPedidos > 0 && count >= maxPedidos) break;
        }
        System.out.println("Pedidos a evaluar   : " + pedidosPrueba.size() + (diasSimulacion > 0 ? String.format(" (días %02d a %02d)", diaInicio, diaInicio + diasSimulacion - 1) : ""));

        // 4. Crear flota dinámica (descubierta desde archivo de mantenimiento o por parámetros)
        List<UnidadTransporte> flotaAco = crearFlotaDinamica(rutaMantenimiento, customAutos, customMotos, customBicis);
        List<UnidadTransporte> flotaAlns = crearFlotaDinamica(rutaMantenimiento, customAutos, customMotos, customBicis);
        System.out.println("Flota total creada  : " + flotaAco.size() + " unidades");

        // 4.1. Aplicar Mantenimiento Preventivo según la fecha de los pedidos (RF-42 / Pregunta 19 FAQ)
        List<com.paqrap.logistics.flota.model.Mantenimiento> mantenimientos = cargador.cargarMantenimientosPreventivos(rutaMantenimiento);
        LocalDateTime inicioSimulacion = pedidosPrueba.get(0).getFechaHoraRegistro();
        System.out.println("\n" + "-".repeat(80));
        System.out.println("  [MANTENIMIENTO PREVENTIVO RF-42] Restricciones activas para " + inicioSimulacion + ":");
        boolean hayMant = false;
        for (com.paqrap.logistics.flota.model.Mantenimiento mant : mantenimientos) {
            if (mant.estaActivoEn(inicioSimulacion)) {
                hayMant = true;
                System.out.println("  -> Unidad " + mant.getCodigoVehiculo() + ": En taller hasta " + mant.getFechaHoraFin() + " -> Inhabilitada");
                for (UnidadTransporte u : flotaAco) {
                    if (mant.getCodigoVehiculo().equalsIgnoreCase(u.getCodigo())) {
                        u.cambiarEstado(EstadoOperativo.EN_MANTENIMIENTO);
                        u.setActivo(false);
                    }
                }
                for (UnidadTransporte u : flotaAlns) {
                    if (mant.getCodigoVehiculo().equalsIgnoreCase(u.getCodigo())) {
                        u.cambiarEstado(EstadoOperativo.EN_MANTENIMIENTO);
                        u.setActivo(false);
                    }
                }
            }
        }
        if (!hayMant) System.out.println("  (Ninguna unidad en mantenimiento en este momento)");
        System.out.println("-".repeat(80));

        // 4.2. Evaluar si se solicita contingencia por averías mecánicas (RF-14, RF-15)
        boolean conAveria = false;
        for (String a : args) {
            if ("averia".equalsIgnoreCase(a) || "--averia".equalsIgnoreCase(a) || "--averias".equalsIgnoreCase(a)) {
                conAveria = true;
                break;
            }
        }
        if (conAveria) {
            System.out.println("\n" + "!".repeat(80));
            System.out.println("  [CONTINGENCIA RF-14 / RF-15] INYECCIÓN DE AVERÍAS EN LA FLOTA:");
            System.out.println("  -> TA02 (Auto): Falla mecánica en motor -> Estado: AVERIADA (Inhabilitado)");
            System.out.println("  -> TM01 (Moto): Falla en transmisión   -> Estado: AVERIADA (Inhabilitado)");
            System.out.println("!".repeat(80));

            for (UnidadTransporte u : flotaAco) {
                if ("TA02".equals(u.getCodigo()) || "TM01".equals(u.getCodigo())) {
                    u.cambiarEstado(EstadoOperativo.AVERIADA);
                    u.setActivo(false);
                }
            }
            for (UnidadTransporte u : flotaAlns) {
                if ("TA02".equals(u.getCodigo()) || "TM01".equals(u.getCodigo())) {
                    u.cambiarEstado(EstadoOperativo.AVERIADA);
                    u.setActivo(false);
                }
            }
        }

        // 5. Ejecutar ACO
        System.out.println("\n>>> Ejecutando Algoritmo ACO (Ant Colony Optimization)...");
        AlgoritmoACO aco = new AlgoritmoACO();
        long tInicioAco = System.currentTimeMillis();
        List<Ruta> rutasAco = aco.construirSolucion(pedidosPrueba, flotaAco, red);
        long tFinAco = System.currentTimeMillis();
        long duracionAco = tFinAco - tInicioAco;

        // 6. Ejecutar ALNS
        System.out.println(">>> Ejecutando Algoritmo ALNS (Adaptive Large Neighborhood Search)...");
        AlgoritmoALNS alns = new AlgoritmoALNS();
        long tInicioAlns = System.currentTimeMillis();
        List<Ruta> rutasAlns = alns.construirSolucion(pedidosPrueba, flotaAlns, red);
        long tFinAlns = System.currentTimeMillis();
        long duracionAlns = tFinAlns - tInicioAlns;

        // 7. Imprimir tabla comparativa
        imprimirResultados(pedidosPrueba.size(), "ACO (Ant Colony)", rutasAco, duracionAco,
                           "ALNS (Large Neighborhood)", rutasAlns, duracionAlns);
    }

    public static List<UnidadTransporte> crearFlotaEstandar() {
        return crearFlotaDinamica(null, 4, 3, 3);
    }

    public static List<UnidadTransporte> crearFlotaDinamica(String rutaMantenimiento, Integer nAutos, Integer nMotos, Integer nBicis) {
        List<UnidadTransporte> flota = new ArrayList<>();
        TipoVehiculo auto = TipoVehiculo.builder().id(1L).nombre("Auto").capacidadMaxima(24).velocidadPromedioKmH(40.0).costoPorKm(8.0).build();
        TipoVehiculo moto = TipoVehiculo.builder().id(2L).nombre("Moto").capacidadMaxima(8).velocidadPromedioKmH(25.0).costoPorKm(6.0).build();
        TipoVehiculo bici = TipoVehiculo.builder().id(3L).nombre("Bicicleta").capacidadMaxima(4).velocidadPromedioKmH(12.0).costoPorKm(3.0).build();

        Set<String> codigosEncontrados = new TreeSet<>();
        if (rutaMantenimiento != null && new File(rutaMantenimiento).exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(rutaMantenimiento))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    linea = linea.trim();
                    if (linea.isEmpty() || linea.startsWith("#")) continue;
                    String[] partes = linea.split(":");
                    if (partes.length == 2) {
                        codigosEncontrados.add(partes[1].trim());
                    }
                }
            } catch (Exception ignored) {}
        }

        // Si se especificaron cantidades por parámetro (ej. --flota=4,3,3)
        if (nAutos != null || nMotos != null || nBicis != null) {
            int cAutos = nAutos != null ? nAutos : 4;
            int cMotos = nMotos != null ? nMotos : 3;
            int cBicis = nBicis != null ? nBicis : 3;
            long id = 1;
            for (int i = 1; i <= cAutos; i++) {
                flota.add(UnidadTransporte.builder().id(id++).codigo(String.format("TA%02d", i)).tipo(auto).estadoOperativo(EstadoOperativo.DISPONIBLE).ubicacionActual(new Ubicacion(27, 14)).activo(true).build());
            }
            for (int i = 1; i <= cMotos; i++) {
                flota.add(UnidadTransporte.builder().id(id++).codigo(String.format("TM%02d", i)).tipo(moto).estadoOperativo(EstadoOperativo.DISPONIBLE).ubicacionActual(new Ubicacion(27, 14)).activo(true).build());
            }
            for (int i = 1; i <= cBicis; i++) {
                flota.add(UnidadTransporte.builder().id(id++).codigo(String.format("TB%02d", i)).tipo(bici).estadoOperativo(EstadoOperativo.DISPONIBLE).ubicacionActual(new Ubicacion(27, 14)).activo(true).build());
            }
            return flota;
        }

        // Si se encontraron vehículos en el archivo de mantenimiento, se construye la flota oficial dinámicamente
        if (!codigosEncontrados.isEmpty()) {
            long id = 1;
            for (String codigo : codigosEncontrados) {
                TipoVehiculo tipo;
                if (codigo.startsWith("TA")) tipo = auto;
                else if (codigo.startsWith("TM")) tipo = moto;
                else tipo = bici;

                flota.add(UnidadTransporte.builder()
                        .id(id++)
                        .codigo(codigo)
                        .tipo(tipo)
                        .estadoOperativo(EstadoOperativo.DISPONIBLE)
                        .ubicacionActual(new Ubicacion(27, 14))
                        .activo(true)
                        .build());
            }
            return flota;
        }

        // Fallback por defecto: 4 Autos, 3 Motos, 3 Bicis
        return crearFlotaDinamica(null, 4, 3, 3);
    }

    private static void imprimirResultados(int totalPedidos,
                                           String nombre1, List<Ruta> rutas1, long tiempo1,
                                           String nombre2, List<Ruta> rutas2, long tiempo2) {
        MetricasSolucion m1 = calcularMetricas(totalPedidos, rutas1, tiempo1);
        MetricasSolucion m2 = calcularMetricas(totalPedidos, rutas2, tiempo2);

        System.out.println("\n" + "=".repeat(80));
        System.out.println(String.format("%-35s | %-20s | %-20s", "MÉTRICA / INDICADOR", nombre1, nombre2));
        System.out.println("-".repeat(80));
        System.out.println(String.format("%-35s | %-20d | %-20d", "Total pedidos evaluados", totalPedidos, totalPedidos));
        System.out.println(String.format("%-35s | %-20d | %-20d", "Pedidos atendidos", m1.pedidosAtendidos, m2.pedidosAtendidos));
        System.out.println(String.format("%-35s | %-19.1f%% | %-19.1f%%", "Tasa de atención", m1.tasaAtencion, m2.tasaAtencion));
        System.out.println(String.format("%-35s | %-19.1f%% | %-19.1f%%", "Entregas en plazo (On-Time)", m1.pctEnPlazo, m2.pctEnPlazo));
        System.out.println(String.format("%-35s | %-20d | %-20d", "Rutas vehiculares construidas", m1.numRutas, m2.numRutas));
        System.out.println(String.format("%-35s | %-17.2f km | %-17.2f km", "Distancia total recorrida", m1.distanciaTotalKm, m2.distanciaTotalKm));
        System.out.println(String.format("%-35s | S/ %-17.2f | S/ %-17.2f", "Costo operativo total", m1.costoTotal, m2.costoTotal));
        System.out.println(String.format("%-35s | %-17d ms | %-17d ms", "Tiempo de ejecución de CPU", m1.tiempoMs, m2.tiempoMs));
        System.out.println(String.format("%-35s | %-17.1f min | %-17.1f min", "Tiempo prom. por ruta", m1.tiempoPromedioMin, m2.tiempoPromedioMin));
        System.out.println("=".repeat(80));

        // Detalle de rutas
        System.out.println("\n--- DETALLE DE RUTAS GENERADAS POR " + nombre1.toUpperCase() + " ---");
        imprimirDetalleRutas(rutas1);

        System.out.println("\n--- DETALLE DE RUTAS GENERADAS POR " + nombre2.toUpperCase() + " ---");
        imprimirDetalleRutas(rutas2);
    }

    private static void imprimirDetalleRutas(List<Ruta> rutas) {
        if (rutas == null || rutas.isEmpty()) {
            System.out.println("  (No se generaron rutas)");
            return;
        }
        for (Ruta r : rutas) {
            String vehiculo = r.getUnidadTransporte() != null ? r.getUnidadTransporte().getCodigo() : "N/A";
            String tipo = (r.getUnidadTransporte() != null && r.getUnidadTransporte().getTipo() != null)
                    ? r.getUnidadTransporte().getTipo().getNombre() : "N/A";
            int cargaTotal = r.getParadas().stream()
                    .mapToInt(p -> p.getPedido() != null ? p.getPedido().getCantidadUnidades() : 0).sum();
            int capMax = (r.getUnidadTransporte() != null && r.getUnidadTransporte().getTipo() != null)
                    ? r.getUnidadTransporte().getTipo().getCapacidadMaxima() : 0;
            System.out.println(String.format("  [%s] Vehículo: %s (%s) | Paradas: %d | Carga: %d/%d u. | Dist: %.1f km | Costo: S/ %.2f | Tiempo: %d min",
                    r.getCodigo(), vehiculo, tipo, r.getParadas().size(), cargaTotal, capMax, r.getDistanciaTotalKm(), r.getCostoTotal(), r.getTiempoEstimadoMin()));

            Ubicacion origen = (r.getAlmacenOrigen() != null && r.getAlmacenOrigen().getUbicacion() != null)
                    ? r.getAlmacenOrigen().getUbicacion() : new Ubicacion(27, 14);

            LocalDateTime tSalida = r.getFechaHoraGeneracion() != null ? r.getFechaHoraGeneracion()
                    : (!r.getParadas().isEmpty() && r.getParadas().get(0).getPedido() != null
                    ? r.getParadas().get(0).getPedido().getFechaHoraRegistro() : LocalDateTime.of(2026, 9, 1, 0, 0));
            LocalDateTime tRetorno = tSalida.plusMinutes(r.getTiempoEstimadoMin());

            System.out.println(String.format("     -> [SALIDA]                   Almacén Central (%d,%d) | Hora Salida: %s",
                    origen.getPosX(), origen.getPosY(), tSalida));

            int idx = 1;
            for (ParadaRuta p : r.getParadas()) {
                if (p.getPedido() != null) {
                    Ubicacion dest = p.getPedido().getDestino();
                    String coords = dest != null ? "(" + dest.getPosX() + "," + dest.getPosY() + ")" : "N/A";
                    System.out.println(String.format("     -> [ENTREGA %02d] Pedido: %-12s | Destino: %-9s | Cant: %2d u. | Plazo: %s | Llegada: %s",
                            idx++, p.getPedido().getCodigo(), coords, p.getPedido().getCantidadUnidades(),
                            p.getPedido().getPlazoLimiteEntrega(), p.getHoraEstimadaLlegada()));
                }
            }

            System.out.println(String.format("     -> [RETORNO/REABASTECIMIENTO] Almacén Central (%d,%d) | Llegada Estimada: %s | Reabastecido y disponible",
                    origen.getPosX(), origen.getPosY(), tRetorno));
        }
    }

    private static MetricasSolucion calcularMetricas(int totalPedidos, List<Ruta> rutas, long tiempoMs) {
        MetricasSolucion m = new MetricasSolucion();
        m.tiempoMs = tiempoMs;
        if (rutas == null) return m;

        m.numRutas = rutas.size();
        int aTiempo = 0;
        for (Ruta r : rutas) {
            m.distanciaTotalKm += r.getDistanciaTotalKm();
            m.costoTotal += r.getCostoTotal();
            m.tiempoPromedioMin += r.getTiempoEstimadoMin();
            for (ParadaRuta p : r.getParadas()) {
                m.pedidosAtendidos++;
                if (p.getPedido() != null && p.getHoraEstimadaLlegada() != null && p.getPedido().getPlazoLimiteEntrega() != null) {
                    if (!p.getHoraEstimadaLlegada().isAfter(p.getPedido().getPlazoLimiteEntrega())) {
                        aTiempo++;
                    }
                } else {
                    aTiempo++;
                }
            }
        }
        if (m.numRutas > 0) {
            m.tiempoPromedioMin /= m.numRutas;
        }
        m.tasaAtencion = totalPedidos > 0 ? (m.pedidosAtendidos * 100.0) / totalPedidos : 0.0;
        m.pctEnPlazo = m.pedidosAtendidos > 0 ? (aTiempo * 100.0) / m.pedidosAtendidos : 100.0;
        return m;
    }

    private static class MetricasSolucion {
        int pedidosAtendidos = 0;
        double tasaAtencion = 0.0;
        double pctEnPlazo = 0.0;
        int numRutas = 0;
        double distanciaTotalKm = 0.0;
        double costoTotal = 0.0;
        long tiempoMs = 0;
        double tiempoPromedioMin = 0.0;
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
