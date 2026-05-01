package com.example.pqcbenchmark;

import java.util.List;

// bench results
public class BenchmarkResult {
    private final String algorithm;
    private final String category;
    private final String deviceTier;
    private final List<OperationMetric> metrics;
    private final BatteryMonitor.BatteryUsageStats batteryStats; // Kept for compatibility but not used
    private final boolean isEmulated;
    private final boolean isQuantumResistant;
    private final String securityLevel;

    public BenchmarkResult(String algorithm, String category, String deviceTier,
                           List<OperationMetric> metrics, BatteryMonitor.BatteryUsageStats batteryStats,
                           boolean isEmulated, boolean isQuantumResistant, String securityLevel) {
        this.algorithm = algorithm;
        this.category = category;
        this.deviceTier = deviceTier;
        this.metrics = metrics;
        this.batteryStats = batteryStats;
        this.isEmulated = isEmulated;
        this.isQuantumResistant = isQuantumResistant;
        this.securityLevel = securityLevel;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getCategory() {
        return category;
    }

    public String getDeviceTier() {
        return deviceTier;
    }

    public List<OperationMetric> getMetrics() {
        return metrics;
    }

    public BatteryMonitor.BatteryUsageStats getBatteryStats() {
        return batteryStats;
    }

    public boolean isEmulated() {
        return isEmulated;
    }

    public boolean isQuantumResistant() {
        return isQuantumResistant;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }
}

// Model class for operation metrics
class OperationMetric {
    private final String operation;
    private final double executionTime;  // milliseconds
    private final double memoryUsage;    // MB
    private final double cpuUtilization; // percentage
    private final double powerUsage;     // estimated (0-100 scale)

    public OperationMetric(String operation, double executionTime, double memoryUsage,
                           double cpuUtilization, double powerUsage) {
        this.operation = operation;
        this.executionTime = executionTime;
        this.memoryUsage = memoryUsage;
        this.cpuUtilization = cpuUtilization;
        this.powerUsage = powerUsage;
    }

    public String getOperation() {
        return operation;
    }

    public double getExecutionTime() {
        return executionTime;
    }

    public double getMemoryUsage() {
        return memoryUsage;
    }

    public double getCpuUtilization() {
        return cpuUtilization;
    }

    public double getPowerUsage() {
        return powerUsage;
    }
}