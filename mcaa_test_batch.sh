#!/bin/bash
# Batch runner: executes mcaa_test_run.sh N times with randomized seeds.
# Usage:
#   bash ./mcaa_test_batch.sh          # 1 run (default)
#   bash ./mcaa_test_batch.sh 10       # 10 runs

N=${1:-1}
echo "=== MCAA Batch Runner ==="
echo "Total runs: $N"
echo "========================="
echo ""

BATCH_TS=$(date +"%Y%m%d_%H%M%S")

for i in $(seq 1 $N); do
    echo "======================================"
    echo "=== BATCH RUN $i/$N ==="
    echo "======================================"
    SEED=$RANDOM bash ./mcaa_test_run.sh
    if [ $? -ne 0 ]; then
        echo "ERROR: Run $i failed, continuing..."
    fi
    echo ""
done

echo "=== Batch complete: $N runs finished ==="
echo ""
echo "Generating batch summary..."
python mcaa_test_analysis.py --since "$BATCH_TS"
echo ""
echo "All batch outputs in mcaa_test_reports/"