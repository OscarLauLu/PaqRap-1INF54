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
resultados = []

for vol in volumenes:
    df_vol = df[df['combinacion'] == vol]
    if df_vol.empty:
        continue
    
    alns = df_vol[df_vol['algoritmo'].str.lower() == 'alns'].sort_values('replica')
    aco = df_vol[df_vol['algoritmo'].str.lower() == 'aco'].sort_values('replica')
    
    if len(alns) != len(aco) or len(alns) == 0:
        continue
        
    pct_alns = alns['pct_cumplimiento_global'].values
    pct_aco = aco['pct_cumplimiento_global'].values
    time_alns = alns['tiempo_ejecucion_ms'].values
    time_aco = aco['tiempo_ejecucion_ms'].values
    
    diff_pct = pct_alns - pct_aco
    
    _, p_shapiro = stats.shapiro(diff_pct) if np.std(diff_pct) > 0 else (0, 1.0)
    w_stat, p_wilcoxon = stats.wilcoxon(pct_alns, pct_aco, alternative='two-sided') if np.any(diff_pct != 0) else (0, 1.0)
    
    n_diff = len(diff_pct[diff_pct != 0])
    efecto_r = calculate_effect_size(w_stat, n_diff)
    
    resultados.append({
        'Volumen': vol,
        'Mediana ALNS (%)': np.median(pct_alns),
        'Mediana ACO (%)': np.median(pct_aco),
        'T. ALNS (ms)': np.median(time_alns),
        'T. ACO (ms)': np.median(time_aco),
        'Shapiro p-val': p_shapiro,
        'Wilcoxon p-val': p_wilcoxon,
        'Tamaño Efecto (r)': efecto_r
    })

res_df = pd.DataFrame(resultados)
print("=== TABLA RESUMEN ESTADÍSTICA (Para Sección 5.1) ===")
print(res_df.to_string(index=False))

sns.set_theme(style="whitegrid")
plt.figure(figsize=(10, 6))
sns.barplot(data=df, x='combinacion', y='pct_cumplimiento_global', hue='algoritmo', order=volumenes, estimator=np.median, errorbar=None)
plt.title('Mediana de Cumplimiento Global (%): ALNS vs ACO')
plt.ylabel('Mediana de Cumplimiento (%)')
plt.xlabel('Volumen de Pedidos (Bloqueos nominales, Sin averías)')
plt.savefig('grafico_cumplimiento.png', dpi=300)

plt.figure(figsize=(10, 6))
ax = sns.barplot(data=df, x='combinacion', y='tiempo_ejecucion_ms', hue='algoritmo', order=volumenes, estimator=np.median, errorbar=None)
plt.title('Tiempo de Cómputo (ms, log): ALNS vs ACO')
plt.ylabel('Tiempo de cómputo (ms)')
plt.xlabel('Volumen de Pedidos')
ax.set_yscale("log")
plt.savefig('grafico_tiempo_log.png', dpi=300)

print("\nImágenes generadas: grafico_cumplimiento.png, grafico_tiempo_log.png")
