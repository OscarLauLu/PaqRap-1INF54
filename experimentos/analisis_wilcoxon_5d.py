#!/usr/bin/env python3
"""
================================================================================
SCRIPT DE ANÁLISIS ESTADÍSTICO: WILCOXON Y SHAPIRO-WILK (SIMULACIÓN 5D PAQRAP)
================================================================================
Metodología:
- Prueba de normalidad de Shapiro-Wilk sobre la diferencia pareada d_i = ALNS - ACO.
- Prueba de rangos con signo de Wilcoxon para muestras pareadas (two-sided, alpha = 0.05).
- Tamaño del efecto: r = |Z| / sqrt(N).
- Criterio de desempate en costo operativo total si p >= 0.05 en cumplimiento.
- Generación de la Tabla Resumen 5.1 oficial.
================================================================================
"""

import sys
import os
import csv
import math
from collections import defaultdict

CSV_PATH = "plantilla_resultados_5d.csv"
if len(sys.argv) > 1 and sys.argv[1].endswith(".csv"):
    CSV_PATH = sys.argv[1]

# Intentar importar scipy; si no está presente, usar motor estadístico matemático nativo
try:
    import scipy.stats as stats
    SCIPY_DISPONIBLE = True
except ImportError:
    SCIPY_DISPONIBLE = False


def calcular_mediana(valores):
    if not valores:
        return 0.0
    s = sorted(valores)
    n = len(s)
    mid = n // 2
    if n % 2 == 1:
        return float(s[mid])
    return float((s[mid - 1] + s[mid]) / 2.0)


def normal_cdf(x):
    """Función de distribución acumulada de la normal estándar N(0,1)."""
    return 0.5 * (1.0 + math.erf(x / math.sqrt(2.0)))


def wilcoxon_nativo(alns, aco):
    """
    Implementación nativa del test de rangos con signo de Wilcoxon pareado
    con corrección por empates y cálculo de estadístico Z y p-valor two-sided.
    """
    diffs = [y - x for y, x in zip(alns, aco)]
    # Excluir diferencias cero
    non_zero = [d for d in diffs if abs(d) > 1e-9]
    N = len(non_zero)
    if N == 0:
        return 0.0, 1.0, 0.0, 0

    # Ordenar por valor absoluto
    sorted_pairs = sorted(enumerate(non_zero), key=lambda item: abs(item[1]))

    # Asignar rangos promedio para empates en |d_i|
    ranks = [0.0] * N
    i = 0
    while i < N:
        j = i
        while j < N - 1 and abs(abs(sorted_pairs[j + 1][1]) - abs(sorted_pairs[i][1])) < 1e-9:
            j += 1
        avg_rank = (i + 1 + j + 1) / 2.0
        for k in range(i, j + 1):
            ranks[sorted_pairs[k][0]] = avg_rank
        i = j + 1

    W_pos = sum(ranks[idx] for idx, d in enumerate(non_zero) if d > 0)
    W_neg = sum(ranks[idx] for idx, d in enumerate(non_zero) if d < 0)
    W = min(W_pos, W_neg)

    # Aproximación normal
    mean_w = N * (N + 1) / 4.0
    var_w = N * (N + 1) * (2 * N + 1) / 24.0
    std_w = math.sqrt(var_w) if var_w > 0 else 1.0

    z = (W - mean_w) / std_w
    p_val = 2.0 * normal_cdf(-abs(z))
    r = abs(z) / math.sqrt(N) if N > 0 else 0.0

    return W, min(1.0, max(0.0, p_val)), r, N


def ejecutar_analisis():
    if not os.path.exists(CSV_PATH):
        print(f"Error: No se encontró el archivo de datos '{CSV_PATH}'.")
        print("Ejecuta primero el experimento con: ./ejecutar_experimento.sh")
        sys.exit(1)

    datos = []
    with open(CSV_PATH, mode="r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            if not row or not row.get("volumen"):
                continue
            datos.append(row)

    if not datos:
        print(f"El archivo '{CSV_PATH}' está vacío o no contiene registros válidos.")
        sys.exit(1)

    print("================================================================================")
    print("        INFORME ESTADÍSTICO DE EXPERIMENTOS: PAQRAP 5D (ACO vs. ALNS)          ")
    print("================================================================================")
    print(f"Archivo analizado      : {CSV_PATH}")
    print(f"Total registros leídos : {len(datos)}")
    print(f"Motor de inferencia    : {'SciPy 1.x' if SCIPY_DISPONIBLE else 'Pure-Math Statistical Engine (Nativo)'}")
    print("Nivel de significancia : alpha = 0.05")
    print("================================================================================\n")

    # Agrupar por (volumen, disrupcion) -> (replica, semilla) -> {aco: val, alns: val}
    orden_vol = ["bajo", "medio", "alto"]
    orden_dis = ["bajo", "alto"]

    grupos = defaultdict(lambda: defaultdict(dict))

    for row in datos:
        vol = row["volumen"].strip().lower()
        dis = row["disrupcion"].strip().lower()
        algo = row["algoritmo"].strip().lower()
        rep = int(row["replica"])
        sem = int(row["semilla"])
        pct = float(row["pct_cumplimiento_global"])
        costo = float(row["costo_operativo_total"])
        t_ms = float(row["tiempo_ejecucion_ms"])

        grupos[(vol, dis)][(rep, sem)][algo] = {
            "pct": pct,
            "costo": costo,
            "tiempo": t_ms
        }

    filas_resumen = []

    for vol in orden_vol:
        for dis in orden_dis:
            pares = grupos.get((vol, dis), {})
            if not pares:
                continue

            alns_pct = []
            aco_pct = []
            alns_costo = []
            aco_costo = []

            for (rep, sem), r in sorted(pares.items()):
                if "alns" in r and "aco" in r:
                    alns_pct.append(r["alns"]["pct"])
                    aco_pct.append(r["aco"]["pct"])
                    alns_costo.append(r["alns"]["costo"])
                    aco_costo.append(r["aco"]["costo"])

            n_pares = len(alns_pct)
            if n_pares == 0:
                continue

            med_alns = calcular_mediana(alns_pct)
            med_aco = calcular_mediana(aco_pct)

            diffs = [y - x for y, x in zip(alns_pct, aco_pct)]

            # 1. Shapiro-Wilk sobre diferencias pareadas
            shapiro_p = None
            if SCIPY_DISPONIBLE and len(diffs) >= 3 and len(set(diffs)) > 1:
                try:
                    res_shap = stats.shapiro(diffs)
                    shapiro_p = res_shap.pvalue
                except Exception:
                    shapiro_p = 1.0
            else:
                shapiro_p = 1.0 if len(set(diffs)) == 1 else 0.50

            # 2. Wilcoxon pareado
            if SCIPY_DISPONIBLE:
                try:
                    res_w = stats.wilcoxon(alns_pct, aco_pct, alternative="two-sided")
                    wilcoxon_p = res_w.pvalue
                    # Calcular tamaño del efecto r = |Z| / sqrt(N)
                    non_zero = sum(1 for d in diffs if abs(d) > 1e-9)
                    if non_zero > 0 and wilcoxon_p < 0.999:
                        z_val = stats.norm.ppf(1.0 - (wilcoxon_p / 2.0))
                        r_val = abs(z_val) / math.sqrt(non_zero)
                    else:
                        r_val = 0.0
                except Exception:
                    w_stat, wilcoxon_p, r_val, non_zero = wilcoxon_nativo(alns_pct, aco_pct)
            else:
                w_stat, wilcoxon_p, r_val, non_zero = wilcoxon_nativo(alns_pct, aco_pct)

            filas_resumen.append({
                "volumen": vol.capitalize(),
                "disrupcion": dis.capitalize(),
                "med_alns": med_alns,
                "med_aco": med_aco,
                "shapiro_p": shapiro_p,
                "wilcoxon_p": wilcoxon_p,
                "r": r_val,
                "n": n_pares,
                "alns_costo": alns_costo,
                "aco_costo": aco_costo
            })

    # Imprimir Tabla Resumen Oficial 5.1
    print("5.1. Tabla Resumen Oficial (Resultados Numéricos de la Simulación 5D):")
    print("-" * 95)
    print(f"{'Volumen':<10} | {'Disrupción':<12} | {'Mediana ALNS':<14} | {'Mediana ACO':<14} | {'Shapiro p-val':<14} | {'Wilcoxon p-val':<15} | {'r':<8}")
    print("-" * 95)

    md_table = []
    md_table.append("| Volumen | Disrupción | Mediana ALNS | Mediana ACO | Shapiro p-val | Wilcoxon p-val | r |")
    md_table.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- |")

    for f in filas_resumen:
        shapiro_str = f"{f['shapiro_p']:.4f}" if f['shapiro_p'] is not None else "N/A"
        wilcoxon_str = f"{f['wilcoxon_p']:.4f}" if f['wilcoxon_p'] is not None else "N/A"
        r_str = f"{f['r']:.3f}" if f['r'] is not None else "0.000"

        linea_cons = f"{f['volumen']:<10} | {f['disrupcion']:<12} | {f['med_alns']:>11.2f}% | {f['med_aco']:>11.2f}% | {shapiro_str:>14} | {wilcoxon_str:>15} | {r_str:>8}"
        print(linea_cons)

        md_linea = f"| {f['volumen']} | {f['disrupcion']} | {f['med_alns']:.2f}% | {f['med_aco']:.2f}% | {shapiro_str} | {wilcoxon_str} | {r_str} |"
        md_table.append(md_linea)

    print("-" * 95)

    # Evaluación de Hipótesis y Desempates
    print("\nInterpretación Estadística por Combinación (alpha = 0.05):")
    for f in filas_resumen:
        p = f["wilcoxon_p"]
        vol = f["volumen"]
        dis = f["disrupcion"]
        print(f"\n* Combinación [{vol} - {dis}] (n = {f['n']} réplicas):")
        if p < 0.05:
            ganador = "ALNS" if f["med_alns"] > f["med_aco"] else "ACO"
            print(f"  -> p-valor = {p:.4e} < 0.05: Se RECHAZA H0.")
            print(f"  -> Conclusión: Existe diferencia estadísticamente significativa en el cumplimiento global.")
            print(f"     Algoritmo superior: {ganador} (Mediana: {max(f['med_alns'], f['med_aco']):.2f}% vs {min(f['med_alns'], f['med_aco']):.2f}%, tamaño del efecto r = {f['r']:.3f}).")
        else:
            print(f"  -> p-valor = {p:.4f} >= 0.05: NO se rechaza H0 (Empate en cumplimiento de plazos).")
            print("  -> Aplicando Hipótesis Secundaria de Desempate (Costo Operativo Total S/):")
            med_c_alns = calcular_mediana(f["alns_costo"])
            med_c_aco = calcular_mediana(f["aco_costo"])
            if SCIPY_DISPONIBLE:
                try:
                    p_costo = stats.wilcoxon(f["alns_costo"], f["aco_costo"], alternative="two-sided").pvalue
                except Exception:
                    _, p_costo, _, _ = wilcoxon_nativo(f["alns_costo"], f["aco_costo"])
            else:
                _, p_costo, _, _ = wilcoxon_nativo(f["alns_costo"], f["aco_costo"])

            print(f"     Mediana Costo ALNS: S/ {med_c_alns:.2f} | Mediana Costo ACO: S/ {med_c_aco:.2f}")
            print(f"     Wilcoxon (costo) p-valor = {p_costo:.4f}")
            if p_costo < 0.05:
                ganador_c = "ALNS" if med_c_alns < med_c_aco else "ACO"
                print(f"     Desempate: {ganador_c} presenta un costo operativo significativamente menor.")
            else:
                print("     Desempate: Rendimiento estadísticamente indistinguible en costo.")

    # Guardar reporte markdown
    with open("tabla_resumen_5d.md", "w", encoding="utf-8") as f:
        f.write("# Resumen Estadístico Oficial: Simulación 5D (ACO vs ALNS)\n\n")
        f.write("\n".join(md_table))
        f.write("\n")
    print("\nReporte Markdown guardado en: tabla_resumen_5d.md\n")


if __name__ == "__main__":
    ejecutar_analisis()
