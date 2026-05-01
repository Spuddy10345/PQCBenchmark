package com.example.pqcbenchmark;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.StringWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// Generates detailed reports based on benchmark results
public class BenchmarkReportGenerator {
    private static final String TAG = "BenchmarkReport";

    private final Context context;
    private final List<BenchmarkResult> results;
    private final Map<String, String> deviceInfo;
    private final boolean isEmulated;

    public BenchmarkReportGenerator(Context context, boolean isEmulated) {
        this.context = context;
        this.results = new ArrayList<>();
        this.deviceInfo = DeviceInfoUtil.collectDeviceInfo(context);
        this.isEmulated = isEmulated;
    }

    // Add a benchmark result to the report
    // @param result The benchmark result to add
public void addResult(BenchmarkResult result) {
        results.add(result);
    }

    // Generate a detailed CSV report with all benchmark results and device information @return The URI of the generated report file
public Uri generateReport() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String filename = "pqc_benchmark_report_" + timestamp + ".csv";

        try {
            StringWriter stringWriter = new StringWriter();
            CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.DEFAULT);

            // Write report header with research title
            csvPrinter.printRecord("Post-Quantum Cryptography Performance Benchmarking on Android");
            csvPrinter.printRecord("Generated on", new Date());
            csvPrinter.printRecord("Is Emulated Device", isEmulated ? "Yes" : "No");
            csvPrinter.println();

            // Write device information
            writeDeviceInfo(csvPrinter);
            csvPrinter.println();

            // Write benchmark header
            csvPrinter.printRecord("Algorithm", "Category", "Operation", "Device",
                    "Execution Time (ms)", "Memory Usage (MB)", "CPU Utilization (%)",
                    "Est. Power Usage", "Security Level", "Quantum Resistant");

            // Write all benchmark results
            for (BenchmarkResult result : results) {
                for (OperationMetric metric : result.getMetrics()) {
                    csvPrinter.printRecord(
                            result.getAlgorithm(),
                            result.getCategory(),
                            metric.getOperation(),
                            result.getDeviceTier(),
                            String.format(Locale.US, "%.2f", metric.getExecutionTime()),
                            String.format(Locale.US, "%.2f", metric.getMemoryUsage()),
                            String.format(Locale.US, "%.2f", metric.getCpuUtilization()),
                            String.format(Locale.US, "%.2f", metric.getPowerUsage()),
                            result.getSecurityLevel(),
                            result.isQuantumResistant() ? "Yes" : "No"
                    );
                }
            }

            // Add summary statistics if multiple results
            if (results.size() > 1) {
                csvPrinter.println();
                csvPrinter.printRecord("Summary Statistics by Algorithm");
                generateSummaryStatistics(csvPrinter);

                // Add operation-type comparisons
                csvPrinter.println();
                csvPrinter.printRecord("Operation Type Comparisons");
                generateOperationTypeComparisons(csvPrinter);

                // Add PQC vs traditional comparison
                csvPrinter.println();
                csvPrinter.printRecord("PQC vs Traditional Cryptography Comparison");
                generatePQCvsTraditionalComparison(csvPrinter);

                // Add category comparisons
                csvPrinter.println();
                csvPrinter.printRecord("Category Comparisons");
                generateCategoryComparisons(csvPrinter);
            }

            csvPrinter.flush();
            csvPrinter.close();

            // Save to file
            return FileStorageUtil.saveFile(context, filename, stringWriter.toString());

        } catch (IOException e) {
            Log.e(TAG, "Error generating report", e);
            return null;
        }
    }

    // Write device info to report
private void writeDeviceInfo(CSVPrinter csvPrinter) throws IOException {
        // Group device info by category
        Map<String, List<Map.Entry<String, String>>> groupedInfo = new HashMap<>();
        groupedInfo.put("Basic", new ArrayList<>());
        groupedInfo.put("CPU", new ArrayList<>());
        groupedInfo.put("Memory", new ArrayList<>());
        groupedInfo.put("System", new ArrayList<>());

        for (Map.Entry<String, String> entry : deviceInfo.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("cpu") || key.contains("CPU")) {
                groupedInfo.get("CPU").add(entry);
            } else if (key.contains("Memory") || key.contains("memory") || key.contains("RAM")) {
                groupedInfo.get("Memory").add(entry);
            } else if (key.contains("android") || key.contains("version") || key.contains("API")) {
                groupedInfo.get("System").add(entry);
            } else {
                groupedInfo.get("Basic").add(entry);
            }
        }

        // Write device info by category
        csvPrinter.printRecord("Device Information");
        for (String category : Arrays.asList("Basic", "CPU", "Memory", "System")) {
            csvPrinter.printRecord(category + " Information");
            for (Map.Entry<String, String> entry : Objects.requireNonNull(groupedInfo.get(category))) {
                csvPrinter.printRecord("  " + entry.getKey(), entry.getValue());
            }
        }
    }

    // Generate summary stats comparing algorithms
private void generateSummaryStatistics(CSVPrinter csvPrinter) throws IOException {
        // Group by algorithm and operation
        Map<String, Map<String, List<OperationMetric>>> algorithmOperationMetrics = new HashMap<>();

        for (BenchmarkResult result : results) {
            String algorithm = result.getAlgorithm();

            if (!algorithmOperationMetrics.containsKey(algorithm)) {
                algorithmOperationMetrics.put(algorithm, new HashMap<>());
            }

            Map<String, List<OperationMetric>> operationMetrics = algorithmOperationMetrics.get(algorithm);

            for (OperationMetric metric : result.getMetrics()) {
                String operation = metric.getOperation();

                if (!operationMetrics.containsKey(operation)) {
                    operationMetrics.put(operation, new ArrayList<>());
                }

                operationMetrics.get(operation).add(metric);
            }
        }

        csvPrinter.printRecord("Algorithm", "Category", "Operation", "Average Time (ms)",
                "Min Time (ms)", "Max Time (ms)", "Avg Memory (MB)", "Avg CPU (%)", "Avg Power",
                "Security Level", "Quantum Resistant");

        for (BenchmarkResult result : results) {
            String algorithm = result.getAlgorithm();
            Map<String, List<OperationMetric>> operationMetrics = algorithmOperationMetrics.get(algorithm);

            for (Map.Entry<String, List<OperationMetric>> operationEntry : operationMetrics.entrySet()) {
                String operation = operationEntry.getKey();
                List<OperationMetric> metrics = operationEntry.getValue();

                double sumTime = 0, sumMemory = 0, sumCpu = 0, sumPower = 0;
                double minTime = Double.MAX_VALUE, maxTime = Double.MIN_VALUE;

                for (OperationMetric metric : metrics) {
                    double time = metric.getExecutionTime();
                    sumTime += time;
                    minTime = Math.min(minTime, time);
                    maxTime = Math.max(maxTime, time);

                    sumMemory += metric.getMemoryUsage();
                    sumCpu += metric.getCpuUtilization();
                    sumPower += metric.getPowerUsage();
                }

                double avgTime = metrics.isEmpty() ? 0 : sumTime / metrics.size();
                double avgMemory = metrics.isEmpty() ? 0 : sumMemory / metrics.size();
                double avgCpu = metrics.isEmpty() ? 0 : sumCpu / metrics.size();
                double avgPower = metrics.isEmpty() ? 0 : sumPower / metrics.size();

                // Handle if no metrics collected
                if (metrics.isEmpty()) {
                    minTime = 0;
                    maxTime = 0;
                }

                csvPrinter.printRecord(
                        algorithm,
                        result.getCategory(),
                        operation,
                        String.format(Locale.US, "%.2f", avgTime),
                        String.format(Locale.US, "%.2f", minTime),
                        String.format(Locale.US, "%.2f", maxTime),
                        String.format(Locale.US, "%.2f", avgMemory),
                        String.format(Locale.US, "%.2f", avgCpu),
                        String.format(Locale.US, "%.2f", avgPower),
                        result.getSecurityLevel(),
                        result.isQuantumResistant() ? "Yes" : "No"
                );
            }
        }
    }

    // Generate comparisons grouped by operation type (KeyGen, Encapsulate/Sign, etc.)
private void generateOperationTypeComparisons(CSVPrinter csvPrinter) throws IOException {
        // Group metrics by operation type
        Map<String, List<OperationMetric>> operationTypeMetrics = new HashMap<>();
        Map<String, String> operationToAlgorithm = new HashMap<>();
        Map<String, String> operationToCategory = new HashMap<>();
        Map<String, Boolean> operationToQuantumResistant = new HashMap<>();

        for (BenchmarkResult result : results) {
            for (OperationMetric metric : result.getMetrics()) {
                String operationKey = metric.getOperation() + " (" + result.getAlgorithm() + ")";

                if (!operationTypeMetrics.containsKey(operationKey)) {
                    operationTypeMetrics.put(operationKey, new ArrayList<>());
                    operationToAlgorithm.put(operationKey, result.getAlgorithm());
                    operationToCategory.put(operationKey, result.getCategory());
                    operationToQuantumResistant.put(operationKey, result.isQuantumResistant());
                }

                operationTypeMetrics.get(operationKey).add(metric);
            }
        }

        // Calculate averages
        Map<String, Double> avgTimes = new HashMap<>();
        Map<String, Double> avgMemory = new HashMap<>();
        Map<String, Double> avgCpu = new HashMap<>();
        Map<String, Double> avgPower = new HashMap<>();

        for (Map.Entry<String, List<OperationMetric>> entry : operationTypeMetrics.entrySet()) {
            String operationKey = entry.getKey();
            List<OperationMetric> metrics = entry.getValue();

            double sumTime = 0, sumMemory = 0, sumCpu = 0, sumPower = 0;

            for (OperationMetric metric : metrics) {
                sumTime += metric.getExecutionTime();
                sumMemory += metric.getMemoryUsage();
                sumCpu += metric.getCpuUtilization();
                sumPower += metric.getPowerUsage();
            }

            int count = metrics.size();
            avgTimes.put(operationKey, sumTime / count);
            avgMemory.put(operationKey, sumMemory / count);
            avgCpu.put(operationKey, sumCpu / count);
            avgPower.put(operationKey, sumPower / count);
        }

        // Group by operation type (KeyGen, Sign, etc.)
        Map<String, List<String>> operationGroups = new HashMap<>();

        for (String operationKey : operationTypeMetrics.keySet()) {
            String baseOperation = operationKey.split(" \\(")[0]; // Extract base operation name

            if (!operationGroups.containsKey(baseOperation)) {
                operationGroups.put(baseOperation, new ArrayList<>());
            }

            operationGroups.get(baseOperation).add(operationKey);
        }

        // Write grouped comparisons
        for (String baseOperation : operationGroups.keySet()) {
            csvPrinter.printRecord(baseOperation + " Comparison");
            csvPrinter.printRecord("Algorithm", "Category", "Quantum Resistant",
                    "Avg Time (ms)", "Avg Memory (MB)", "Avg CPU (%)", "Avg Power");

            List<String> operationKeys = operationGroups.get(baseOperation);

            // Sort by execution time
            assert operationKeys != null;
            operationKeys.sort(Comparator.comparingDouble(avgTimes::get));

            for (String operationKey : operationKeys) {
                csvPrinter.printRecord(
                        operationToAlgorithm.get(operationKey),
                        operationToCategory.get(operationKey),
                        operationToQuantumResistant.get(operationKey) ? "Yes" : "No",
                        String.format(Locale.UK, "%.2f", avgTimes.get(operationKey)),
                        String.format(Locale.UK, "%.2f", avgMemory.get(operationKey)),
                        String.format(Locale.UK, "%.2f", avgCpu.get(operationKey)),
                        String.format(Locale.UK, "%.2f", avgPower.get(operationKey))
                );
            }

            csvPrinter.println();
        }
    }

    // Generate comparison between PQC and traditional cryptography
private void generatePQCvsTraditionalComparison(CSVPrinter csvPrinter) throws IOException {
        // Group metrics by operation type and whether they are quantum resistant
        Map<String, Map<Boolean, List<Double>>> operationTimeByQuantum = new HashMap<>();
        Map<String, Map<Boolean, List<Double>>> operationMemoryByQuantum = new HashMap<>();
        Map<String, Map<Boolean, List<Double>>> operationCpuByQuantum = new HashMap<>();
        Map<String, Map<Boolean, List<Double>>> operationPowerByQuantum = new HashMap<>();

        // Initialize maps for all operation types
        initializeOperationMaps(operationTimeByQuantum);
        initializeOperationMaps(operationMemoryByQuantum);
        initializeOperationMaps(operationCpuByQuantum);
        initializeOperationMaps(operationPowerByQuantum);

        // Collect metrics
        for (BenchmarkResult result : results) {
            boolean isQuantumResistant = result.isQuantumResistant();

            for (OperationMetric metric : result.getMetrics()) {
                String operation = metric.getOperation();

                // Add metric to appropriate list
                Objects.requireNonNull(Objects.requireNonNull(operationTimeByQuantum.get(operation)).get(isQuantumResistant)).add(metric.getExecutionTime());
                Objects.requireNonNull(Objects.requireNonNull(operationMemoryByQuantum.get(operation)).get(isQuantumResistant)).add(metric.getMemoryUsage());
                Objects.requireNonNull(Objects.requireNonNull(operationCpuByQuantum.get(operation)).get(isQuantumResistant)).add(metric.getCpuUtilization());
                Objects.requireNonNull(Objects.requireNonNull(operationPowerByQuantum.get(operation)).get(isQuantumResistant)).add(metric.getPowerUsage());
            }
        }

        // Write comparison
        csvPrinter.printRecord("Operation", "Metric", "PQC Average", "Traditional Average", "Factor (PQC/Traditional)");

        // Process each operation type
        for (String operation : operationTimeByQuantum.keySet()) {
            // Only include operations that have both PQC and traditional data
            if (!hasDataForBothTypes(Objects.requireNonNull(operationTimeByQuantum.get(operation)))) {
                continue;
            }

            // Time comparison
            double pqAvgTime = average(Objects.requireNonNull(Objects.requireNonNull(operationTimeByQuantum.get(operation)).get(true)));
            double tradAvgTime = average(Objects.requireNonNull(Objects.requireNonNull(operationTimeByQuantum.get(operation)).get(false)));
            double timeFactor = pqAvgTime / tradAvgTime;

            csvPrinter.printRecord(
                    operation,
                    "Execution Time (ms)",
                    String.format(Locale.US, "%.2f", pqAvgTime),
                    String.format(Locale.US, "%.2f", tradAvgTime),
                    String.format(Locale.US, "%.2fx", timeFactor)
            );

            // Memory comparison
            double pqAvgMem = average(Objects.requireNonNull(Objects.requireNonNull(operationMemoryByQuantum.get(operation)).get(true)));
            double tradAvgMem = average(Objects.requireNonNull(Objects.requireNonNull(operationMemoryByQuantum.get(operation)).get(false)));
            double memFactor = pqAvgMem / tradAvgMem;

            csvPrinter.printRecord(
                    operation,
                    "Memory Usage (MB)",
                    String.format(Locale.US, "%.2f", pqAvgMem),
                    String.format(Locale.US, "%.2f", tradAvgMem),
                    String.format(Locale.US, "%.2fx", memFactor)
            );

            // CPU comparison
            double pqAvgCpu = average(Objects.requireNonNull(Objects.requireNonNull(operationCpuByQuantum.get(operation)).get(true)));
            double tradAvgCpu = average(Objects.requireNonNull(Objects.requireNonNull(operationCpuByQuantum.get(operation)).get(false)));
            double cpuFactor = pqAvgCpu / tradAvgCpu;

            csvPrinter.printRecord(
                    operation,
                    "CPU Utilization (%)",
                    String.format(Locale.US, "%.2f", pqAvgCpu),
                    String.format(Locale.US, "%.2f", tradAvgCpu),
                    String.format(Locale.US, "%.2fx", cpuFactor)
            );

            // Power comparison
            double pqAvgPower = average(Objects.requireNonNull(Objects.requireNonNull(operationPowerByQuantum.get(operation)).get(true)));
            double tradAvgPower = average(Objects.requireNonNull(Objects.requireNonNull(operationPowerByQuantum.get(operation)).get(false)));
            double powerFactor = pqAvgPower / tradAvgPower;

            csvPrinter.printRecord(
                    operation,
                    "Power Usage",
                    String.format(Locale.US, "%.2f", pqAvgPower),
                    String.format(Locale.US, "%.2f", tradAvgPower),
                    String.format(Locale.US, "%.2fx", powerFactor)
            );

            csvPrinter.println();
        }
    }

    // Generate comparisons by algorithm category
private void generateCategoryComparisons(CSVPrinter csvPrinter) throws IOException {
        // Group algorithms by category
        Map<String, List<BenchmarkResult>> resultsByCategory = new HashMap<>();

        for (BenchmarkResult result : results) {
            String category = result.getCategory();

            if (!resultsByCategory.containsKey(category)) {
                resultsByCategory.put(category, new ArrayList<>());
            }

            resultsByCategory.get(category).add(result);
        }

        // Process each category
        for (String category : resultsByCategory.keySet()) {
            List<BenchmarkResult> categoryResults = resultsByCategory.get(category);

            csvPrinter.printRecord(category + " Comparison");
            csvPrinter.printRecord("Algorithm", "Operation", "Avg Time (ms)", "Avg Memory (MB)",
                    "Avg CPU (%)", "Avg Power", "Quantum Resistant");

            // Process each algorithm in this category
            for (BenchmarkResult result : categoryResults) {
                // Group metrics by operation
                Map<String, List<OperationMetric>> metricsByOperation = new HashMap<>();

                for (OperationMetric metric : result.getMetrics()) {
                    String operation = metric.getOperation();

                    if (!metricsByOperation.containsKey(operation)) {
                        metricsByOperation.put(operation, new ArrayList<>());
                    }

                    Objects.requireNonNull(metricsByOperation.get(operation)).add(metric);
                }

                // Calculate averages for each operation
                for (String operation : metricsByOperation.keySet()) {
                    List<OperationMetric> metrics = metricsByOperation.get(operation);

                    assert metrics != null;
                    double avgTime = metrics.stream()
                            .mapToDouble(OperationMetric::getExecutionTime)
                            .average()
                            .orElse(0);

                    double avgMemory = metrics.stream()
                            .mapToDouble(OperationMetric::getMemoryUsage)
                            .average()
                            .orElse(0);

                    double avgCpu = metrics.stream()
                            .mapToDouble(OperationMetric::getCpuUtilization)
                            .average()
                            .orElse(0);

                    double avgPower = metrics.stream()
                            .mapToDouble(OperationMetric::getPowerUsage)
                            .average()
                            .orElse(0);

                    csvPrinter.printRecord(
                            result.getAlgorithm(),
                            operation,
                            String.format(Locale.US, "%.2f", avgTime),
                            String.format(Locale.US, "%.2f", avgMemory),
                            String.format(Locale.US, "%.2f", avgCpu),
                            String.format(Locale.US, "%.2f", avgPower),
                            result.isQuantumResistant() ? "Yes" : "No"
                    );
                }
            }

            csvPrinter.println();
        }
    }

    // Initialize operation maps for PQC vs traditional comparison
private void initializeOperationMaps(Map<String, Map<Boolean, List<Double>>> map) {
        for (String operation : Arrays.asList("KeyGen", "Encapsulate", "Decapsulate", "Sign", "Verify")) {
            map.put(operation, new HashMap<>());
            map.get(operation).put(true, new ArrayList<>());  // PQC
            map.get(operation).put(false, new ArrayList<>()); // Traditional
        }
    }

    // Check if there is data for both PQC and traditional algorithms
private boolean hasDataForBothTypes(Map<Boolean, List<Double>> data) {
        return !Objects.requireNonNull(data.get(true)).isEmpty() && !Objects.requireNonNull(data.get(false)).isEmpty();
    }

    // Calculate average of a list of doubles
private double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }

        double sum = 0;
        for (Double value : values) {
            sum += value;
        }

        return sum / values.size();
    }
}