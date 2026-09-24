import pandas as pd
import numpy as np
from scipy import stats

df = pd.read_csv('plantilla_resultados_5d.csv')
alns = df[df['algoritmo'].str.lower() == 'alns']
aco = df[df['algoritmo'].str.lower() == 'aco']

def calc_stats(series):
    s = series[series > 0]
    return {
        'Media': series.mean(),
        'Media armónica': stats.hmean(s) if len(s) > 0 else 0,
        'Desv. estándar': series.std()
    }

print("=== Resumen Global (Cumplimiento %) ===")
print("ALNS:", calc_stats(alns['pct_cumplimiento_global']))
print("ACO:", calc_stats(aco['pct_cumplimiento_global']))
print("Wilcoxon p-val:", stats.wilcoxon(alns['pct_cumplimiento_global'], aco['pct_cumplimiento_global']).pvalue)

print("\n=== Resumen Global (Tiempos ms) ===")
print("ALNS:", calc_stats(alns['tiempo_ejecucion_ms']))
print("ACO:", calc_stats(aco['tiempo_ejecucion_ms']))
print("Wilcoxon p-val:", stats.wilcoxon(alns['tiempo_ejecucion_ms'], aco['tiempo_ejecucion_ms']).pvalue)
