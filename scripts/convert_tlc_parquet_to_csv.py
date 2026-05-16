#!/usr/bin/env python3
"""
Convert TLC Trip Record Parquet files to CSV for Java consumption.
Handles all TLC schemas: yellow, green, FHV, and FH-HV (high-volume).

Schema-normalized columns in output:
  pickup_datetime, dropoff_datetime, fare_amount, total_amount,
  trip_distance, vendor_id, PULocationID, DOLocationID

Run once before the Java benchmark, or whenever Parquet files are updated.
"""
import os, sys, glob
import pandas as pd
import numpy as np

DATA_DIR = "data/TLC Trip Record Data"
OUT_PATH = "data/TLC Trip Record Data/tlc_all_trips.csv"

def normalize(df, fname):
    """Normalize column names across TLC dataset variants."""
    base = os.path.basename(fname)

    # ── Pickup datetime ──
    for src, dst in [("tpep_pickup_datetime", "pickup_datetime"),
                     ("lpep_pickup_datetime", "pickup_datetime")]:
        if src in df.columns:
            df = df.rename(columns={src: dst})

    # ── Dropoff datetime ──
    for src, dst in [("tpep_dropoff_datetime", "dropoff_datetime"),
                     ("lpep_dropoff_datetime", "dropoff_datetime"),
                     ("dropOff_datetime", "dropoff_datetime"),
                     ("dropoffDatetime", "dropoff_datetime")]:
        if src in df.columns:
            df = df.rename(columns={src: dst})

    # ── Trip distance ──
    for src, dst in [("trip_distance", "trip_distance"),
                     ("trip_miles", "trip_distance")]:
        if src != "trip_distance" and src in df.columns:
            df = df.rename(columns={src: "trip_distance"})

    # ── Vendor ID ──
    for src in ["VendorID", "hvfhs_license_num", "dispatching_base_num"]:
        if src in df.columns and "vendor_id" not in df.columns:
            df["vendor_id"] = df[src].astype(str)
            break
    if "vendor_id" not in df.columns and "Affiliated_base_number" in df.columns:
        df["vendor_id"] = df["Affiliated_base_number"].astype(str)
    if "vendor_id" not in df.columns:
        df["vendor_id"] = "TLC-UNKNOWN"

    # ── Fare / total_amount ──
    if "base_passenger_fare" in df.columns and "fare_amount" not in df.columns:
        df["fare_amount"] = df["base_passenger_fare"]

    # Estimate total for FHV (no fare data) – typical zone-to-zone FHV rate
    if "total_amount" not in df.columns and "fare_amount" not in df.columns:
        # FHV: estimate $15-45 based on trip distance if available
        if "trip_distance" in df.columns:
            df["total_amount"] = 8 + df["trip_distance"].fillna(3) * 5.5
        else:
            df["total_amount"] = 25.0
    elif "total_amount" not in df.columns:
        df["total_amount"] = df["fare_amount"].fillna(15) * 1.5

    # ── Location IDs ──
    for src, dst in [("PUlocationID", "PULocationID"),
                     ("DOlocationID", "DOLocationID")]:
        if src in df.columns and dst not in df.columns:
            df[dst] = df[src]

    # Ensure canonical columns exist
    for col in ["pickup_datetime", "dropoff_datetime", "fare_amount",
                "total_amount", "trip_distance", "PULocationID", "DOLocationID"]:
        if col not in df.columns:
            df[col] = np.nan

    return df


def main():
    files = sorted(glob.glob(os.path.join(DATA_DIR, "*.parquet")))
    if not files:
        print("No Parquet files found in", DATA_DIR)
        sys.exit(1)

    # Skip if CSV is newer than all Parquet files
    csv_exists = os.path.exists(OUT_PATH)
    if csv_exists:
        csv_mtime = os.path.getmtime(OUT_PATH)
        all_older = all(os.path.getmtime(f) <= csv_mtime for f in files)
        if all_older:
            print(f"CSV up to date: {OUT_PATH}")
            return

    print(f"Converting {len(files)} Parquet files...")
    total = 0
    canonical_cols = ["vendor_id", "pickup_datetime", "dropoff_datetime",
                      "fare_amount", "total_amount", "trip_distance",
                      "PULocationID", "DOLocationID"]

    for i, f in enumerate(files):
        base = os.path.basename(f)
        print(f"  [{i+1}/{len(files)}] {base} ...", end=" ", flush=True)
        try:
            df = pd.read_parquet(f)
            df = normalize(df, f)

            # Keep only canonical + any extra useful columns
            extra = [c for c in df.columns if c not in canonical_cols]
            cols = [c for c in canonical_cols if c in df.columns] + extra
            sub = df[cols]

            mode = 'w' if total == 0 else 'a'
            header = (total == 0)
            sub.to_csv(OUT_PATH, mode=mode, index=False, header=header)
            rows = len(sub)
            total += rows
            print(f"{rows:,} rows")
        except Exception as e:
            print(f"ERROR: {e}")

    print(f"Done. {total:,} total rows written to {OUT_PATH}")


if __name__ == "__main__":
    main()