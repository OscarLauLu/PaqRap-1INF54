import pandas as pd
import numpy as np
from scipy import stats
import matplotlib.pyplot as plt
import seaborn as sns
import math
import warnings
warnings.filterwarnings('ignore')

def calculate_effect_size(w_stat, n):
    if n == 0: return 0
    mu = n * (n + 1) / 4.0
    sigma = math.sqrt(n * (n + 1) * (2 * n + 1) / 24.0)
    if sigma == 0: return 0
    z = (w_stat - mu) / sigma
    return abs(z) / math.sqrt(n)

# 1. Cargar datos
df = pd.read_csv('plantilla_resultados_5d.csv')

# Extraer el volumen a mayúsculas
df['combinacion'] = df['volumen'].str.upper()
volumenes = ['BAJO', 'MEDIO', 'ALTO']

metricas = {
    'Cumplimiento Global (%)': 'pct_cumplimiento_global',
    'Cumplimiento Producto P (%)': 'pct_cumplimiento_producto_p',
    'Makespan (min)': 'makespan_min',
    'Tiempo Cómputo (ms)': 'tiempo_ejecucion_ms'
}

resultados_totales = []

for nombre_metrica, col_metrica in metricas.items():
    print(f"\n{'='*60}")
    print(f" ANÁLISIS PARA: {nombre_metrica}")
    print(f"{'='*60}")
    
    resultados_metrica = []
    
    for vol in volumenes:
        df_vol = df[df['combinacion'] == vol]
        if df_vol.empty:
            continue
        
        alns = df_vol[df_vol['algoritmo'].str.lower() == 'alns'].sort_values('replica')
        aco = df_vol[df_vol['algoritmo'].str.lower() == 'aco'].sort_values('replica')
        
        if len(alns) != len(aco) or len(alns) == 0:
            continue
            
        val_alns = alns[col_metrica].values
        val_aco = aco[col_metrica].values
        
        diff = val_alns - val_aco
        
        _, p_shapiro = stats.shapiro(diff) if np.std(diff) > 0 else (0, 1.0)
        w_stat, p_wilcoxon = stats.wilcoxon(val_alns, val_aco, alternative='two-sided') if np.any(diff != 0) else (0, 1.0)
        
        n_diff = len(diff[diff != 0])
        efecto_r = calculate_effect_size(w_stat, n_diff)
        
        resultados_metrica.append({
            'Volumen': vol,
            'Mediana ALNS': np.median(val_alns),
            'Mediana ACO': np.median(val_aco),
            'Shapiro p-val': p_shapiro,
            'Wilcoxon p-val': p_wilcoxon,
            'Tamaño Efecto (r)': efecto_r
        })
        
    if resultados_metrica:
        res_df = pd.DataFrame(resultados_metrica)
        print(res_df.to_string(index=False))
        resultados_totales.extend(resultados_metrica)

# Generar gráficos
sns.set_theme(style="whitegrid")

# Gráfico 1: Cumplimiento global
plt.figure(figsize=(10, 6))
sns.barplot(data=df, x='combinacion', y='pct_cumplimiento_global', hue='algoritmo', order=volumenes, estimator=np.median, errorbar=None)
plt.title('Mediana de Cumplimiento Global (%): ALNS vs ACO')
plt.ylabel('Mediana de Cumplimiento (%)')
plt.xlabel('Volumen de Pedidos')
plt.savefig('grafico_cumplimiento.png', dpi=300)

# Gráfico 2: Cumplimiento de producto P
plt.figure(figsize=(10, 6))
sns.barplot(data=df, x='combinacion', y='pct_cumplimiento_producto_p', hue='algoritmo', order=volumenes, estimator=np.median, errorbar=None)
plt.title('Mediana de Cumplimiento Producto P (%): ALNS vs ACO')
plt.ylabel('Mediana de Cumplimiento Producto P (%)')
plt.xlabel('Volumen de Pedidos')
plt.savefig('grafico_cumplimiento_producto_p.png', dpi=300)

# Gráfico 3: Makespan
plt.figure(figsize=(10, 6))
sns.barplot(data=df, x='combinacion', y='makespan_min', hue='algoritmo', order=volumenes, estimator=np.median, errorbar=None)
plt.title('Makespan (min): ALNS vs ACO')
plt.ylabel('Makespan (min)')
plt.xlabel('Volumen de Pedidos')
plt.savefig('grafico_makespan.png', dpi=300)

# Gráfico 4: Tiempo de cómputo log
plt.figure(figsize=(10, 6))
ax = sns.barplot(data=df, x='combinacion', y='tiempo_ejecucion_ms', hue='algoritmo', order=volumenes, estimator=np.median, errorbar=None)
plt.title('Tiempo de Cómputo (ms, log): ALNS vs ACO')
plt.ylabel('Tiempo de cómputo (ms)')
plt.xlabel('Volumen de Pedidos')
ax.set_yscale("log")
plt.savefig('grafico_tiempo_log.png', dpi=300)

print("\nImágenes generadas: grafico_cumplimiento.png, grafico_cumplimiento_producto_p.png, grafico_makespan.png, grafico_tiempo_log.png")
