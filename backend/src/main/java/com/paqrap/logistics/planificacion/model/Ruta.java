package com.paqrap.logistics.planificacion.model;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.redvial.model.Ubicacion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una ruta de entrega planificada (RF-05, RF-06, RF-07, RF-08, RF-55).
 */
@Data
@lombok.EqualsAndHashCode(of = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rutas")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    private LocalDateTime fechaHoraGeneracion;

    private double distanciaTotalKm;

    private int tiempoEstimadoMin;

    private double costoTotal;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoRuta estado = EstadoRuta.PLANIFICADA;

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL)
    @OrderBy("orden ASC")
    @ToString.Exclude
    @Builder.Default
    private List<ParadaRuta> paradas = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_id")
    private UnidadTransporte unidadTransporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_id")
    private Almacen almacenOrigen;

    /**
     * Solo en rutas de rescate: tramo previo hasta el punto de la avería y trasvase. No se guarda como
     * columna (sería la tabla "trasvase" del documento 62); la operación diaria lo vuelve a adjuntar
     * desde RegistroRescates cada vez que carga la ruta.
     */
    @jakarta.persistence.Transient
    @ToString.Exclude
    private TramoRescate rescate;

    /**
     * Calcula el costo estimado multiplicando la distancia total por la tarifa del vehículo (RF-05).
     * Redondeado a dos decimales en soles.
     */
    public double calcularCosto(double tarifaPorKm) {
        this.costoTotal = Math.round((this.distanciaTotalKm * tarifaPorKm) * 100.0) / 100.0;
        return this.costoTotal;
    }

    /**
     * Calcula el tiempo total estimado sumando tiempos de tránsito y los 60 min de servicio por parada (RF-09).
     */
    public int calcularTiempoEstimado() {
        int tiempoServicioTotal = (paradas != null) ? paradas.size() * 60 : 0;
        // Si hay velocidad del vehículo, el tiempo de tránsito se deriva de la distancia
        int tiempoTransito = 0;
        if (unidadTransporte != null && unidadTransporte.getTipo() != null
                && unidadTransporte.getTipo().getVelocidadPromedioKmH() > 0) {
            double horas = distanciaTotalKm / unidadTransporte.getTipo().getVelocidadPromedioKmH();
            tiempoTransito = (int) Math.ceil(horas * 60.0);
        }
        this.tiempoEstimadoMin = tiempoTransito + tiempoServicioTotal;
        return this.tiempoEstimadoMin;
    }

    /**
     * Agrega una parada a la secuencia de entregas de la ruta.
     */
    public void agregarParada(ParadaRuta parada) {
        if (parada != null) {
            if (this.paradas == null) {
                this.paradas = new ArrayList<>();
            }
            parada.setRuta(this);
            parada.setOrden(this.paradas.size() + 1);
            this.paradas.add(parada);
        }
    }

    /**
     * Valida que la hora estimada de llegada a cada parada no exceda el plazo límite comprometido (RF-08).
     */
    public boolean cumplePlazos() {
        if (paradas == null || paradas.isEmpty()) {
            return true;
        }
        for (ParadaRuta parada : paradas) {
            if (parada.getPedido() != null && parada.getHoraEstimadaLlegada() != null) {
                if (parada.getHoraEstimadaLlegada().isAfter(parada.getPedido().getPlazoLimiteEntrega())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Valida si el total de paquetes asignados excede la capacidad máxima del vehículo (RF-07).
     */
    public boolean capacidadExcedida() {
        if (unidadTransporte == null || unidadTransporte.getTipo() == null) {
            return false;
        }
        int cargaTotal = 0;
        if (paradas != null) {
            for (ParadaRuta parada : paradas) {
                if (parada.getPedido() != null) {
                    cargaTotal += parada.getPedido().getCantidadUnidades();
                }
            }
        }
        return cargaTotal > unidadTransporte.getTipo().getCapacidadMaxima();
    }

    /**
     * Paquetes que el vehículo todavía lleva en esta ruta: los de las paradas aún no entregadas.
     * La carga se completa al salir del almacén, así que una ruta recién planificada lleva todo.
     */
    public int cargaPendiente() {
        if (paradas == null || estado == EstadoRuta.COMPLETADA) {
            return 0;
        }
        int carga = 0;
        for (ParadaRuta parada : paradas) {
            if (!parada.isEntregada() && parada.getPedido() != null) {
                carga += parada.getPedido().getCantidadUnidades();
            }
        }
        return carga;
    }

    /**
     * Paquetes que lleva físicamente el vehículo de esta ruta en ese instante: en una ruta de rescate,
     * antes de terminar el trasvase la carga sigue en la unidad averiada.
     */
    public int cargaABordo(LocalDateTime instante) {
        return trasvasePendiente(instante) ? 0 : cargaPendiente();
    }

    /**
     * Ruta en curso de cada unidad: si el algoritmo le encadenó varias, la más antigua sin completar.
     * Sirve para saber qué carga lleva cada vehículo en este momento.
     */
    public static java.util.Map<String, Ruta> rutaEnCursoPorUnidad(java.util.Collection<Ruta> rutas) {
        java.util.Map<String, Ruta> enCurso = new java.util.HashMap<>();
        for (Ruta r : rutas) {
            if (r == null || r.getEstado() == EstadoRuta.COMPLETADA || r.getUnidadTransporte() == null) continue;
            String codigo = r.getUnidadTransporte().getCodigo();
            Ruta actual = enCurso.get(codigo);
            if (actual == null || esAnterior(r, actual)) {
                enCurso.put(codigo, r);
            }
        }
        return enCurso;
    }

    // ===== Inicio de las entregas (rutas normales y de rescate) =====

    /** Instante desde el que el vehículo sale hacia la primera parada. */
    public LocalDateTime inicioEntregas() {
        return rescate != null ? rescate.getFin() : fechaHoraGeneracion;
    }

    /** Punto desde el que el vehículo sale hacia la primera parada. */
    public Ubicacion origenEntregas() {
        if (rescate != null) return rescate.getPuntoTrasvase();
        return almacenOrigen != null ? almacenOrigen.getUbicacion() : null;
    }

    /** true si es una ruta de rescate cuyo trasvase aún no termina en ese instante. */
    public boolean trasvasePendiente(LocalDateTime instante) {
        return rescate != null && instante != null && instante.isBefore(rescate.getFin());
    }

    /**
     * Rutas de rescate: mientras la unidad va al punto de la avería o trasvasa, se actualiza su posición
     * (por calles, primero en X y luego en Y) y no entrega nada todavía.
     *
     * @return true si la unidad aún está en el tramo de rescate (el llamador no debe procesar paradas)
     */
    public boolean avanzarTramoRescate(LocalDateTime instante) {
        if (!trasvasePendiente(instante) || unidadTransporte == null) return false;
        if (estado == EstadoRuta.PLANIFICADA) estado = EstadoRuta.EN_EJECUCION;
        LocalDateTime salida = rescate.getSalida();
        LocalDateTime llegada = rescate.getLlegada();
        if (instante.isBefore(llegada)) {
            long total = java.time.Duration.between(salida, llegada).getSeconds();
            long hecho = Math.max(0, java.time.Duration.between(salida, instante).getSeconds());
            double f = total > 0 ? Math.min(1.0, (double) hecho / total) : 1.0;
            unidadTransporte.setUbicacionActual(enL(rescate.getPartida(), rescate.getPuntoTrasvase(), f));
        } else {
            Ubicacion punto = rescate.getPuntoTrasvase();
            unidadTransporte.setUbicacionActual(new Ubicacion(punto.getPosX(), punto.getPosY()));
        }
        return true;
    }

    // ===== Regreso al almacén al terminar las entregas (FAQ 10) =====

    /** Punto donde termina la última entrega (o el almacén de salida si no hay paradas con destino). */
    public Ubicacion puntoFinalEntregas() {
        if (paradas != null) {
            for (int i = paradas.size() - 1; i >= 0; i--) {
                ParadaRuta p = paradas.get(i);
                if (p.getPedido() != null && p.getPedido().getDestino() != null) {
                    return p.getPedido().getDestino();
                }
            }
        }
        return almacenOrigen != null ? almacenOrigen.getUbicacion() : null;
    }

    /** Instante en que termina el acondicionamiento de la última entrega. */
    public LocalDateTime finEntregas() {
        LocalDateTime fin = fechaHoraGeneracion;
        if (paradas != null) {
            for (ParadaRuta p : paradas) {
                if (p.getHoraEstimadaLlegada() == null) continue;
                LocalDateTime salida = p.getHoraEstimadaLlegada().plusMinutes(p.getTiempoServicioMin());
                if (fin == null || salida.isAfter(fin)) fin = salida;
            }
        }
        return fin;
    }

    /**
     * Almacén al que vuelve el vehículo: el más cercano (distancia por calles de la retícula) que tenga
     * stock, porque una unidad no puede volver a un almacén sin stock (FAQ 10). El central siempre tiene.
     */
    public static Almacen almacenDeRetorno(Ubicacion desde, java.util.Collection<? extends Almacen> almacenes) {
        if (desde == null || almacenes == null) return null;
        Almacen mejor = null;
        int mejorDist = Integer.MAX_VALUE;
        for (Almacen a : almacenes) {
            if (a == null || a.getUbicacion() == null || !a.tieneStockSuficiente(1)) continue;
            int d = Math.abs(a.getUbicacion().getPosX() - desde.getPosX()) + Math.abs(a.getUbicacion().getPosY() - desde.getPosY());
            if (d < mejorDist || (d == mejorDist && mejor != null && a.getCodigo().compareTo(mejor.getCodigo()) < 0)) {
                mejor = a;
                mejorDist = d;
            }
        }
        return mejor;
    }

    /** Hora de llegada al almacén de retorno, a la velocidad promedio del tipo de vehículo. */
    public LocalDateTime horaLlegadaAlmacen(Almacen destino) {
        LocalDateTime salida = finEntregas();
        Ubicacion desde = puntoFinalEntregas();
        if (salida == null || destino == null || destino.getUbicacion() == null || desde == null) return salida;
        double velocidad = (unidadTransporte != null && unidadTransporte.getTipo() != null
                && unidadTransporte.getTipo().getVelocidadPromedioKmH() > 0)
                ? unidadTransporte.getTipo().getVelocidadPromedioKmH() : 25.0;
        int km = Math.abs(destino.getUbicacion().getPosX() - desde.getPosX())
                + Math.abs(destino.getUbicacion().getPosY() - desde.getPosY());
        long segundos = Math.round(km / velocidad * 3600);
        return salida.plusSeconds(segundos);
    }

    /**
     * Con todas las paradas entregadas, el vehículo vuelve al almacén en vez de aparecer allí de golpe.
     * Mientras viaja sigue EN_RUTA y se actualiza su posición (por calles: primero en X y luego en Y);
     * al llegar la ruta se completa y la unidad queda DISPONIBLE en ese almacén.
     *
     * @return true si el vehículo ya llegó al almacén
     */
    public boolean avanzarRetorno(LocalDateTime instante, java.util.Collection<? extends Almacen> almacenes) {
        UnidadTransporte u = unidadTransporte;
        Ubicacion desde = puntoFinalEntregas();
        Almacen destino = almacenDeRetorno(desde, almacenes);
        Ubicacion llegadaEn = (destino != null) ? destino.getUbicacion() : new Ubicacion(27, 14);
        LocalDateTime salida = finEntregas();
        LocalDateTime llegada = (destino != null) ? horaLlegadaAlmacen(destino) : instante;

        if (instante != null && llegada != null && instante.isBefore(llegada) && desde != null && u != null) {
            long total = java.time.Duration.between(salida, llegada).getSeconds();
            long transcurrido = Math.max(0, java.time.Duration.between(salida, instante).getSeconds());
            double f = total > 0 ? Math.min(1.0, (double) transcurrido / total) : 1.0;
            u.setUbicacionActual(enL(desde, llegadaEn, f));
            return false;
        }
        estado = EstadoRuta.COMPLETADA;
        if (u != null) {
            u.setUbicacionActual(new Ubicacion(llegadaEn.getPosX(), llegadaEn.getPosY()));
            if (u.getEstadoOperativo() == com.paqrap.logistics.flota.model.EstadoOperativo.EN_RUTA) {
                u.setEstadoOperativo(com.paqrap.logistics.flota.model.EstadoOperativo.DISPONIBLE);
            }
        }
        return true;
    }

    /** Punto a una fracción f del recorrido en L (primero en X, luego en Y) entre dos esquinas. */
    static Ubicacion enL(Ubicacion a, Ubicacion b, double f) {
        int dx = b.getPosX() - a.getPosX();
        int dy = b.getPosY() - a.getPosY();
        double total = Math.abs(dx) + Math.abs(dy);
        if (total == 0) return new Ubicacion(a.getPosX(), a.getPosY());
        double recorrido = f * total;
        if (recorrido <= Math.abs(dx)) {
            return new Ubicacion((int) Math.round(a.getPosX() + Math.signum(dx) * recorrido), a.getPosY());
        }
        return new Ubicacion(b.getPosX(), (int) Math.round(a.getPosY() + Math.signum(dy) * (recorrido - Math.abs(dx))));
    }

    private static boolean esAnterior(Ruta a, Ruta b) {
        if (a.getFechaHoraGeneracion() == null || b.getFechaHoraGeneracion() == null) {
            return a.getId() != null && b.getId() != null && a.getId() < b.getId();
        }
        return a.getFechaHoraGeneracion().isBefore(b.getFechaHoraGeneracion());
    }
}
