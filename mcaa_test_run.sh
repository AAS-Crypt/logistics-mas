#!/bin/bash

echo "=== Logistics MAS MCAA Benchmark run ==="
echo "Usage: ./mcaa_test_run.sh | Runs all benchmarks and reports with output"
echo ""
RUN_TS=$(date +"%Y%m%d_%H%M%S")
# Use SEED env var if set, otherwise randomize (0-32767)
SEED=${SEED:-$RANDOM}
echo "Seed: $SEED"
# max.orders=2000 caps ALL algorithms to the same subset for fair comparison.
# Set to 0 for unlimited (may cause OOM on LP at large datasets).
# Pre-step: Convert TLC Parquet to CSV (one-time, fast if already done)
echo "--- [0/6] Converting TLC Parquet files to CSV (if needed) ---"
python scripts/convert_tlc_parquet_to_csv.py || echo "WARNING: TLC conversion failed, synthetic fallback will be used"

# Compile
mvn clean compile
if [ $? -ne 0 ]; then
    echo "ERROR: Compilation failed"
    exit 1
fi

# Build full classpath including Maven dependencies (Windows-compatible)
echo "--- Resolving Maven classpath ---"
CP_FILE="mcaa_classpath.tmp"
mvn -q dependency:build-classpath -Dmdep.outputFile="$CP_FILE" 2>/dev/null
if [ $? -ne 0 ] || [ ! -f "$CP_FILE" ]; then
    echo "WARNING: mvn dependency:build-classpath failed, using manual jar discovery..."
    # Fallback: find Maven repository jars
    M2_REPO="${HOME}/.m2/repository"
    OJALGO_JAR=$(find "$M2_REPO/org/ojalgo" -name "ojalgo-*.jar" 2>/dev/null | head -1)
    PARQUET_JARS=$(find "$M2_REPO/org/apache/parquet" -name "*.jar" 2>/dev/null | tr '\n' ';')
    AVRO_JAR=$(find "$M2_REPO/org/apache/avro" -name "avro-1*.jar" 2>/dev/null | head -1)
    HADOOP_JARS=$(find "$M2_REPO/org/apache/hadoop" -name "hadoop-common-*.jar" 2>/dev/null | tr '\n' ';')
    SNAPPY_JAR=$(find "$M2_REPO/org/xerial/snappy" -name "snappy-java-*.jar" 2>/dev/null | head -1)
    JACKSON_JARS=$(find "$M2_REPO/com/fasterxml/jackson/core" -name "jackson-core-*.jar" 2>/dev/null | tr '\n' ';')
    M2_CP="${OJALGO_JAR};${PARQUET_JARS};${AVRO_JAR};${HADOOP_JARS};${SNAPPY_JAR};${JACKSON_JARS}"
else
    M2_CP=$(cat "$CP_FILE")
    rm -f "$CP_FILE"
fi

# Windows java.exe uses semicolons as classpath separator
LIB_CP="target/classes;lib/jade.jar;lib/commons-codec-1.3.jar"
FULL_CP="${LIB_CP};${M2_CP}"

echo "Classpath: $FULL_CP"
echo ""

# Synthetic benchmark grid
echo "--- [1/6] Running synthetic BenchmarkRunner ---"
java -cp "$FULL_CP" -Dbenchmark.iterations=300 -Dbenchmark.seed=$SEED com.logistics.benchmark.BenchmarkRunner

# Olist e-commerce dataset
echo ""
echo "--- [2/6] Running MCAA_OlistTest ---"
java -cp "$FULL_CP" -Dmax.orders=2000 -Dseed=$SEED com.logistics.test.MCAA_OlistTest

# Supply Chain Logistics dataset
echo ""
echo "--- [3/6] Running MCAA_SupplyChainTest ---"
java -Xmx6144m -cp "$FULL_CP" -Dmax.orders=2000 -Dseed=$SEED com.logistics.test.MCAA_SupplyChainTest

# INCOM2024 Logistics Delay dataset
echo ""
echo "--- [4/6] Running MCAA_IncomTest ---"
java -Xmx6144m -cp "$FULL_CP" -Dmax.orders=2000 -Dseed=$SEED com.logistics.test.MCAA_IncomTest

# TLC Trip Record Data (original dates — historical comparison)
echo ""
echo "--- [5/6] Running MCAA_TlcTest (historical deadlines) ---"
java -Xmx6144m -cp "$FULL_CP" -Dmax.orders=2000 -Dseed=$SEED com.logistics.test.MCAA_TlcTest

# TLC Trip Record Data (shifted synthetic deadlines — measurable service level)
echo ""
echo "--- [6/6] Running MCAA_TlcSynthTest (shifted deadlines) ---"
java -Xmx6144m -cp "$FULL_CP" -Dmax.orders=2000 -Dseed=$SEED com.logistics.test.MCAA_TlcSynthTest

# Generate analysis reports for all CSV files
echo ""
echo "--- Running MCAA analysis ---"
python mcaa_test_analysis.py --since "$RUN_TS"

echo ""
echo "=== All MCAA tests complete ==="