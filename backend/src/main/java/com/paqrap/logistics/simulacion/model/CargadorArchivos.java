package com.paqrap.logistics.simulacion.model;

import com.paqrap.logistics.flota.model.Averia;
import com.paqrap.logistics.flota.model.TipoAveria;
import com.paqrap.logistics.flota.service.PlanMantenimientoService;
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

    public static int[] extraerAnioMes(String rutaArchivo) {
        int anio = 2026;
        int mes = 9;
        if (rutaArchivo == null || rutaArchivo.trim().isEmpty()) {
            return new int[]{anio, mes};
        }
        String nombre = new File(rutaArchivo).getName();
        // Quitar prefijo de timestamp si fue subido vía ArchivoStorageService (ej. "1731234567890-")
        nombre = nombre.replaceFirst("^\\d{10,14}[-_]", "");

        // 1. Buscar año de 4 dígitos (2020 a 2099) seguido de mes (01 a 12), ej. 202610.bloqueadas, ventas202610.txt
        Matcher m4 = Pattern.compile("(?<!\\d)(20\\d{2})(0[1-9]|1[0-2])(?![0-9])").matcher(nombre);
        if (m4.find()) {
            return new int[]{Integer.parseInt(m4.group(1)), Integer.parseInt(m4.group(2))};
        }

        // 2. Buscar año de 2 dígitos (20 a 39) seguido de mes (01 a 12), ej. bloqueo.2610.txt
        Matcher m2 = Pattern.compile("(?<!\\d)(2[0-9]|3[0-9])(0[1-9]|1[0-2])(?![0-9])").matcher(nombre);
        if (m2.find()) {
            return new int[]{2000 + Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2))};
        }

        return new int[]{anio, mes};
    }

    /**
     * Valida si el formato sintáctico del archivo es correcto antes de ejecutar la simulación (RF-64).
     */
    public boolean validarFormato(String rutaArchivo) {
        if (rutaArchivo == null || rutaArchivo.trim().isEmpty()) {
            return false;
        }
        File archivo = new File(rutaArchivo);
        return archivo.exists() && archivo.isFile() && archivo.canRead() && archivo.length() > 0;
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

        int[] anioMes = extraerAnioMes(rutaArchivo);
        int anio = anioMes[0];
        int mes = anioMes[1];

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int numLinea = 0;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                numLinea++;
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1).trim();
                linea = linea.replace(';', ',');

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

        int[] anioMes = extraerAnioMes(rutaArchivo);
        int anio = anioMes[0];
        int mes = anioMes[1];

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int numLinea = 0;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                numLinea++;
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1).trim();
                linea = linea.replace(';', ',');

                // Formato: ##d##h##m-##d##h##m:x1,y1,...,xn,yn o con coma de separación
                int posDosPuntos = linea.indexOf(':');
                String ventanaTiempo;
                String coords;
                if (posDosPuntos != -1) {
                    ventanaTiempo = linea.substring(0, posDosPuntos).trim();
                    coords = linea.substring(posDosPuntos + 1).trim();
                } else {
                    int posGuion = linea.indexOf('-');
                    int primeraComa = linea.indexOf(',');
                    if (posGuion != -1 && primeraComa > posGuion) {
                        ventanaTiempo = linea.substring(0, primeraComa).trim();
                        coords = linea.substring(primeraComa + 1).trim();
                    } else {
                        continue;
                    }
                }

                String[] tiempos = ventanaTiempo.split("-");
                if (tiempos.length == 2) {
                    LocalDateTime inicio = parsearFechaSimulada(tiempos[0].trim(), anio, mes);
                    LocalDateTime fin = parsearFechaSimulada(tiempos[1].trim(), anio, mes);

                    Bloqueo b = Bloqueo.builder()
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

        int[] anioMes = extraerAnioMes(rutaArchivo);
        int anio = anioMes[0];
        int mes = anioMes[1];

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int num = 0;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                num++;
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1).trim();
                linea = linea.replace(';', ',');
                String[] partes = linea.split(",");
                if (partes.length >= 4) {
                    LocalDateTime fecha = parsearFechaSimulada(partes[0].trim(), anio, mes);
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
        if (texto == null) return LocalDateTime.of(anio, mes, 1, 0, 0, 0);
        texto = texto.replace("\uFEFF", "").trim();
        Matcher m = PATRON_FECHA.matcher(texto);
        if (m.find()) {
            int dia = Integer.parseInt(m.group(1));
            int hora = Integer.parseInt(m.group(2));
            int min = Integer.parseInt(m.group(3));
            int maxDias = java.time.YearMonth.of(anio, mes).lengthOfMonth();
            dia = Math.min(Math.max(1, dia), maxDias);
            hora = Math.min(Math.max(0, hora), 23);
            min = Math.min(Math.max(0, min), 59);
            return LocalDateTime.of(anio, mes, dia, hora, min, 0);
        }
        return LocalDateTime.of(anio, mes, 1, 0, 0, 0);
    }

    /**
     * Parsea un archivo de mantenimiento preventivo (RF-42 / Pregunta 19 FAQ).
     * Formato oficial: aaaammdd:TTNN (ej. 20260901:TA01). Toda unidad, sin importar su tipo,
     * queda no disponible de 00:00 a 23:59 de ese día.
     */
    public List<com.paqrap.logistics.flota.model.Mantenimiento> cargarMantenimientosPreventivos(String rutaArchivo) {
        if (!validarFormato(rutaArchivo)) {
            log.warn("Archivo de mantenimiento preventivo no encontrado o inaccesible: {}", rutaArchivo);
            return new ArrayList<>();
        }
        List<com.paqrap.logistics.flota.model.Mantenimiento> mantList =
                PlanMantenimientoService.parsearArchivo(new File(rutaArchivo).toPath());
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
