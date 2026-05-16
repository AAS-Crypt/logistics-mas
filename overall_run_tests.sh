#!/bin/bash

echo "=== Logistics MAS Tests run ==="
echo "Usage: ./overall_run_tests.sh | Runs all three tests/reports with output"
echo ""

java -cp "target/classes;lib/*" com.logistics.test.AlgorithmComparisonTest
java -cp "target/classes;lib/*" com.logistics.test.HardAlgorithmComparisonTest
java -cp "target/classes;lib/*" com.logistics.test.RealWorldAlgorithmTest