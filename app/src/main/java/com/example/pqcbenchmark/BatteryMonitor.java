package com.example.pqcbenchmark;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

// the class to attempt to monitor battery usage, but too innacurate for any meaningful results
public class BatteryMonitor {
    private static final String TAG = "BatteryMonitor";

    private Context context;
    private int initialBatteryLevel;
    private float initialTemperature;
    private long startTime;

    // init battery monitor @param context Application context
public BatteryMonitor(Context context) {
        this.context = context;
        this.startTime = System.currentTimeMillis();

        // Get initial battery stats
        BatteryStats initialStats = getBatteryStats();
        this.initialBatteryLevel = initialStats.batteryLevel;
        this.initialTemperature = initialStats.temperature;
    }

    // monitor usage
public void start() {
        BatteryStats stats = getBatteryStats();
        initialBatteryLevel = stats.batteryLevel;
        initialTemperature = stats.temperature;
        startTime = System.currentTimeMillis();

        Log.d(TAG, String.format("Battery monitoring started. Initial level: %d%%, temp: %.1f°C",
                initialBatteryLevel, initialTemperature / 10.0f));
    }

    // stop monitor and get stats @return BatteryUsageStats object containing the battery usage information
public BatteryUsageStats stop() {
        BatteryStats currentStats = getBatteryStats();
        long endTime = System.currentTimeMillis();

        // Calculate differences
        int levelDrop = initialBatteryLevel - currentStats.batteryLevel;
        float tempChange = currentStats.temperature - initialTemperature;
        long duration = endTime - startTime;

        // Calculate rate of battery drain per hour
        float hourFraction = duration / (1000.0f * 60.0f * 60.0f); // hours
        float drainRatePerHour = hourFraction > 0 ? levelDrop / hourFraction : 0;

        BatteryUsageStats usage = new BatteryUsageStats(
                initialBatteryLevel,
                currentStats.batteryLevel,
                levelDrop,
                drainRatePerHour,
                initialTemperature / 10.0f, // convert to Celsius
                currentStats.temperature / 10.0f,
                tempChange / 10.0f,
                duration
        );

        Log.d(TAG, String.format("Battery monitoring stopped. Final level: %d%%, drop: %d%%, temp: %.1f°C",
                currentStats.batteryLevel, levelDrop, currentStats.temperature / 10.0f));

        return usage;
    }

    // get current stats @return BatteryStats object with current battery information
private BatteryStats getBatteryStats() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);

        int level = 0;
        int scale = 1;
        float temperature = 0;
        boolean isCharging = false;

        if (batteryStatus != null) {
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
        }

        //normalise level
        int batteryPct = Math.round(level * 100 / (float) scale);

        return new BatteryStats(batteryPct, temperature, isCharging);
    }

    // get approx consumtion - NOT ACCURATE!! @param algorithmName Name of the algorithm being tested @param operationName Name of the operation (e.g., KeyGen, Encryption) @param executionTime Execution time in milliseconds @return Estimated power consumption in arbitrary units (0-100)
public static float getEstimatedPowerConsumption(String algorithmName, String operationName, double executionTime) {
        // Basic power estimation heuristics based on algorithm type and operation
        float baseFactor = 1.0f;

        // Adjust based on algorithm (PQC algorithms generally use more power than traditional)
        if (algorithmName.contains("Kyber")) {
            baseFactor = 2.5f;
        } else if (algorithmName.contains("SPHINCS")) {
            baseFactor = 3.0f; // Hash-based signatures are typically more intensive
        } else if (algorithmName.contains("Falcon")) {
            baseFactor = 2.8f;
        } else if (algorithmName.contains("McEliece")) {
            baseFactor = 3.2f; // Code-based algorithms can be more intensive
        } else if (algorithmName.contains("RSA")) {
            baseFactor = 1.8f;
        } else if (algorithmName.contains("ECC")) {
            baseFactor = 1.5f;
        }

        // Adjust based on operation
    float powerEstimate = getPowerEstimate(operationName, (float) executionTime, baseFactor);
    return Math.min(powerEstimate, 100.0f);
    }

    private static float getPowerEstimate(String operationName, float executionTime, float baseFactor) {
        float operationFactor = 1.0f;
        if (operationName.contains("KeyGen")) {
            operationFactor = 1.5f; // Key generation is typically more intensive
        } else if (operationName.contains("Sign")) {
            operationFactor = 1.3f;
        } else if (operationName.contains("Verify")) {
            operationFactor = 0.8f; // Verification is typically less intensive
        } else if (operationName.contains("Encrypt")) {
            operationFactor = 1.2f;
        } else if (operationName.contains("Decrypt")) {
            operationFactor = 1.1f;
        }

        // Normalize by execution time (longer = more power)
        // We cap this at 100 for a reference scale
        float powerEstimate = baseFactor * operationFactor * executionTime / 100.0f;
        return powerEstimate;
    }

    // Class to store battery stats at a point in time
private static class BatteryStats {
        final int batteryLevel;    // percentage
        final float temperature;   // raw value, divide by 10 for Celsius
        final boolean isCharging;

        BatteryStats(int batteryLevel, float temperature, boolean isCharging) {
            this.batteryLevel = batteryLevel;
            this.temperature = temperature;
            this.isCharging = isCharging;
        }
    }

    // Class to hold battery usage statistics
public static class BatteryUsageStats {
        final int initialLevel;       // percentage
        final int finalLevel;         // percentage
        final int levelDrop;          // percentage points
        final float drainRatePerHour; // percentage points per hour
        final float initialTemp;      // in Celsius
        final float finalTemp;        // in Celsius
        final float tempChange;       // in Celsius
        final long duration;          // milliseconds

        BatteryUsageStats(int initialLevel, int finalLevel, int levelDrop, float drainRatePerHour,
                          float initialTemp, float finalTemp, float tempChange, long duration) {
            this.initialLevel = initialLevel;
            this.finalLevel = finalLevel;
            this.levelDrop = levelDrop;
            this.drainRatePerHour = drainRatePerHour;
            this.initialTemp = initialTemp;
            this.finalTemp = finalTemp;
            this.tempChange = tempChange;
            this.duration = duration;
        }

        @NonNull
        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            return String.format(
                    "Battery: %d%% -> %d%% (-%d%%), Rate: %.1f%%/hour\n" +
                            "Temperature: %.1f°C -> %.1f°C (change: %+.1f°C)\n" +
                            "Duration: %.1f seconds",
                    initialLevel, finalLevel, levelDrop, drainRatePerHour,
                    initialTemp, finalTemp, tempChange,
                    duration / 1000.0f
            );
        }
    }
}