# PAQRAP Frontend - Simulación 5D

Frontend desarrollado en **React + TypeScript + Vite + Tailwind CSS** consumiendo el backend Spring Boot de **PAQRAP** mediante una arquitectura de **Custom Hooks**.

## Requisitos
- Node.js >= 18
- pnpm (`pnpm -v`)

## Instrucciones de Ejecución

1. **Instalar dependencias:**
   ```bash
   cd frontend
   pnpm install
   ```

2. **Iniciar en modo desarrollo:**
   ```bash
   pnpm dev
   ```
   La aplicación se abrirá en `http://localhost:5173`.

3. **Construir para producción:**
   ```bash
   pnpm run build
   ```

## Arquitectura de Custom Hooks y APIs

- `useSimulacion`: Control de ciclo de vida del motor de simulación y sincronización del reloj simulado (`/api/simulacion/reloj`, `/api/simulacion/iniciar`, `/api/simulacion/detener`).
- `useAlmacenes`: Consulta en tiempo real de los almacenes (Central e Intermedios), niveles de stock y umbrales de ocupación (`/api/almacenes`).
- `useFlota`: Monitoreo de unidades de transporte (Autos, Motos, Bicicletas) y selección interactiva de unidad con detalles de ruta y conductor (`/api/flota/unidades`).
- `useIncidentes`: Historial y estado de bloqueos viales y averías mecánicas (`/api/mapa/incidencias`).
- `useMapaOperaciones`: Puntos de entrega/clientes y tramos viales (`/api/mapa/pedidos`).
- `useAlertas`: Alertas del panel de operaciones (`/api/mapa/alertas`).

## Integración con Backend
El archivo `vite.config.ts` incluye un proxy automático hacia `http://localhost:8080/api`. Si el backend no está activo, los hooks utilizan datos simulados idénticos a la pantalla de referencia para garantizar interactividad inmediata.
