package com.paqrap.logistics.redvial.model;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa la red vial y geográfica ortogonal de la ciudad (70x50 km, nodos cada 1 km, sin diagonales ni curvas).
 * Provee cálculo de distancias ortogonales, pathfinding BFS respetando bloqueos viales, y gestión de tramos.
 */
@Slf4j
@Component
public class RedVial {

    @Getter
    private final int anchoKm = 70;

    @Getter
    private final int altoKm = 50;

    @Getter
    private final int separacionNodosKm = 1;

    private final Nodo[][] mallaNodos = new Nodo[anchoKm + 1][altoKm + 1];
    private final Map<String, Tramo> tramos = new ConcurrentHashMap<>();
    private final Set<String> tramosBloqueadosActivos = ConcurrentHashMap.newKeySet();

    public static class BloqueoVialInfo {
        private final LocalDateTime inicio;
        private final LocalDateTime fin;
        private final Set<String> aristas;

        public BloqueoVialInfo(LocalDateTime inicio, LocalDateTime fin, Set<String> aristas) {
            this.inicio = inicio;
            this.fin = fin;
            this.aristas = aristas;
        }

        public boolean estaActivoEn(LocalDateTime t) {
            if (t == null || inicio == null || fin == null) return false;
            return !t.isBefore(inicio) && !t.isAfter(fin);
        }

        public Set<String> getAristas() {
            return aristas;
        }
    }

    private final List<BloqueoVialInfo> listaBloqueos = new java.util.concurrent.CopyOnWriteArrayList<>();

    private LocalDateTime lastInstanteCache = null;
    private Set<String> lastAristasCache = null;

    public Set<String> aristasBloqueadasEn(LocalDateTime instante) {
        if (instante == null || listaBloqueos.isEmpty()) {
            return Collections.emptySet();
        }
        if (instante.equals(lastInstanteCache) && lastAristasCache != null) {
            return lastAristasCache;
        }
        Set<String> activas = new HashSet<>();
        for (BloqueoVialInfo b : listaBloqueos) {
            if (b.estaActivoEn(instante)) {
                activas.addAll(b.getAristas());
            }
        }
        lastInstanteCache = instante;
        lastAristasCache = activas;
        return activas;
    }

    @PostConstruct
    public void inicializarRed() {
        log.info("Inicializando RedVial de {}x{} km con nodos cada {} km...", anchoKm, altoKm, separacionNodosKm);
        for (int x = 0; x <= anchoKm; x++) {
            for (int y = 0; y <= altoKm; y++) {
                mallaNodos[x][y] = new Nodo(x, y);
            }
        }

        for (int x = 0; x <= anchoKm; x++) {
            for (int y = 0; y <= altoKm; y++) {
                Nodo actual = mallaNodos[x][y];
                // Conexión horizontal derecha
                if (x + 1 <= anchoKm) {
                    Nodo derecha = mallaNodos[x + 1][y];
                    registrarTramo(actual, derecha);
                }
                // Conexión vertical arriba
                if (y + 1 <= altoKm) {
                    Nodo arriba = mallaNodos[x][y + 1];
                    registrarTramo(actual, arriba);
                }
            }
        }
        log.info("RedVial inicializada exitosamente con {} nodos y {} tramos bidireccionales.",
                (anchoKm + 1) * (altoKm + 1), tramos.size());
    }

    private void registrarTramo(Nodo origen, Nodo destino) {
        Tramo tramo = new Tramo(origen, destino);
        tramos.put(tramo.getClaveCanonica(), tramo);
    }

    /**
     * Obtiene el nodo correspondiente a las coordenadas (x, y).
     */
    public Nodo obtenerNodo(int x, int y) {
        if (x < 0 || x > anchoKm || y < 0 || y > altoKm) {
            return null;
        }
        return mallaNodos[x][y];
    }

    /**
     * Calcula la distancia Manhattan en kilómetros entre dos nodos.
     */
    public double calcularDistancia(Nodo origen, Nodo destino) {
        if (origen == null || destino == null) {
            return 0.0;
        }
        return Math.abs(origen.getX() - destino.getX()) + Math.abs(origen.getY() - destino.getY());
    }

    /**
     * Verifica si un tramo vial está bloqueado en un instante simulado.
     */
    public boolean estaBloqueado(Tramo tramo, LocalDateTime instante) {
        if (tramo == null) return false;
        if (instante == null) return tramo.isBloqueado();
        return aristasBloqueadasEn(instante).contains(tramo.getClaveCanonica());
    }

    public boolean estaBloqueadoSegmento(int x1, int y1, int x2, int y2, LocalDateTime instante) {
        String clave = obtenerClaveCanonica(x1, y1, x2, y2);
        return aristasBloqueadasEn(instante).contains(clave);
    }

    /**
     * Calcula la ruta mínima de tramos ortogonales entre origen y destino mediante BFS,
     * respetando los bloqueos temporales vigentes en el instante dado.
     * Si no hay bloqueo, devuelve los tramos de la trayectoria ortogonal directa.
     */
    public List<Tramo> calcularRutaMinima(Nodo origen, Nodo destino, LocalDateTime instante) {
        if (origen == null || destino == null) {
            return Collections.emptyList();
        }
        if (origen.equals(destino)) {
            return Collections.emptyList();
        }

        Queue<Nodo> cola = new ArrayDeque<>();
        Map<Nodo, Nodo> padre = new HashMap<>();
        Set<Nodo> visitados = new HashSet<>();

        cola.add(origen);
        visitados.add(origen);

        boolean encontrado = false;
        int[][] direcciones = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!cola.isEmpty()) {
            Nodo actual = cola.poll();
            if (actual.equals(destino)) {
                encontrado = true;
                break;
            }

            for (int[] dir : direcciones) {
                int nx = actual.getX() + dir[0];
                int ny = actual.getY() + dir[1];

                if (nx >= 0 && nx <= anchoKm && ny >= 0 && ny <= altoKm) {
                    Nodo vecino = mallaNodos[nx][ny];
                    if (!visitados.contains(vecino)) {
                        String claveTramo = obtenerClaveCanonica(actual.getX(), actual.getY(), nx, ny);
                        Tramo tramo = tramos.get(claveTramo);
                        if (tramo != null && !estaBloqueado(tramo, instante)) {
                            visitados.add(vecino);
                            padre.put(vecino, actual);
                            cola.add(vecino);
                        }
                    }
                }
            }
        }

        if (!encontrado) {
            log.warn("No se encontró ruta libre de bloqueos entre {} y {}. Intentando ruta de contingencia.", origen, destino);
            return construirRutaOrtogonalDirecta(origen, destino);
        }

        List<Tramo> ruta = new ArrayList<>();
        Nodo paso = destino;
        while (!paso.equals(origen)) {
            Nodo p = padre.get(paso);
            String clave = obtenerClaveCanonica(p.getX(), p.getY(), paso.getX(), paso.getY());
            Tramo t = tramos.get(clave);
            if (t != null) {
                ruta.add(0, t);
            }
            paso = p;
        }
        return ruta;
    }

    private List<Tramo> construirRutaOrtogonalDirecta(Nodo origen, Nodo destino) {
        List<Tramo> tramosDirectos = new ArrayList<>();
        int cx = origen.getX();
        int cy = origen.getY();

        while (cx != destino.getX()) {
            int sigX = (destino.getX() > cx) ? cx + 1 : cx - 1;
            String clave = obtenerClaveCanonica(cx, cy, sigX, cy);
            Tramo t = tramos.get(clave);
            if (t != null) tramosDirectos.add(t);
            cx = sigX;
        }
        while (cy != destino.getY()) {
            int sigY = (destino.getY() > cy) ? cy + 1 : cy - 1;
            String clave = obtenerClaveCanonica(cx, cy, cx, sigY);
            Tramo t = tramos.get(clave);
            if (t != null) tramosDirectos.add(t);
            cy = sigY;
        }
        return tramosDirectos;
    }

    public void aplicarBloqueo(Bloqueo bloqueo) {
        if (bloqueo == null || bloqueo.getCoordenadasNodos() == null) return;
        String[] coords = bloqueo.getCoordenadasNodos().split(",");
        Set<String> aristas = new HashSet<>();
        for (int i = 0; i + 3 < coords.length; i += 2) {
            try {
                int x1 = Integer.parseInt(coords[i].trim());
                int y1 = Integer.parseInt(coords[i + 1].trim());
                int x2 = Integer.parseInt(coords[i + 2].trim());
                int y2 = Integer.parseInt(coords[i + 3].trim());
                procesarBloqueoSegmento(x1, y1, x2, y2, true, bloqueo.getFechaHoraInicio(), bloqueo.getFechaHoraFin());

                int stepX = Integer.compare(x2, x1);
                int stepY = Integer.compare(y2, y1);
                int cx = x1;
                int cy = y1;
                while (cx != x2) {
                    int nx = cx + stepX;
                    aristas.add(obtenerClaveCanonica(cx, cy, nx, cy));
                    cx = nx;
                }
                while (cy != y2) {
                    int ny = cy + stepY;
                    aristas.add(obtenerClaveCanonica(cx, cy, cx, ny));
                    cy = ny;
                }
            } catch (Exception e) {
                log.error("Error al parsear coordenadas de bloqueo: {}", e.getMessage());
            }
        }
        if (!aristas.isEmpty()) {
            listaBloqueos.add(new BloqueoVialInfo(bloqueo.getFechaHoraInicio(), bloqueo.getFechaHoraFin(), aristas));
        }
    }

    public void removerBloqueo(Bloqueo bloqueo) {
        if (bloqueo == null || bloqueo.getCoordenadasNodos() == null) return;
        String[] coords = bloqueo.getCoordenadasNodos().split(",");
        for (int i = 0; i + 3 < coords.length; i += 2) {
            try {
                int x1 = Integer.parseInt(coords[i].trim());
                int y1 = Integer.parseInt(coords[i + 1].trim());
                int x2 = Integer.parseInt(coords[i + 2].trim());
                int y2 = Integer.parseInt(coords[i + 3].trim());
                procesarBloqueoSegmento(x1, y1, x2, y2, false, null, null);
            } catch (Exception e) {
                log.error("Error al remover tramo bloqueado: {}", e.getMessage());
            }
        }
    }

    private void procesarBloqueoSegmento(int x1, int y1, int x2, int y2, boolean bloquear, LocalDateTime inicio, LocalDateTime fin) {
        int stepX = Integer.compare(x2, x1);
        int stepY = Integer.compare(y2, y1);

        if (x1 == x2 && y1 == y2) {
            return;
        }

        int cx = x1;
        int cy = y1;

        while (cx != x2) {
            int nx = cx + stepX;
            String clave = obtenerClaveCanonica(cx, cy, nx, cy);
            actualizarEstadoTramo(clave, bloquear, inicio, fin);
            cx = nx;
        }
        while (cy != y2) {
            int ny = cy + stepY;
            String clave = obtenerClaveCanonica(cx, cy, cx, ny);
            actualizarEstadoTramo(clave, bloquear, inicio, fin);
            cy = ny;
        }
    }

    private void actualizarEstadoTramo(String clave, boolean bloquear, LocalDateTime inicio, LocalDateTime fin) {
        Tramo tramo = tramos.get(clave);
        if (tramo != null) {
            tramo.setBloqueado(bloquear);
            if (bloquear) {
                tramo.setInicioBloqueo(inicio);
                tramo.setFinBloqueo(fin);
                tramosBloqueadosActivos.add(clave);
            } else {
                tramosBloqueadosActivos.remove(clave);
            }
        }
    }

    public static String obtenerClaveCanonica(int x1, int y1, int x2, int y2) {
        if (x1 < x2 || (x1 == x2 && y1 <= y2)) {
            return x1 + "," + y1 + "-" + x2 + "," + y2;
        } else {
            return x2 + "," + y2 + "-" + x1 + "," + y1;
        }
    }

    /**
     * Calcula la distancia más corta (en km) entre dos nodos considerando tramos bloqueados (RF-12).
     * Si no hay tramos bloqueados activos en el instante, retorna la distancia Manhattan directa.
     */
    public double distanciaMinima(Nodo origen, Nodo destino, LocalDateTime instante) {
        if (origen == null || destino == null) return Double.MAX_VALUE;
        if (origen.equals(destino)) return 0.0;

        Set<String> aristasBloqueadas = aristasBloqueadasEn(instante);
        double distManhattan = calcularDistancia(origen, destino);
        if (aristasBloqueadas.isEmpty()) {
            return distManhattan;
        }

        int[][] dist = new int[anchoKm + 1][altoKm + 1];
        for (int i = 0; i <= anchoKm; i++) {
            java.util.Arrays.fill(dist[i], -1);
        }

        // Usamos un array nativo para la cola para evitar objetos innecesarios
        // Tamaño máximo = Nodos = 3621
        int[] colaX = new int[4000];
        int[] colaY = new int[4000];
        int head = 0;
        int tail = 0;

        colaX[tail] = origen.getX();
        colaY[tail] = origen.getY();
        tail++;
        dist[origen.getX()][origen.getY()] = 0;

        int[][] direcciones = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int destX = destino.getX();
        int destY = destino.getY();

        while (head < tail) {
            int cx = colaX[head];
            int cy = colaY[head];
            head++;

            int d = dist[cx][cy];
            if (cx == destX && cy == destY) {
                return (double) d;
            }

            for (int[] dir : direcciones) {
                int nx = cx + dir[0];
                int ny = cy + dir[1];
                if (nx >= 0 && nx <= anchoKm && ny >= 0 && ny <= altoKm) {
                    if (dist[nx][ny] == -1) {
                        String claveTramo = obtenerClaveCanonica(cx, cy, nx, ny);
                        if (!aristasBloqueadas.contains(claveTramo)) {
                            dist[nx][ny] = d + 1;
                            colaX[tail] = nx;
                            colaY[tail] = ny;
                            tail++;
                        }
                    }
                }
            }
        }
        return distManhattan * 1.5;
    }

    // Caché de distancias expansivas por Nodo origen (Memoization)
    private final Map<Nodo, int[][]> distanciasCache = new ConcurrentHashMap<>();
    private Set<String> aristasEnDistanciasCache = null;

    /**
     * Calcula la distancia más corta desde el nodo origen hacia TODOS los nodos de la red en un solo BFS.
     * Retorna null si no hay bloqueos (para que el llamador use la distancia Manhattan directamente).
     */
    public int[][] distanciasDesde(Nodo origen, LocalDateTime instante) {
        Set<String> aristasBloqueadas = aristasBloqueadasEn(instante);
        
        // Invalidar caché si el mapa de calles bloqueadas cambió
        if (aristasEnDistanciasCache == null || !aristasEnDistanciasCache.equals(aristasBloqueadas)) {
            distanciasCache.clear();
            aristasEnDistanciasCache = new HashSet<>(aristasBloqueadas);
        }

        if (aristasBloqueadas.isEmpty()) {
            return null; // Sin bloqueos, la distancia Manhattan es la mínima
        }

        // Si ya calculamos la onda desde esta esquina, retornamos la copia de la RAM
        int[][] cached = distanciasCache.get(origen);
        if (cached != null) {
            return cached;
        }

        int[][] dist = new int[anchoKm + 1][altoKm + 1];
        for (int i = 0; i <= anchoKm; i++) {
            java.util.Arrays.fill(dist[i], -1);
        }

        int[] colaX = new int[4000];
        int[] colaY = new int[4000];
        int head = 0, tail = 0;

        colaX[tail] = origen.getX();
        colaY[tail] = origen.getY();
        tail++;
        dist[origen.getX()][origen.getY()] = 0;

        int[][] direcciones = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (head < tail) {
            int cx = colaX[head];
            int cy = colaY[head];
            head++;
            int d = dist[cx][cy];

            for (int[] dir : direcciones) {
                int nx = cx + dir[0];
                int ny = cy + dir[1];
                if (nx >= 0 && nx <= anchoKm && ny >= 0 && ny <= altoKm) {
                    if (dist[nx][ny] == -1) {
                        String claveTramo = obtenerClaveCanonica(cx, cy, nx, ny);
                        if (!aristasBloqueadas.contains(claveTramo)) {
                            dist[nx][ny] = d + 1;
                            colaX[tail] = nx;
                            colaY[tail] = ny;
                            tail++;
                        }
                    }
                }
            }
        }
        
        // Guardamos el cálculo en la memoria caché para la próxima hormiga
        distanciasCache.put(origen, dist);
        return dist;
    }

    public double distanciaMinima(Ubicacion origen, Ubicacion destino, LocalDateTime instante) {
        if (origen == null || destino == null) return Double.MAX_VALUE;
        Nodo n1 = obtenerNodo(origen.getPosX(), origen.getPosY());
        Nodo n2 = obtenerNodo(destino.getPosX(), destino.getPosY());
        return distanciaMinima(n1, n2, instante);
    }

    /**
     * Obtiene la lista ordenada de nodos en el camino mínimo libre de bloqueos viales.
     */
    public List<Nodo> caminoMinimoNodos(Nodo origen, Nodo destino, LocalDateTime instante) {
        if (origen == null || destino == null) return Collections.emptyList();
        if (origen.equals(destino)) return Collections.singletonList(origen);

        Queue<Nodo> cola = new ArrayDeque<>();
        Map<Nodo, Nodo> padre = new HashMap<>();
        Set<Nodo> visitados = new HashSet<>();
        cola.add(origen);
        visitados.add(origen);

        boolean encontrado = false;
        int[][] direcciones = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!cola.isEmpty()) {
            Nodo actual = cola.poll();
            if (actual.equals(destino)) {
                encontrado = true;
                break;
            }

            for (int[] dir : direcciones) {
                int nx = actual.getX() + dir[0];
                int ny = actual.getY() + dir[1];
                if (nx >= 0 && nx <= anchoKm && ny >= 0 && ny <= altoKm) {
                    Nodo vecino = mallaNodos[nx][ny];
                    if (!visitados.contains(vecino)) {
                        String claveTramo = obtenerClaveCanonica(actual.getX(), actual.getY(), nx, ny);
                        Tramo tramo = tramos.get(claveTramo);
                        if (tramo != null && !estaBloqueado(tramo, instante)) {
                            visitados.add(vecino);
                            padre.put(vecino, actual);
                            cola.add(vecino);
                        }
                    }
                }
            }
        }

        if (!encontrado) {
            return Collections.emptyList();
        }

        List<Nodo> camino = new ArrayList<>();
        Nodo paso = destino;
        while (!paso.equals(origen)) {
            camino.add(0, paso);
            paso = padre.get(paso);
        }
        camino.add(0, origen);
        return camino;
    }

    public List<Nodo> caminoMinimoNodos(Ubicacion origen, Ubicacion destino, LocalDateTime instante) {
        if (origen == null || destino == null) return Collections.emptyList();
        Nodo n1 = obtenerNodo(origen.getPosX(), origen.getPosY());
        Nodo n2 = obtenerNodo(destino.getPosX(), destino.getPosY());
        return caminoMinimoNodos(n1, n2, instante);
    }
}

