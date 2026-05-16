#!/bin/bash

echo "=== Logistics MAS Simulation ==="
echo "Usage: ./overall_run_simulation.sh [num_orders] [num_resources] [duration_minutes]"
echo ""

NUM_ORDERS=${1:-10}
NUM_RESOURCES=${2:-3}
DURATION=${3:-5}

echo "Starting simulation with:"
echo "  Orders: $NUM_ORDERS"
echo "  Resources: $NUM_RESOURCES"
echo "  Duration: $DURATION minutes"
echo ""

java -cp "target/classes;lib/*" com.logistics.simulator.LogisticsSimulator $NUM_ORDERS $NUM_RESOURCES $DURATION

echo ""
echo "Simulation completed."