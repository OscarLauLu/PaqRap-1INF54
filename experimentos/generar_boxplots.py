import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import os

def generar_graficos():
    archivo_csv = 'plantilla_resultados_5d.csv'
    
    if not os.path.exists(archivo_csv):
        print(f"Error: No se encontró el archivo {archivo_csv} en este directorio.")
        return

    # 1. Cargar datos
    df = pd.read_csv(archivo_csv)

    # Limpiar columnas por seguridad
    df['volumen'] = df['volumen'].astype(str).str.strip().str.capitalize()
    df['disrupcion'] = df['disrupcion'].astype(str).str.strip().str.capitalize()
    df['algoritmo'] = df['algoritmo'].astype(str).str.strip().str.upper()

    # Crear columna combinada para el eje X
    df['Escenario'] = df['volumen'] + '\n' + df['disrupcion']

    # Orden lógico de los escenarios
    orden_escenarios = [
        "Bajo\nBajo", "Bajo\nAlto",
        "Medio\nBajo", "Medio\nAlto",
        "Alto\nBajo", "Alto\nAlto"
    ]

    # Estilo visual moderno y limpio
    sns.set_theme(style="whitegrid")
    colores = {"ALNS": "#4C72B0", "ACO": "#DD8452"} # Azul y Naranja clásico

    # ==========================================
    # GRÁFICO 1: Boxplot de Cumplimiento Global
    # ==========================================
    plt.figure(figsize=(12, 6))
    sns.boxplot(
        data=df, 
        x='Escenario', 
        y='pct_cumplimiento_global', 
        hue='algoritmo',
        order=orden_escenarios,
        palette=colores,
        linewidth=1.5
    )
    plt.title('Distribución del Cumplimiento Global (%) por Escenario: ALNS vs ACO', fontsize=14, pad=15, fontweight='bold')
    plt.ylabel('Cumplimiento Global (%)', fontsize=12)
    plt.xlabel('Combinación Volumen - Disrupción', fontsize=12)
    plt.legend(title='Algoritmo', loc='upper right')
    plt.tight_layout()
    plt.savefig('boxplot_cumplimiento.png', dpi=300)
    print("✅ Guardado con éxito: boxplot_cumplimiento.png")

    # ==========================================
    # GRÁFICO 2: Boxplot de Tiempos de Ejecución
    # ==========================================
    plt.figure(figsize=(12, 6))
    sns.boxplot(
        data=df, 
        x='Escenario', 
        y='tiempo_ejecucion_ms', 
        hue='algoritmo',
        order=orden_escenarios,
        palette=colores,
        linewidth=1.5
    )
    plt.title('Tiempos de Ejecución (escala logarítmica) por Escenario: ALNS vs ACO', fontsize=14, pad=15, fontweight='bold')
    plt.ylabel('Tiempo de cómputo (ms)', fontsize=12)
    plt.xlabel('Combinación Volumen - Disrupción', fontsize=12)
    plt.yscale('log') # ESCALA LOGARÍTMICA OBLIGATORIA
    plt.legend(title='Algoritmo', loc='upper right')
    plt.tight_layout()
    plt.savefig('boxplot_tiempos.png', dpi=300)
    print("✅ Guardado con éxito: boxplot_tiempos.png")

if __name__ == "__main__":
    generar_graficos()
