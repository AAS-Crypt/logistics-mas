#!/bin/bash

echo "========================================="
echo "MAS Research Testing Framework"
echo "========================================="

MVN_CMD="mvn"
TEST_DIR="./overall_test_reports/research_data"

run_java() {
    java -cp "target/classes;lib/*" "$@"
}
mkdir -p "$TEST_DIR"

function run_basic_tests() {
    echo "Running Basic Unit Tests (T-01 to T-07)..."
    echo "-----------------------------------------"
    
    echo "1. T-01: MCAA Scoring Test"
    $MVN_CMD test -Dtest=MCAATest 2>&1 | tee -a "$TEST_DIR/test_t01_mcaa.log"
    
    echo "2. T-02: Contract Net Protocol Test"
    $MVN_CMD test -Dtest=ContractNetTest 2>&1 | tee -a "$TEST_DIR/test_t02_contractnet.log"
    
    echo "3. T-03: Event Subscription Test"
    echo "   Note: Run manually with JADE GUI for visualization"
    echo "   Command: java -cp \"target/classes;lib/*\" jade.Boot -gui -agents \"monitor:com.logistics.agents.MonitorAgent;res1:com.logistics.agents.ResourceAgent\""
    
    echo "4. T-04: CAGA Nash Bargaining Test"
    $MVN_CMD test -Dtest=CAGATest 2>&1 | tee -a "$TEST_DIR/test_t04_caga.log"
    
    echo "5. T-05: AERA Impact Calculation Test"
    $MVN_CMD test -Dtest=AERATest 2>&1 | tee -a "$TEST_DIR/test_t05_aera.log"
    
    echo "6. T-06: PCRA Cluster Optimization Test"
    run_java com.logistics.simulator.PCRASimulator 2>&1 | tee -a "$TEST_DIR/test_t06_pcra.log"
    
    echo "7. T-07: SPTA Policy Update Test"
    $MVN_CMD test -Dtest=SPTATest 2>&1 | tee -a "$TEST_DIR/test_t07_spta.log"
}

function run_extended_scenarios() {
    echo ""
    echo "Running Extended Test Scenarios..."
    echo "----------------------------------"
    
    echo "1. High Priority Order Test"
    run_java com.logistics.simulator.ScenarioRunner --scenario high_priority --orders 10 --urgency 0.8 2>&1 | tee -a "$TEST_DIR/scenario_high_priority.log"
    
    echo "2. Low Budget Order Test"
    run_java com.logistics.simulator.ScenarioRunner --scenario low_budget --budget_multiplier 0.5 2>&1 | tee -a "$TEST_DIR/scenario_low_budget.log"
    
    echo "3. Multiple Resources Test"
    run_java com.logistics.simulator.ScenarioRunner --scenario multiple_resources --resources 10 --orders 50 2>&1 | tee -a "$TEST_DIR/scenario_multiple_resources.log"
    
    echo "4. Edge Case - Extreme Urgency"
    run_java com.logistics.simulator.ScenarioRunner --scenario extreme_urgency --deadline_multiplier 0.1 2>&1 | tee -a "$TEST_DIR/scenario_extreme_urgency.log"
}

function run_uncertainty_analysis() {
    echo ""
    echo "Running Uncertainty Analysis..."
    echo "-------------------------------"
    
    echo "1. Monte Carlo Simulation (100 iterations)"
    run_java com.logistics.simulator.MonteCarloSimulator --iterations 100 --uncertainty_level medium 2>&1 | tee -a "$TEST_DIR/monte_carlo_medium.log"
    
    echo "2. Sensitivity Analysis for MCAA weights"
    run_java com.logistics.simulator.SensitivityAnalyzer --parameter "mcaa_time_weight" --range "0.1:0.9:0.1" --scenarios 5 2>&1 | tee -a "$TEST_DIR/sensitivity_time_weight.log"
    
    echo "3. Algorithm Comparison Under Uncertainty"
    run_java com.logistics.benchmark.AlgorithmComparator --algorithms "MCAA,GA,PSO,Random" --scenarios "uncertainty" --iterations 20 2>&1 | tee -a "$TEST_DIR/algorithm_comparison.log"
}

function generate_report() {
    echo ""
    echo "Generating Research Report..."
    echo "-----------------------------"
    
    if [ -f "overall_analyze_results.py" ]; then
        python overall_analyze_results.py --input_dir "$TEST_DIR" --output "$TEST_DIR/research_report.md"
        echo "Report generated: $TEST_DIR/research_report.md"
    else
        echo "Analysis script not found. Creating basic summary..."
        
        echo "# Research Test Summary" > "$TEST_DIR/summary.md"
        echo "Generated: $(date)" >> "$TEST_DIR/summary.md"
        echo "" >> "$TEST_DIR/summary.md"
        echo "## Test Files Generated:" >> "$TEST_DIR/summary.md"
        ls -la "$TEST_DIR/" | grep ".log" | awk '{print "- " $9 " (" $5 " bytes)"}' >> "$TEST_DIR/summary.md"
        
        echo "Basic summary created: $TEST_DIR/summary.md"
    fi
}

function show_help() {
    echo "Usage: $0 [option]"
    echo ""
    echo "Options:"
    echo "  all        Run all tests (basic + extended + uncertainty)"
    echo "  basic      Run only basic unit tests (T-01 to T-07)"
    echo "  extended   Run only extended scenarios"
    echo "  uncertainty Run only uncertainty analysis"
    echo "  report     Generate research report from collected data"
    echo "  help       Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 all      # Run complete test suite"
    echo "  $0 basic    # Run only basic validation tests"
}

case "$1" in
    "all")
        run_basic_tests
        run_extended_scenarios
        run_uncertainty_analysis
        generate_report
        ;;
    "basic")
        run_basic_tests
        ;;
    "extended")
        run_extended_scenarios
        ;;
    "uncertainty")
        run_uncertainty_analysis
        ;;
    "report")
        generate_report
        ;;
    "help"|"-h"|"--help")
        show_help
        ;;
    *)
        echo "Please specify a command. Use '$0 help' for options."
        show_help
        ;;
esac

echo ""
echo "========================================="
echo "Testing completed. Results in: $TEST_DIR"
echo "========================================="