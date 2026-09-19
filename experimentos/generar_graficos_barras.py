import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import os

def generar_graficos():
    archivo_csv = 'plantilla_resultados_5d.csv'
    
    if not os.path.exists(archivo_csv):
        print(f"Error: No se encontró el archivo {archivo_csv} en este directorio.")
        return

    df = pd.read_csv(archivo_csv)

    df['volumen'] = df['volumen'].astype(str).str.strip().str.capitalize()
    df['disrupcion'] = df['disrupcion'].astype(str).str.strip().str.capitalize()
    df['algoritmo'] = df['algoritmo'].astype(str).str.strip().str.upper()

    df['Escenario'] = df['volumen'] + '-\n' + df['disrupcion']

    orden_escenarios = [
        "Bajo-\nBajo", "Bajo-\nAlto",
        "Medio-\nBajo", "Medio-\nAlto",
        "Alto-\nBajo", "Alto-\nAlto"
    ]

    medianas = df.groupby(['Escenario', 'algoritmo'])['pct_cumplimiento_global'].median().reset_index()
    medianas_tiempo = df.groupby(['Escenario', 'algoritmo'])['tiempo_ejecucion_ms'].median().reset_index()

    sns.set_theme(style="darkgrid")
    colores = {"ALNS": "#4C72B0", "ACO": "#DD8452"}

    # GRÁFICO 1
    plt.figure(figsize=(10, 5))
    ax1 = sns.barplot(
        data=medianas, 
        x='Escenario', 
        y='pct_cumplimiento_global', 
        hue='algoritmo',
        order=orden_escenarios,
        palette=colores
    )
    plt.title('Mediana de cumplimiento global (%): ALNS vs ACO por combinación', fontweight='bold')
    plt.ylabel('Mediana de cumplimiento global (%)')
    plt.xlabel('Combinación Volumen-Disrupción')
    
    for container in ax1.containers:
        ax1.bar_label(container, fmt='%.1f', padding=1, size=8)

    plt.tight_layout()
    plt.savefig('grafico_cumplimiento_barras.png', dpi=300)
    print("Guardado: grafico_cumplimiento_barras.png")

    # GRÁFICO 2
    plt.figure(figsize=(10, 5))
    ax2 = sns.barplot(
        data=medianas_tiempo, 
        x='Escenario', 
        y='tiempo_ejecucion_ms', 
        hue='algoritmo',
        order=orden_escenarios,
        palette=colores
    )
    plt.title('Tiempo de cómputo (ms): ALNS vs ACO por combinación', fontweight='bold')
    plt.ylabel('Tiempo de cómputo (ms, escala log)')
    plt.xlabel('Combinación Volumen-Disrupción')
    plt.yscale('log')
    
    for container in ax2.containers:
        ax2.bar_label(container, fmt='%.0f', padding=1, size=8)

    plt.tight_layout()
    plt.savefig('grafico_tiempos_barras.png', dpi=300)
    print("Guardado: grafico_tiempos_barras.png")

if __name__ == "__main__":
    generar_graficos()
