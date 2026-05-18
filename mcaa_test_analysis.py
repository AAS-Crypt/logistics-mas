#!/usr/bin/env python3
"""
Batched analysis script for MCAA benchmark comparison.
Scans mcaa_test_reports/ for CSV files grouped by dataset type,
supports filtering by --latest N and --since TIMESTAMP.

Usage:
  python mcaa_test_analysis.py                          # all files
  python mcaa_test_analysis.py --latest 100              # latest 100 runs per dataset
  python mcaa_test_analysis.py --since 20260515_000000   # runs from May 15 onward
  python mcaa_test_analysis.py --datasets olist,incom --latest 50
"""
import os
import glob
import sys
import argparse
from pathlib import Path
from datetime import datetime
from collections import defaultdict

import pandas as pd
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import seaborn as sns

REPORTS_DIR = "mcaa_test_reports"
FIGURES_DIR = os.path.join(REPORTS_DIR, "figures")
TS_FMT = "%Y%m%d_%H%M%S"

DATASET_PATTERNS = {
    "benchmark":   "_benchmark_results.csv",
    "olist":       "_olist_results.csv",
    "supplychain": "_supplychain_results.csv",
    "incom":       "_incom_results.csv",
    "tlc":         "_tlc_results.csv",
    "tlc_synth":   "_tlc_synth_results.csv",
}

DATASET_LABELS = {
    "benchmark":   "Synthetic Grid",
    "olist":       "Olist E-Commerce",
    "supplychain": "Supply Chain",
    "incom":       "INCOM2024",
    "tlc":         "TLC Historical",
    "tlc_synth":   "TLC Synth Deadlines",
}

COLOURS = {
    "MCAA": "#e74c3c",
    "Vickrey": "#3498db",
    "DoubleAuction": "#2ecc71",
    "LP": "#9b59b6",
}
MARKERS = {"MCAA": "o", "Vickrey": "s", "DoubleAuction": "D", "LP": "^"}
ALGO_ORDER = ["MCAA", "Vickrey", "DoubleAuction", "LP"]

REQUIRED_COLS = [
    "seed", "algorithm", "serviceLevel",
    "totalCost", "executionTimeMs", "giniCoefficient",
]

# ── File discovery ───────────────────────────────────────────────────────

def parse_timestamp(filename: str) -> str:
    """Extract YYYYMMDD_HHMMSS timestamp from a result filename."""
    base = os.path.basename(filename)
    # e.g. "20260510_171620_tlc_synth_results.csv" -> "20260510_171620"
    parts = base.split("_")
    if len(parts) >= 2 and parts[0].isdigit() and len(parts[0]) == 8:
        return f"{parts[0]}_{parts[1]}"
    return "00000000_000000"

def discover_files(reports_dir: str, datasets: set, latest: int = 0,
                   since: str = "") -> dict:
    """
    Find and group CSV files by dataset type.
    Returns dict: dataset_label -> list of (timestamp, filepath)
    """
    all_files = glob.glob(os.path.join(reports_dir, "*.csv"))
    groups = defaultdict(list)

    for fpath in sorted(all_files):
        fname = os.path.basename(fpath)
        ts = parse_timestamp(fpath)
        # Filter by --since
        if since and ts < since:
            continue
        for ds, suffix in DATASET_PATTERNS.items():
            if ds not in datasets:
                continue
            if fname.endswith(suffix):
                groups[ds].append((ts, fpath))
                break

    # Apply --latest N per dataset
    if latest > 0:
        for ds in groups:
            groups[ds].sort(key=lambda x: x[0], reverse=True)
            groups[ds] = groups[ds][:latest]
            groups[ds].sort(key=lambda x: x[0])

    return dict(groups)

# ── Loading ──────────────────────────────────────────────────────────────

def load_all(groups: dict) -> dict:
    """Load all CSVs, returning dict: dataset_label -> DataFrame (concatenated)."""
    data = {}
    for ds, files in groups.items():
        if not files:
            continue
        dfs = []
        for ts, fpath in files:
            try:
                df = pd.read_csv(fpath)
                dfs.append(df)
            except Exception as e:
                print(f"  WARNING: skipping {fpath}: {e}")
        if dfs:
            combined = pd.concat(dfs, ignore_index=True)
            data[ds] = combined
            print(f"  {DATASET_LABELS.get(ds, ds):>16s}: {len(files)} files, {len(combined):,} rows")
    return data

# ── Helpers ──────────────────────────────────────────────────────────────

def ensure_dir(path: str) -> None:
    Path(path).mkdir(parents=True, exist_ok=True)

def latest_ts(groups: dict) -> str:
    """Return the latest timestamp across all selected file groups."""
    best = "00000000_000000"
    for files in groups.values():
        for ts, _ in files:
            if ts > best:
                best = ts
    return best

# ── Figures ──────────────────────────────────────────────────────────────

def plot_aggregated_pareto(data: dict, out_dir: str, ts: str) -> None:
    """Pareto frontier aggregated across all runs, coloured by algorithm."""
    fig, ax = plt.subplots(figsize=(10, 7))
    # Concatenate all datasets, group by algorithm+seed
    all_dfs = []
    for ds, df in data.items():
        df = df.copy()
        df["_dataset"] = ds
        all_dfs.append(df)
    if not all_dfs:
        print("  No data for Pareto plot")
        plt.close(fig)
        return
    combined = pd.concat(all_dfs, ignore_index=True)

    agg = combined.groupby(["algorithm", "seed"]).agg(
        serviceLevel=("serviceLevel", "mean"),
        totalCost=("totalCost", "mean"),
    ).reset_index()

    all_points = []
    for algo in agg["algorithm"].unique():
        sub = agg[agg["algorithm"] == algo]
        ax.scatter(
            sub["serviceLevel"], sub["totalCost"],
            c=COLOURS.get(algo, "gray"),
            marker=MARKERS.get(algo, "o"),
            label=algo, alpha=0.5, edgecolors="k", linewidth=0.3, s=30,
        )
        all_points.append(sub[["serviceLevel", "totalCost"]].values)

    all_pts = np.vstack(all_points) if all_points else np.empty((0, 2))
    pareto_mask = np.ones(len(all_pts), dtype=bool)
    for i in range(len(all_pts)):
        svc_i, cost_i = all_pts[i, 0], all_pts[i, 1]
        for j in range(len(all_pts)):
            if i == j:
                continue
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
    ax.set_title("Pareto Frontier: Service Level vs Total Cost (all datasets)", fontsize=14)
    ax.legend(fontsize=10)
    ax.grid(True, alpha=0.3)
    ax.annotate("<- Better", xy=(0.02, 0.02), xycoords="axes fraction",
                fontsize=9, color="gray")
    fig.tight_layout()
    path = os.path.join(out_dir, f"{ts}_pareto_frontier.png")
    fig.savefig(path, dpi=200, bbox_inches="tight")
    plt.close(fig)
    print(f"  Saved {path}")

def plot_execution_time_boxplot(data: dict, out_dir: str, ts: str) -> None:
    """Boxplot of execution time per algorithm, faceted by dataset."""
    # Build combined DataFrame with dataset column
    rows = []
    for ds, df in data.items():
        d = df[["algorithm", "executionTimeMs"]].copy()
        d["Dataset"] = DATASET_LABELS.get(ds, ds)
        rows.append(d)
    if not rows:
        return
    combined = pd.concat(rows, ignore_index=True)
    combined = combined[combined["algorithm"].isin(ALGO_ORDER)]

    n_datasets = combined["Dataset"].nunique()
    fig, ax = plt.subplots(figsize=(max(8, n_datasets * 2.5), 6))

    sns.boxplot(
        data=combined, x="Dataset", y="executionTimeMs",
        hue="algorithm", hue_order=ALGO_ORDER,
        palette=COLOURS, ax=ax,
    )
    ax.set_yscale("log")
    ax.set_xlabel("Dataset", fontsize=12)
    ax.set_ylabel("Execution Time (ms, log scale)", fontsize=12)
    ax.set_title("Execution Time per Algorithm x Dataset", fontsize=14)
    ax.tick_params(axis="x", rotation=20)
    ax.legend(title="Algorithm", fontsize=9, title_fontsize=10)
    ax.grid(axis="y", alpha=0.3)
    fig.tight_layout()
    path = os.path.join(out_dir, f"{ts}_execution_time_boxplot.png")
    fig.savefig(path, dpi=200, bbox_inches="tight")
    plt.close(fig)
    print(f"  Saved {path}")

def plot_service_level_heatmap(data: dict, out_dir: str, ts: str) -> None:
    """Service level heatmap (only for benchmark grid data)."""
    if "benchmark" not in data or data["benchmark"].empty:
        print("  Skipping heatmap (no benchmark grid data)")
        return
    df = data["benchmark"].copy()
    if "etaVariance" not in df.columns or "weightStrategy" not in df.columns:
        print("  Skipping heatmap (missing etaVariance/weightStrategy columns)")
        return

    pivot = df.pivot_table(
        index="etaVariance", columns="weightStrategy",
        values="serviceLevel", aggfunc="mean",
    )
    pivot = pivot.reindex(index=[0.0, 0.1, 0.2, 0.3])
    for c in ["balanced", "costHeavy", "timeHeavy"]:
        if c not in pivot.columns:
            pivot[c] = np.nan
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
    path = os.path.join(out_dir, f"{ts}_service_level_heatmap.png")
    fig.savefig(path, dpi=200, bbox_inches="tight")
    plt.close(fig)
    print(f"  Saved {path}")

# ── Summary ──────────────────────────────────────────────────────────────

def generate_summary(data: dict, groups: dict, out_file: str) -> None:
    lines = []
    lines.append("=" * 78)
    lines.append("BENCHMARK SUMMARY - MCAA vs Vickrey vs Double Auction vs LP")
    lines.append("=" * 78)
    total_runs = sum(len(df) for df in data.values())
    total_files = sum(len(v) for v in groups.values())
    lines.append(f"Datasets: {len(data)} | Files loaded: {total_files} | Total rows: {total_runs:,}")
    lines.append("")

    for ds in ["benchmark", "olist", "supplychain", "incom", "tlc", "tlc_synth"]:
        if ds not in data or data[ds].empty:
            continue
        df = data[ds]
        n_files = len(groups.get(ds, []))
        lines.append("=" * 78)
        lines.append(f"  {DATASET_LABELS.get(ds, ds)}  [{n_files} run(s), {len(df):,} rows]")
        lines.append("=" * 78)
        lines.append("")
        lines.append("Per-Algorithm Averages (mean \u00b1 std):")
        lines.append("-" * 78)
        header = f"{'Algorithm':<16} {'ServiceLvl':>18} {'TotalCost':>18} {'Time(ms)':>15} {'Gini':>10}"
        lines.append(header)
        lines.append("-" * 78)
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
                f"{svc.mean():>8.4f}\u00b1{svc.std():<7.4f}"
                f"{cost.mean():>10.0f}\u00b1{cost.std():<7.0f}"
                f"{time_ms.mean():>9.1f}\u00b1{time_ms.std():<5.1f}"
                f"{gini.mean():>8.4f}"
            )
        lines.append("")
        means = df.groupby("algorithm")[["serviceLevel", "totalCost", "executionTimeMs"]].mean()
        best_svc = means["serviceLevel"].idxmax()
        best_cost = means["totalCost"].idxmin()
        best_time = means["executionTimeMs"].idxmin()
        lines.append("Best per Metric (by mean):")
        lines.append(f"  Highest Service Level: {best_svc} ({means.loc[best_svc, 'serviceLevel']:.4f})")
        lines.append(f"  Lowest Total Cost:      {best_cost} ({means.loc[best_cost, 'totalCost']:.0f})")
        lines.append(f"  Fastest Execution:      {best_time} ({means.loc[best_time, 'executionTimeMs']:.1f} ms)")
        lines.append("")

    # Benchmark heatmap table (only if we have benchmark data)
    if "benchmark" in data and not data["benchmark"].empty:
        df = data["benchmark"]
        if "etaVariance" in df.columns and "weightStrategy" in df.columns:
            lines.append("Service Level by ETA Variance x Weight Strategy (benchmark grid):")
            lines.append("-" * 78)
            pivot = df.pivot_table(
                index="etaVariance", columns="weightStrategy",
                values="serviceLevel", aggfunc="mean",
            )
            pivot = pivot.reindex(index=[0.0, 0.1, 0.2, 0.3])
            for c in ["balanced", "costHeavy", "timeHeavy"]:
                if c not in pivot.columns:
                    pivot[c] = np.nan
            pivot = pivot.reindex(columns=["balanced", "costHeavy", "timeHeavy"])
            lines.append(pivot.to_string(float_format="%.4f"))
            lines.append("")

    lines.append("=" * 78)
    text = "\n".join(lines)
    print(text)
    with open(out_file, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"\nSummary written to {out_file}")

# ── Main ─────────────────────────────────────────────────────────────────

def parse_args():
    p = argparse.ArgumentParser(
        description="Batched analysis for MCAA benchmark results")
    p.add_argument("--latest", type=int, default=0,
                   help="Use only the latest N runs per dataset type")
    p.add_argument("--since", type=str, default="",
                   help="Only include runs with timestamp >= YYYYMMDD_HHmmss")
    p.add_argument("--datasets", type=str, default="all",
                   help="Comma-separated datasets: bench,olist,sc,incom,tlc,tsynth (or 'all')")
    return p.parse_args()

def resolve_datasets(flag: str) -> set:
    if flag.lower() == "all":
        return set(DATASET_PATTERNS.keys())
    mapping = {
        "bench": "benchmark", "olist": "olist", "sc": "supplychain",
        "supplychain": "supplychain", "incom": "incom", "tlc": "tlc",
        "tsynth": "tlc_synth", "tlc_synth": "tlc_synth",
    }
    result = set()
    for part in flag.split(","):
        part = part.strip().lower()
        ds = mapping.get(part, part)
        if ds in DATASET_PATTERNS:
            result.add(ds)
    return result

def main():
    args = parse_args()
    datasets = resolve_datasets(args.datasets)
    if not datasets:
        print("ERROR: No valid datasets specified")
        sys.exit(1)

    print("=" * 60)
    print("MCAA Benchmark Analysis - Batched Mode")
    print(f"  Datasets: {sorted(datasets)}")
    print(f"  Latest: {args.latest if args.latest > 0 else 'ALL'}")
    print(f"  Since: {args.since or 'ANY'}")
    print("=" * 60)

    # Discover files
    print("\nDiscovering CSV files ...")
    groups = discover_files(REPORTS_DIR, datasets,
                            latest=args.latest, since=args.since)
    if not groups:
        print("ERROR: No matching CSV files found."
              f" Check {REPORTS_DIR}/ or adjust filters.")
        sys.exit(1)

    # Load data
    print("\nLoading data ...")
    data = load_all(groups)

    # Output naming
    ts = latest_ts(groups)
    ensure_dir(FIGURES_DIR)
    summary_path = os.path.join(REPORTS_DIR, f"{ts}_summary.txt")

    # Generate figures
    print("\nGenerating Pareto frontier plot ...")
    plot_aggregated_pareto(data, FIGURES_DIR, ts)
    print("Generating execution time boxplot ...")
    plot_execution_time_boxplot(data, FIGURES_DIR, ts)
    print("Generating service level heatmap ...")
    plot_service_level_heatmap(data, FIGURES_DIR, ts)

    # Summary
    print("Generating summary statistics ...")
    generate_summary(data, groups, summary_path)

    print("\nDone. All outputs generated.")

if __name__ == "__main__":
    main()