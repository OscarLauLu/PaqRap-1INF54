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

metricas = {
    'Cumplimiento Global (%)': 'pct_cumplimiento_global',
    'Cumplimiento Producto P (%)': 'pct_cumplimiento_producto_p',
    'Makespan (min)': 'makespan_min',
    'Tiempo Cómputo (ms)': 'tiempo_ejecucion_ms'
}

for nombre, col in metricas.items():
    print(f"\n=== Resumen Global ({nombre}) ===")
    print("ALNS:", calc_stats(alns[col]))
    print("ACO:", calc_stats(aco[col]))
    print("Wilcoxon p-val:", stats.wilcoxon(alns[col], aco[col]).pvalue)

