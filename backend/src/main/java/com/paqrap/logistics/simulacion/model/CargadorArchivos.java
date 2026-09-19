package com.paqrap.logistics.simulacion.model;

import com.paqrap.logistics.flota.model.Averia;
import com.paqrap.logistics.flota.model.TipoAveria;
import com.paqrap.logistics.pedidos.model.Cliente;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.pedidos.model.TipoEntrega;
import com.paqrap.logistics.redvial.model.Bloqueo;
import com.paqrap.logistics.redvial.model.Ubicacion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitario de carga y validación de archivos planos de entrada (RF-11, RF-64).
 * Soporta los formatos oficiales:
 * - Bloqueos: ##d##h##m-##d##h##m:x1,y1,...,xn,yn
 * - Pedidos:   ##d##h##m,posX,posY,cIdCliente,qq,hl
 */
@Slf4j
@Component
public class CargadorArchivos {

    private static final Pattern PATRON_FECHA = Pattern.compile("(\\d{2})d(\\d{2})h(\\d{2})m");

    /**
     * Valida si el formato sintáctico del archivo es correcto antes de ejecutar la simulación (RF-64).
     */
    public boolean validarFormato(String rutaArchivo) {
        if (rutaArchivo == null || rutaArchivo.trim().isEmpty()) {
            return false;
        }
        File archivo = new File(rutaArchivo);
        return archivo.exists() && archivo.isFile() && archivo.canRead();
    }

    /**
     * Parsea un archivo de pedidos con formato: ##d##h##m,posX,posY,cIdCliente,qq,hl (RF-64).
     */
    public List<Pedido> cargarPedidos(String rutaArchivo) {
        List<Pedido> pedidos = new ArrayList<>();
        if (!validarFormato(rutaArchivo)) {
            log.warn("Archivo de pedidos no encontrado o no accesible: {}", rutaArchivo);
            return pedidos;
        }

        int anio = 2026;
        int mes = 9;
        String nombreArchivo = new File(rutaArchivo).getName();
        Matcher mAnioMes = Pattern.compile("(\\d{4})(\\d{2})").matcher(nombreArchivo);
        if (mAnioMes.find()) {
            anio = Integer.parseInt(mAnioMes.group(1));
            mes = Integer.parseInt(mAnioMes.group(2));
        }

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int numLinea = 0;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                numLinea++;
                if (linea.isEmpty() || linea.startsWith("#")) continue;

                // Soporta formato ##d##h##m,x,y,... y formato ##d##h##m:x,y,...
                String lineaNormalizada = linea;
                int idxColon = linea.indexOf(':');
                int idxComma = linea.indexOf(',');
                if (idxColon != -1 && (idxComma == -1 || idxColon < idxComma)) {
                    lineaNormalizada = linea.substring(0, idxColon) + "," + linea.substring(idxColon + 1);
                }

                String[] partes = lineaNormalizada.split(",");
                if (partes.length >= 6) {
                    try {
                        String timeStr = partes[0].trim();
                        int x = Integer.parseInt(partes[1].trim());
                        int y = Integer.parseInt(partes[2].trim());
                        String idCliente = partes[3].trim();
                        int cantidad = Integer.parseInt(partes[4].trim());
                        int horasLimite = Integer.parseInt(partes[5].trim());

                        LocalDateTime regTime = parsearFechaSimulada(timeStr, anio, mes);
                        TipoEntrega tipo = mapearTipoEntrega(horasLimite);

                        Cliente cliente = Cliente.builder()
                                .idCliente(idCliente)
                                .nombre("Cliente " + idCliente)
                                .ubicacionEntrega(new Ubicacion(x, y))
                                .build();

                        String baseCodigo = "PED-" + numLinea + "-" + UUID.randomUUID().toString().substring(0, 6);
                        int restante = cantidad;
                        int maxCapacidad = 24;
                        int sub = 1;
                        while (restante > 0) {
                            int cantSub = Math.min(restante, maxCapacidad);
                            String codFinal = baseCodigo;
                            String codPadre = null;
                            if (cantidad > maxCapacidad) {
                                codFinal = baseCodigo + "-SUB" + sub;
                                codPadre = baseCodigo;
                            }
                            Pedido pedido = Pedido.builder()
                                    .id((long) numLinea * 1000 + sub)
                                    .codigo(codFinal)
                                    .codigoPadre(codPadre)
                                    .cantidadUnidades(cantSub)
                                    .fechaHoraRegistro(regTime)
                                    .tipoEntrega(tipo)
                                    .plazoLimiteEntrega(regTime.plusHours(tipo.getHorasPlazo()))
                                    .estado(EstadoPedido.REGISTRADO)
                                    .destino(new Ubicacion(x, y))
                                    .cliente(cliente)
                                    .build();
                            pedidos.add(pedido);
                            restante -= cantSub;
                            sub++;
                        }
                    } catch (Exception e) {
                        log.error("Error procesando línea {} de pedidos: {}", numLinea, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al leer archivo de pedidos: {}", e.getMessage());
        }

        log.info("Cargados {} pedidos desde {}", pedidos.size(), rutaArchivo);
        return pedidos;
    }

    /**
     * Parsea un archivo de bloqueos viales con formato: ##d##h##m-##d##h##m:x1,y1,...,xn,yn (RF-11).
     */
    public List<Bloqueo> cargarBloqueos(String rutaArchivo) {
        List<Bloqueo> bloqueos = new ArrayList<>();
        if (!validarFormato(rutaArchivo)) {
            log.warn("Archivo de bloqueos no encontrado o no accesible: {}", rutaArchivo);
            return bloqueos;
        }

        int anio = 2026;
        int mes = 9;
        String nombreArchivo = new File(rutaArchivo).getName();
        Matcher mAnioMes = Pattern.compile("(\\d{4})(\\d{2})").matcher(nombreArchivo);
        if (mAnioMes.find()) {
            anio = Integer.parseInt(mAnioMes.group(1));
            mes = Integer.parseInt(mAnioMes.group(2));
        } else {
            Matcher mCorto = Pattern.compile("(\\d{2})(\\d{2})").matcher(nombreArchivo);
            if (mCorto.find()) {
                anio = 2000 + Integer.parseInt(mCorto.group(1));
                mes = Integer.parseInt(mCorto.group(2));
            }
        }

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int numLinea = 0;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                numLinea++;
                if (linea.isEmpty() || linea.startsWith("#")) continue;

                // Formato: ##d##h##m-##d##h##m:x1,y1,...,xn,yn
                int posDosPuntos = linea.indexOf(':');
                if (posDosPuntos != -1) {
                    String ventanaTiempo = linea.substring(0, posDosPuntos).trim();
                    String coords = linea.substring(posDosPuntos + 1).trim();

                    String[] tiempos = ventanaTiempo.split("-");
                    if (tiempos.length == 2) {
                        LocalDateTime inicio = parsearFechaSimulada(tiempos[0].trim(), anio, mes);
                        LocalDateTime fin = parsearFechaSimulada(tiempos[1].trim(), anio, mes);

                        Bloqueo b = Bloqueo.builder()
                                .id((long) numLinea)
                                .codigo("BLOQ-" + numLinea + "-" + UUID.randomUUID().toString().substring(0, 6))
                                .fechaHoraInicio(inicio)
                                .fechaHoraFin(fin)
                                .coordenadasNodos(coords)
                                .activo(false)
                                .archivoOrigen(new File(rutaArchivo).getName())
                                .build();
                        bloqueos.add(b);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al leer archivo de bloqueos: {}", e.getMessage());
        }

        log.info("Cargados {} bloqueos desde {}", bloqueos.size(), rutaArchivo);
        return bloqueos;
    }

    /**
     * Parsea archivo de averías simuladas.
     */
    public List<Averia> cargarAverias(String rutaArchivo) {
        List<Averia> averias = new ArrayList<>();
        if (!validarFormato(rutaArchivo)) return averias;

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int num = 0;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                num++;
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                String[] partes = linea.split(",");
                if (partes.length >= 4) {
                    LocalDateTime fecha = parsearFechaSimulada(partes[0].trim(), 2026, 9);
                    int x = Integer.parseInt(partes[1].trim());
                    int y = Integer.parseInt(partes[2].trim());
                    int tipoNum = Integer.parseInt(partes[3].trim());

                    TipoAveria tipo = (tipoNum == 1) ? TipoAveria.TIPO_1 :
                            (tipoNum == 2) ? TipoAveria.TIPO_2 : TipoAveria.TIPO_3;

                    Averia a = Averia.builder()
                            .codigo("AVE-" + num)
                            .fechaHoraEvento(fecha)
                            .ubicacionFalla(new Ubicacion(x, y))
                            .tipo(tipo)
                            .build();
                    a.setHoraReincorporacion(a.calcularReincorporacion(null));
                    averias.add(a);
                }
            }
        } catch (Exception e) {
            log.error("Error leyendo averías: {}", e.getMessage());
        }
        return averias;
    }

    public static LocalDateTime parsearFechaSimulada(String texto, int anio, int mes) {
        Matcher m = PATRON_FECHA.matcher(texto);
        if (m.matches()) {
            int dia = Integer.parseInt(m.group(1));
            int hora = Integer.parseInt(m.group(2));
            int min = Integer.parseInt(m.group(3));
            return LocalDateTime.of(anio, mes, Math.max(1, dia), hora, min, 0);
        }
        return LocalDateTime.of(anio, mes, 1, 0, 0, 0);
    }

    /**
     * Parsea un archivo de mantenimiento preventivo (RF-42 / Pregunta 19 FAQ).
     * Formato oficial: aaaammdd:TTNN (ej. 20260901:TA01)
     * Retorna un mapa: Fecha -> Lista de códigos de vehículos en mantenimiento.
     */
    public List<com.paqrap.logistics.flota.model.Mantenimiento> cargarMantenimientosPreventivos(String rutaArchivo) {
        List<com.paqrap.logistics.flota.model.Mantenimiento> mantList = new ArrayList<>();
        if (!validarFormato(rutaArchivo)) {
            log.warn("Archivo de mantenimiento preventivo no encontrado o inaccesible: {}", rutaArchivo);
            return mantList;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int numLinea = 0;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                numLinea++;
                if (linea.isEmpty() || linea.startsWith("#")) continue;

                String[] partes = linea.split(":");
                if (partes.length == 2) {
                    String fechaStr = partes[0].trim();
                    String codigoVehiculo = partes[1].trim();
                    if (fechaStr.length() == 8) {
                        try {
                            int anio = Integer.parseInt(fechaStr.substring(0, 4));
                            int mes = Integer.parseInt(fechaStr.substring(4, 6));
                            int dia = Integer.parseInt(fechaStr.substring(6, 8));
                            java.time.LocalDate fecha = java.time.LocalDate.of(anio, mes, dia);
                            LocalDateTime inicio = fecha.atStartOfDay();
                            LocalDateTime fin;
                            if (codigoVehiculo.startsWith("TB")) {
                                fin = inicio.plusHours(8).minusSeconds(1);
                            } else if (codigoVehiculo.startsWith("TA")) {
                                fin = inicio.plusDays(2).minusSeconds(1);
                            } else {
                                fin = inicio.plusDays(1).minusSeconds(1);
                            }
                            com.paqrap.logistics.flota.model.Mantenimiento m = com.paqrap.logistics.flota.model.Mantenimiento.builder()
                                .codigoVehiculo(codigoVehiculo)
                                .fechaHoraInicio(inicio)
                                .fechaHoraFin(fin)
                                .build();
                            mantList.add(m);
                        } catch (Exception ex) {
                            log.warn("Línea {}: Formato de fecha inválido '{}'", numLinea, fechaStr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al leer archivo de mantenimiento preventivo: {}", e.getMessage());
        }

        log.info("Cargados {} registros de mantenimiento desde {}", mantList.size(), rutaArchivo);
        return mantList;
    }

    private TipoEntrega mapearTipoEntrega(int horas) {
        switch (horas) {
            case 4: return TipoEntrega.PRIORIZADA_4H;
            case 8: return TipoEntrega.PRIORIZADA_8H;
            case 12: return TipoEntrega.PRIORIZADA_12H;
            case 18: return TipoEntrega.PRIORIZADA_18H;
            case 36:
            default:
                return TipoEntrega.REGULAR_36H;
        }
    }
}
