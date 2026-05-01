package com.example.pqcbenchmark;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

 //gathers and organises BM info
public class DeviceInfoUtil {
    private static final String TAG = "DeviceInfoUtil";

     //collects comprehensive device information
    public static Map<String, String> collectDeviceInfo(Context context) {
        Map<String, String> deviceInfo = new HashMap<>();

        //get device info
        deviceInfo.put("model", Build.MODEL);
        deviceInfo.put("manufacturer", Build.MANUFACTURER);
        deviceInfo.put("device", Build.DEVICE);
        deviceInfo.put("androidVersion", Build.VERSION.RELEASE);
        deviceInfo.put("apiLevel", String.valueOf(Build.VERSION.SDK_INT));
        deviceInfo.put("buildId", Build.ID);

        deviceInfo.put("cpuABI", Build.SUPPORTED_ABIS[0]);
        deviceInfo.put("cpuCores", String.valueOf(Runtime.getRuntime().availableProcessors()));

        try {
            //get CPU info from /proc/cpuinfo
            deviceInfo.putAll(getCpuInfoFromProc());
        } catch (Exception e) {
            Log.e(TAG, "Error reading CPU info", e);
            deviceInfo.put("cpuInfo", "Error reading CPU info: " + e.getMessage());
        }

        //mem info
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        deviceInfo.put("totalMemory", formatSize(memoryInfo.totalMem));
        deviceInfo.put("availableMemory", formatSize(memoryInfo.availMem));
        deviceInfo.put("lowMemoryThreshold", formatSize(memoryInfo.threshold));
        deviceInfo.put("isLowMemory", String.valueOf(memoryInfo.lowMemory));

        //storage info
        File externalStorageDir = Environment.getExternalStorageDirectory();
        StatFs statFs = new StatFs(externalStorageDir.getPath());

        long blockSize = statFs.getBlockSizeLong();
        long totalSize = statFs.getBlockCountLong() * blockSize;
        long availableSize = statFs.getAvailableBlocksLong() * blockSize;

        deviceInfo.put("totalStorage", formatSize(totalSize));
        deviceInfo.put("availableStorage", formatSize(availableSize));

        return deviceInfo;
    }

    //get detailed CPU information from /proc/cpuinfo, return map of CPU information
    private static Map<String, String> getCpuInfoFromProc() throws IOException {
        Map<String, String> cpuInfo = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    //extract important data
                    if (key.equals("Processor") || key.equals("model name") ||
                            key.equals("Hardware") || key.equals("cpu MHz") ||
                            key.equals("BogoMIPS") || key.equals("Features")) {

                        //convert key to a consistent format
                        key = key.replace(" ", "_").toLowerCase();
                        cpuInfo.put("cpu_" + key, value);
                    }
                }
            }
        }

        return cpuInfo;
    }

    //formats file format, return formatted size string
    private static String formatSize(long size) {
        if (size <= 0) return "0";

        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return String.format("%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    //get avg CPU frequency, return avg CPU frequency in MHz, or -1 if unable to determine
    public static double getAverageCpuFrequency() {
        try {
            double total = 0;
            int count = 0;

            File cpuDir = new File("/sys/devices/system/cpu/");
            File[] cpuFiles = cpuDir.listFiles(file -> file.getName().matches("cpu\\d+"));

            if (cpuFiles != null) {
                for (File cpuFile : cpuFiles) {
                    File freqFile = new File(cpuFile, "cpufreq/scaling_cur_freq");
                    if (freqFile.exists()) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(freqFile))) {
                            String line = reader.readLine();
                            if (line != null) {
                                // Convert from kHz to MHz
                                total += Long.parseLong(line.trim()) / 1000.0;
                                count++;
                            }
                        }
                    }
                }
            }

            return count > 0 ? total / count : -1;
        } catch (Exception e) {
            Log.e(TAG, "Error getting CPU frequency", e);
            return -1;
        }
    }

    //gets CPU util, return CPU utilization percentage, or -1 if unable
    public static double getCpuUtilization() {
        try {
            long[] prevCpuTime = getCpuTimeSnapshot();

            // wait for cpu time to update
            Thread.sleep(500);

            long[] currCpuTime = getCpuTimeSnapshot();

            long prevTotal = prevCpuTime[0] + prevCpuTime[1] + prevCpuTime[2] + prevCpuTime[3];
            long currTotal = currCpuTime[0] + currCpuTime[1] + currCpuTime[2] + currCpuTime[3];

            long prevIdle = prevCpuTime[3];
            long currIdle = currCpuTime[3];

            long totalDiff = currTotal - prevTotal;
            long idleDiff = currIdle - prevIdle;

            return totalDiff > 0 ? 100.0 * (1.0 - idleDiff / (double) totalDiff) : -1;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating CPU utilization", e);
            return -1;
        }
    }

     //gets CPU time snapshot, return  array containing [user, nice, system, idle] time then averaged
    private static long[] getCpuTimeSnapshot() throws IOException {
        long[] cpuTime = new long[4];  // user, nice, system, idle

        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = reader.readLine();
            if (line != null && line.startsWith("cpu ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 5) {
                    cpuTime[0] = Long.parseLong(parts[1]);  // user
                    cpuTime[1] = Long.parseLong(parts[2]);  // nice
                    cpuTime[2] = Long.parseLong(parts[3]);  // system
                    cpuTime[3] = Long.parseLong(parts[4]);  // idle
                }
            }
        }

        return cpuTime;
    }
}