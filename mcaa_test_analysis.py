#!/usr/bin/env python3
import os
import glob
import sys
from pathlib import Path

import pandas as pd
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime

REPORTS_DIR = "mcaa_test_reports"
FIGURES_DIR = os.path.join(REPORTS_DIR, "figures")
list_of_files = glob.glob(os.path.join(REPORTS_DIR, "*_benchmark_results.csv"))
if not list_of_files:
    raise FileNotFoundError(f"No CSV files found in {REPORTS_DIR}. Run BenchmarkRunner first.")
CSV_FILE = max(list_of_files, key=os.path.getmtime)
filename = os.path.basename(CSV_FILE)
timestamp = filename.split('_benchmark_results')[0]
SUMMARY_FILE = os.path.join(REPORTS_DIR, f"{timestamp}_summary.txt")

def load_data(path: str) -> pd.DataFrame:
    if not os.path.exists(path):
        print(f"ERROR: {path} not found. Run BenchmarkRunner first.")
        sys.exit(1)
    df = pd.read_csv(path)
    required_cols = [
        "seed", "numOrders", "numResources", "etaVariance",
        "weightStrategy", "algorithm", "serviceLevel",
        "totalCost", "executionTimeMs", "giniCoefficient",
    ]
    missing = [c for c in required_cols if c not in df.columns]
    if missing:
        print(f"ERROR: CSV missing columns: {missing}")
        sys.exit(1)
    print(f"Loaded {len(df)} rows from {path}")
    return df

def ensure_dir(path: str) -> None:
    Path(path).mkdir(parents=True, exist_ok=True)

def plot_pareto_frontier(df: pd.DataFrame, out_dir: str) -> None:
    fig, ax = plt.subplots(figsize=(10, 7))
    agg = df.groupby(["algorithm", "seed"]).agg(
        serviceLevel=("serviceLevel", "mean"),
        totalCost=("totalCost", "mean"),
    ).reset_index()

    colours = {"MCAA": "#e74c3c", "Vickrey": "#3498db", "DoubleAuction": "#2ecc71", "LP": "#9b59b6"}
    markers = {"MCAA": "o", "Vickrey": "s", "DoubleAuction": "D", "LP": "^"}

    all_points = []
    for algo in agg["algorithm"].unique():
        sub = agg[agg["algorithm"] == algo]
        ax.scatter(
            sub["serviceLevel"], sub["totalCost"],
            c=colours.get(algo, "gray"),
            marker=markers.get(algo, "o"),
            label=algo, alpha=0.6, edgecolors="k", linewidth=0.3, s=40,
        )
        all_points.append(sub[["serviceLevel", "totalCost"]].values)

    all_pts = np.vstack(all_points) if all_points else np.empty((0, 2))
    pareto_mask = np.ones(len(all_pts), dtype=bool)
    for i in range(len(all_pts)):
        svc_i, cost_i = all_pts[i, 0], all_pts[i, 1]
        for j in range(len(all_pts)):
            svc_j, cost_j = all_pts[j, 0], all_pts[j, 1]
            if (cost_j <= cost_i and svc_j >= svc_i) and \
               (cost_j < cost_i or svc_j > svc_i):
                pareto_mask[i] = False
                break

    pareto_pts = all_pts[pareto_mask]
    order = np.argsort(pareto_pts[:, 1])
    pareto_pts = pareto_pts[order]
    if len(pareto_pts) > 1:
        ax.plot(pareto_pts[:, 0], pareto_pts[:, 1],
                "k--", linewidth=1.5, label="Pareto frontier")

    ax.set_xlabel("Service Level (on-time fraction)", fontsize=12)
    ax.set_ylabel("Total Cost", fontsize=12)
    ax.set_title("Pareto Frontier: Service Level vs Total Cost", fontsize=14)
    ax.legend(fontsize=10)
    ax.grid(True, alpha=0.3)
    ax.annotate("<- Better", xy=(0.02, 0.02), xycoords="axes fraction", fontsize=9, color="gray")
    fig.tight_layout()
    path = os.path.join(out_dir, f"{timestamp}_pareto_frontier.png")
    fig.savefig(path, dpi=200, bbox_inches="tight")
    plt.close(fig)
    print(f"  Saved {path}")

def plot_execution_time_boxplot(df: pd.DataFrame, out_dir: str) -> None:
    fig, ax = plt.subplots(figsize=(8, 6))

    order = ["MCAA", "Vickrey", "DoubleAuction", "LP"]
    palette = {"MCAA": "#e74c3c", "Vickrey": "#3498db", "DoubleAuction": "#2ecc71", "LP": "#9b59b6"}

    sns.boxplot(
        data=df, x="algorithm", y="executionTimeMs",
        order=order, hue="algorithm", palette=palette, ax=ax,
        legend=False,
    )
    ax.set_yscale("log")
    ax.set_xlabel("Algorithm", fontsize=12)
    ax.set_ylabel("Execution Time (ms, log scale)", fontsize=12)
    ax.set_title("Execution Time per Algorithm", fontsize=14)
    ax.grid(axis="y", alpha=0.3)
    fig.tight_layout()
    path = os.path.join(out_dir, f"{timestamp}_execution_time_boxplot.png")
    fig.savefig(path, dpi=200, bbox_inches="tight")
    plt.close(fig)
    print(f"  Saved {path}")

def plot_service_level_heatmap(df: pd.DataFrame, out_dir: str) -> None:
    pivot = df.pivot_table(
        index="etaVariance", columns="weightStrategy",
        values="serviceLevel", aggfunc="mean",
    )
    pivot = pivot.reindex(index=[0.0, 0.1, 0.2, 0.3])
    pivot = pivot.reindex(columns=["balanced", "costHeavy", "timeHeavy"])
    fig, ax = plt.subplots(figsize=(8, 5))
    sns.heatmap(
        pivot, annot=True, fmt=".3f", cmap="RdYlGn", vmin=0, vmax=1,
        linewidths=0.5, ax=ax, cbar_kws={"label": "Mean Service Level"},
    )
    ax.set_title("Service Level: ETA Variance x Weight Strategy", fontsize=14)
    ax.set_ylabel("ETA Variance", fontsize=12)
    ax.set_xlabel("Weight Strategy", fontsize=12)
    fig.tight_layout()
    path = os.path.join(out_dir, f"{timestamp}_service_level_heatmap.png")
    fig.savefig(path, dpi=200, bbox_inches="tight")
    plt.close(fig)
    print(f"  Saved {path}")


def generate_summary(df: pd.DataFrame, out_file: str) -> None:
    lines = []
    lines.append("=" * 72)
    lines.append("BENCHMARK SUMMARY - MCAA vs Vickrey vs Double Auction vs LP")
    lines.append("=" * 72)
    lines.append(f"Total runs: {len(df)}")
    lines.append("")
    lines.append("Per-Algorithm Averages (mean +/- std):")
    lines.append("-" * 72)
    header = f"{'Algorithm':<16} {'ServiceLvl':>12} {'TotalCost':>12} {'Time(ms)':>12} {'Gini':>10}"
    lines.append(header)
    lines.append("-" * 72)
    for algo in ["MCAA", "Vickrey", "DoubleAuction", "LP"]:
        sub = df[df["algorithm"] == algo]
        if len(sub) == 0:
            continue
        svc = sub["serviceLevel"]
        cost = sub["totalCost"]
        time_ms = sub["executionTimeMs"]
        gini = sub["giniCoefficient"]
        lines.append(
            f"{algo:<16} "
            f"{svc.mean():>8.4f}+/-{svc.std():<6.4f}"
            f"{cost.mean():>10.0f}+/-{cost.std():<8.0f}"
            f"{time_ms.mean():>10.1f}+/-{time_ms.std():<8.1f}"
            f"{gini.mean():>8.4f}"
        )
    lines.append("")
    means = df.groupby("algorithm")[["serviceLevel", "totalCost", "executionTimeMs"]].mean()
    best_svc_algo = means["serviceLevel"].idxmax()
    best_cost_algo = means["totalCost"].idxmin()
    best_time_algo = means["executionTimeMs"].idxmin()
    lines.append("Best per Metric (by mean):")
    lines.append(f"  Highest Service Level: {best_svc_algo} ({means.loc[best_svc_algo, 'serviceLevel']:.4f})")
    lines.append(f"  Lowest Total Cost:      {best_cost_algo} ({means.loc[best_cost_algo, 'totalCost']:.0f})")
    lines.append(f"  Fastest Execution:      {best_time_algo} ({means.loc[best_time_algo, 'executionTimeMs']:.1f} ms)")
    lines.append("")
    lines.append("Service Level by ETA Variance x Weight Strategy:")
    lines.append("-" * 72)
    pivot = df.pivot_table(
        index="etaVariance", columns="weightStrategy",
        values="serviceLevel", aggfunc="mean",
    )
    pivot = pivot.reindex(index=[0.0, 0.1, 0.2, 0.3])
    pivot = pivot.reindex(columns=["balanced", "costHeavy", "timeHeavy"])
    lines.append(pivot.to_string(float_format="%.4f"))
    lines.append("")
    lines.append("=" * 72)
    text = "\n".join(lines)
    print(text)
    with open(out_file, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"\nSummary written to {out_file}")

def main() -> None:
    csv_path = sys.argv[1] if len(sys.argv) > 1 else CSV_FILE
    out_dir = sys.argv[2] if len(sys.argv) > 2 else FIGURES_DIR
    summary_path = sys.argv[3] if len(sys.argv) > 3 else SUMMARY_FILE
    ensure_dir(out_dir)
    print(f"Loading {csv_path} ...")
    df = load_data(csv_path)
    print("Generating Pareto frontier plot ...")
    plot_pareto_frontier(df, out_dir)
    print("Generating execution time boxplot ...")
    plot_execution_time_boxplot(df, out_dir)
    print("Generating service level heatmap ...")
    plot_service_level_heatmap(df, out_dir)
    print("Generating summary statistics ...")
    generate_summary(df, summary_path)
    print("\nDone. All outputs generated.")

if __name__ == "__main__":
    main()