package com.example.pqcbenchmark;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Runs benchmark tasks for cryptographic algorithms in the background
class BenchmarkTask {
    private static final String TAG = "BenchmarkTask";

    private final MainActivity activity;
    private final CryptoAlgorithm algorithm;
    private final String deviceModel;
    private final Set<String> operations;
    private final boolean isEmulated;

    // Number of iterations for more accurate benchmarking
    private static final int ITERATIONS = 5;

    public BenchmarkTask(MainActivity activity, CryptoAlgorithm algorithm, String deviceModel,
                         Set<String> operations, boolean isEmulated) {
        this.activity = activity;
        this.algorithm = algorithm;
        this.deviceModel = deviceModel;
        this.operations = operations;
        this.isEmulated = isEmulated;
    }

    // Execute the benchmark @return Results of the benchmark
public BenchmarkResult execute() {
        updateProgress("Starting benchmark for " + algorithm.getAlgorithmName());

        List<OperationMetric> metrics = new ArrayList<>();

        // Always run key generation first if requested
        if (operations.contains("KeyGen")) {
            metrics.add(benchmarkOperation("KeyGen", () -> algorithm.generateKeys()));
        } else if (needsKeyGeneration()) {
            // Generate keys without measuring if needed for other operations
            updateProgress("Generating keys (not measured)...");
            algorithm.generateKeys();
        }

        // Run KEM/encryption operations if applicable
        if (algorithm instanceof KEMAlgorithm) {
            KEMAlgorithm kemAlgo = (KEMAlgorithm) algorithm;

            if (operations.contains("Encapsulate")) {
                metrics.add(benchmarkOperation("Encapsulate", kemAlgo::encapsulate));
            }

            if (operations.contains("Decapsulate")) {
                // Ensure encapsulation is done first
                if (!operations.contains("Encapsulate")) {
                    updateProgress("Running encapsulation (not measured)...");
                    kemAlgo.encapsulate();
                }
                metrics.add(benchmarkOperation("Decapsulate", kemAlgo::decapsulate));
            }
        }

        // Run signature operations if applicable
        if (algorithm instanceof SignatureAlgorithm) {
            SignatureAlgorithm sigAlgo = (SignatureAlgorithm) algorithm;

            if (operations.contains("Sign")) {
                metrics.add(benchmarkOperation("Sign", sigAlgo::sign));
            }

            if (operations.contains("Verify")) {
                // Ensure signing is done first
                if (!operations.contains("Sign")) {
                    updateProgress("Running signing (not measured)...");
                    sigAlgo.sign();
                }
                metrics.add(benchmarkOperation("Verify", sigAlgo::verify));
            }
        }

        updateProgress("Benchmark completed for " + algorithm.getAlgorithmName());

        return new BenchmarkResult(
                algorithm.getAlgorithmName(),
                algorithm.getCategory(),
                deviceModel,
                metrics,
                null, // No battery stats
                isEmulated,
                algorithm.isQuantumResistant(),
                algorithm.getSecurityLevel()
        );
    }

    // Check if key generation is needed for other operations
private boolean needsKeyGeneration() {
        if (algorithm instanceof KEMAlgorithm &&
                (operations.contains("Encapsulate") || operations.contains("Decapsulate"))) {
            return true;
        }

        if (algorithm instanceof SignatureAlgorithm &&
                (operations.contains("Sign") || operations.contains("Verify"))) {
            return true;
        }

        return false;
    }

    // Benchmark a single operation and collect performance metrics
private OperationMetric benchmarkOperation(String operationName, Runnable operation) {
        updateProgress("Running " + operationName + "...");

        // Run garbage collection before benchmark for more accurate memory measurement
        System.gc();
        try {
            Thread.sleep(200); // Wait for garbage collection
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted during GC wait", e);
        }

        // Get initial metrics
        long startMemory = getMemoryUsage();
        long startCpuTime = Debug.threadCpuTimeNanos();
        long startTime = System.currentTimeMillis();

        // Run the operation multiple times for better accuracy
        for (int i = 0; i < ITERATIONS; i++) {
            operation.run();
        }

        // Get final metrics
        long endTime = System.currentTimeMillis();
        long endCpuTime = Debug.threadCpuTimeNanos();

        // Run garbage collection again to stabilize
        System.gc();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted during GC wait", e);
        }

        long endMemory = getMemoryUsage();

        // Calculate metrics
        double executionTime = (endTime - startTime) / (double) ITERATIONS;
        double cpuUtilization = ((endCpuTime - startCpuTime) / 1_000_000.0) / (endTime - startTime) * 100;
        double memoryUsage = Math.max(0, (endMemory - startMemory) / 1024.0 / 1024.0); // Convert to MB

        // Calculate estimated power usage based on algorithm complexity
        // This is a very rough estimate and should be used only for relative comparisons
        double powerUsage = estimatePowerUsage(algorithm, operationName, executionTime);

        Log.d(TAG, String.format("%s - Time: %.2fms, CPU: %.2f%%, Memory: %.2fMB, Est. Power: %.2f",
                operationName, executionTime, cpuUtilization, memoryUsage, powerUsage));

        return new OperationMetric(operationName, executionTime, memoryUsage, cpuUtilization, powerUsage);
    }

    // Get current memory usage in bytes
private long getMemoryUsage() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);

        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.totalMem - memoryInfo.availMem;
        }

        return 0;
    }

    // Estimate power usage based on algorithm type and operation This provides a relative comparison only
private double estimatePowerUsage(CryptoAlgorithm algorithm, String operationName, double executionTime) {
        // Basic power estimation heuristics based on algorithm type and operation
        double baseFactor = 1.0;

        // Adjust based on algorithm type
        if (algorithm.getCategory().contains("Lattice")) {
            baseFactor = 2.5; // Lattice-based algorithms (Kyber, Dilithium)
        } else if (algorithm.getCategory().contains("Hash")) {
            baseFactor = 3.0; // Hash-based signatures (SPHINCS+)
        } else if (algorithm.getCategory().contains("Code")) {
            baseFactor = 3.2; // Code-based algorithms
        } else if (algorithm.getCategory().contains("Multivariate")) {
            baseFactor = 2.8; // Multivariate signature algorithms
        } else if (algorithm.getCategory().contains("Integer")) {
            baseFactor = 1.8; // RSA
        } else if (algorithm.getCategory().contains("Elliptic")) {
            baseFactor = 1.5; // ECC
        }

        // Adjust based on operation
        double operationFactor = 1.0;
        if (operationName.equals("KeyGen")) {
            operationFactor = 1.5; // Key generation is typically more intensive
        } else if (operationName.equals("Sign")) {
            operationFactor = 1.3;
        } else if (operationName.equals("Verify")) {
            operationFactor = 0.8; // Verification is typically less intensive
        } else if (operationName.equals("Encapsulate")) {
            operationFactor = 1.2;
        } else if (operationName.equals("Decapsulate")) {
            operationFactor = 1.1;
        }

        // Normalize by execution time (longer = more power)
        // Cap at 100 for a reference scale
        double powerEstimate = baseFactor * operationFactor * executionTime / 100.0;
        return Math.min(powerEstimate, 100.0);
    }

    // Update the UI with progress information
private void updateProgress(String message) {
        activity.runOnUiThread(() -> {
            TextView statusText = activity.findViewById(R.id.status_text);
            statusText.setText(message);
        });
    }
}