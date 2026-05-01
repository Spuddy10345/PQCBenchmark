package com.example.pqcbenchmark;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "PQCBenchmark";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 101;

    // UI Components
    private Button startBenchmarkButton;
    private ProgressBar benchmarkProgress;
    private TextView statusText;
    private TextView resultsText;
    private TextView deviceInfoText;
    private Spinner algorithmSpinner;
    private RadioGroup algorithmTypeGroup;
    private RadioButton kemRadio;
    private RadioButton signatureRadio;
    private RadioButton allRadio;
    private CheckBox keyGenCheckbox;
    private CheckBox encapsulateCheckbox;
    private CheckBox decapsulateCheckbox;
    private CheckBox signCheckbox;
    private CheckBox verifyCheckbox;
    private CheckBox quantumResistantOnlyCheckbox;
    private CheckBox shareBenchmarkCheckbox;
    private LinearLayout encryptionOptionsLayout;
    private LinearLayout signatureOptionsLayout;

    // Benchmark components
    private AlgorithmManager algorithmManager;
    private BenchmarkReportGenerator reportGenerator;
    private List<BenchmarkResult> benchmarkResults;

    // Device information
    private Map<String, String> deviceInfo;
    private boolean isEmulated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize managers
        algorithmManager = new AlgorithmManager();
        benchmarkResults = new ArrayList<>();

        // Check if device is emulated
        isEmulated = isEmulator();
        reportGenerator = new BenchmarkReportGenerator(this, isEmulated);

        // Initialize UI components
        initializeUI();

        // Set up listeners
        setupListeners();

        // Get and display device information
        deviceInfo = DeviceInfoUtil.collectDeviceInfo(this);
        displayDeviceInfo();
    }

    private void initializeUI() {
        // Initialize main UI components
        startBenchmarkButton = findViewById(R.id.start_benchmark_button);
        benchmarkProgress = findViewById(R.id.benchmark_progress);
        statusText = findViewById(R.id.status_text);
        resultsText = findViewById(R.id.results_text);
        deviceInfoText = findViewById(R.id.device_info_text);

        // Initialize algorithm selection components
        algorithmTypeGroup = findViewById(R.id.algorithm_type_group);
        kemRadio = findViewById(R.id.kem_radio);
        signatureRadio = findViewById(R.id.signature_radio);
        allRadio = findViewById(R.id.all_radio);
        algorithmSpinner = findViewById(R.id.algorithm_spinner);

        // Initialize operation selection components
        keyGenCheckbox = findViewById(R.id.keygen_checkbox);
        encapsulateCheckbox = findViewById(R.id.encapsulate_checkbox);
        decapsulateCheckbox = findViewById(R.id.decapsulate_checkbox);
        signCheckbox = findViewById(R.id.sign_checkbox);
        verifyCheckbox = findViewById(R.id.verify_checkbox);

        // Initialize option layouts
        encryptionOptionsLayout = findViewById(R.id.encryption_options_layout);
        signatureOptionsLayout = findViewById(R.id.signature_options_layout);

        // Initialize filter options
        quantumResistantOnlyCheckbox = findViewById(R.id.quantum_resistant_only_checkbox);
        shareBenchmarkCheckbox = findViewById(R.id.share_benchmark_checkbox);

        // Set up initial algorithm list
        updateAlgorithmSpinner();
    }

    private void setupListeners() {
        // Start benchmark button
        startBenchmarkButton.setOnClickListener(v -> {
            if (checkAndRequestPermissions()) {
                runBenchmark();
            }
        });

        // Algorithm type selection
        algorithmTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateAlgorithmSpinner();
            updateOperationOptions();
        });

        // Quantum resistant filter
        quantumResistantOnlyCheckbox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateAlgorithmSpinner());

        // Algorithm selection
        algorithmSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateOperationOptions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void updateAlgorithmSpinner() {
        List<AlgorithmManager.AlgorithmInfo> algorithms = new ArrayList<>();

        // Get algorithms based on selected type
        if (kemRadio.isChecked()) {
            algorithms.addAll(algorithmManager.getEncryptionAlgorithms());
        } else if (signatureRadio.isChecked()) {
            algorithms.addAll(algorithmManager.getSignatureAlgorithms());
        } else { // All algorithms
            algorithms.addAll(algorithmManager.getAllAlgorithms());
        }

        // Apply quantum resistant filter if selected
        if (quantumResistantOnlyCheckbox.isChecked()) {
            algorithms.removeIf(algorithm -> !algorithm.isQuantumResistant());
        }

        // Create adapter
        ArrayAdapter<AlgorithmManager.AlgorithmInfo> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, algorithms);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        algorithmSpinner.setAdapter(adapter);
    }

    private void updateOperationOptions() {
        // Get selected algorithm
        if (algorithmSpinner.getSelectedItem() == null) {
            return;
        }

        AlgorithmManager.AlgorithmInfo selectedAlgorithm =
                (AlgorithmManager.AlgorithmInfo) algorithmSpinner.getSelectedItem();

        // Show/hide operation options based on algorithm capabilities
        encryptionOptionsLayout.setVisibility(
                selectedAlgorithm.supportsEncryption() ? View.VISIBLE : View.GONE);
        signatureOptionsLayout.setVisibility(
                selectedAlgorithm.supportsSignatures() ? View.VISIBLE : View.GONE);

        // Reset checkboxes
        keyGenCheckbox.setChecked(true); // Always enable key generation
        encapsulateCheckbox.setChecked(selectedAlgorithm.supportsEncryption());
        decapsulateCheckbox.setChecked(selectedAlgorithm.supportsEncryption());
        signCheckbox.setChecked(selectedAlgorithm.supportsSignatures());
        verifyCheckbox.setChecked(selectedAlgorithm.supportsSignatures());
    }

    private boolean checkAndRequestPermissions() {
        // For Android 10+ (API 29+), we need to request special permission for file access
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Storage Permission Required");
                builder.setMessage("This app needs permission to manage all files to save benchmark results. Please grant this permission in the next screen.");
                builder.setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, STORAGE_PERMISSION_REQUEST_CODE);
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
                return false;
            }
            return true;
        }
        // For Android 6-9, we need to request runtime permissions
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
            return true;
        }
        // For Android 5.1 and below, permissions are granted at install time
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runBenchmark();
            } else {
                Toast.makeText(this, "Permission denied. Cannot save benchmark results.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                runBenchmark();
            } else {
                Toast.makeText(this, "Permission denied. Cannot save benchmark results.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void runBenchmark() {
        // Get selected algorithm
        AlgorithmManager.AlgorithmInfo algorithmInfo =
                (AlgorithmManager.AlgorithmInfo) algorithmSpinner.getSelectedItem();

        if (algorithmInfo == null) {
            Toast.makeText(this, "Please select an algorithm", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create algorithm instance
        CryptoAlgorithm algorithm = AlgorithmFactory.createAlgorithm(algorithmInfo.getName());

        if (algorithm == null) {
            Toast.makeText(this, "Failed to create algorithm instance", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected operations
        Set<String> selectedOperations = new HashSet<>();

        if (keyGenCheckbox.isChecked()) {
            selectedOperations.add("KeyGen");
        }

        if (algorithmInfo.supportsEncryption()) {
            if (encapsulateCheckbox.isChecked()) {
                selectedOperations.add("Encapsulate");
            }
            if (decapsulateCheckbox.isChecked()) {
                selectedOperations.add("Decapsulate");
            }
        }

        if (algorithmInfo.supportsSignatures()) {
            if (signCheckbox.isChecked()) {
                selectedOperations.add("Sign");
            }
            if (verifyCheckbox.isChecked()) {
                selectedOperations.add("Verify");
            }
        }

        // Verify at least one operation is selected
        if (selectedOperations.isEmpty()) {
            Toast.makeText(this, "Please select at least one operation to benchmark", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get device info
        String deviceModel = deviceInfo.getOrDefault("model", "Unknown Device");

        // Start benchmark
        startBenchmark(algorithm, deviceModel, selectedOperations);
    }

    private void startBenchmark(CryptoAlgorithm algorithm, String deviceModel, Set<String> operations) {
        // Disable UI during benchmark
        startBenchmarkButton.setEnabled(false);
        benchmarkProgress.setVisibility(View.VISIBLE);
        statusText.setText("Running benchmark...");
        resultsText.setText("");

        // Run benchmark in background thread
        new Thread(() -> {
            BenchmarkTask task = new BenchmarkTask(this, algorithm, deviceModel, operations, isEmulated);
            BenchmarkResult result = task.execute();

            // Add result to list
            benchmarkResults.add(result);
            reportGenerator.addResult(result);

            // Update UI
            runOnUiThread(() -> {
                // Re-enable UI
                startBenchmarkButton.setEnabled(true);
                benchmarkProgress.setVisibility(View.GONE);
                statusText.setText("Benchmark complete");

                // Show results
                showResults(result);

                // Ask if user wants to run another benchmark or generate report
                if (benchmarkResults.size() > 1) {
                    askForMoreBenchmarks();
                }

                // Share results if requested
                if (shareBenchmarkCheckbox.isChecked()) {
                    shareResults();
                }
            });
        }).start();
    }

    // Ask user if they want to run more benchmarks or generate a report
private void askForMoreBenchmarks() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Benchmark Completed");
        builder.setMessage("You have completed " + benchmarkResults.size() + " benchmarks. " +
                "Would you like to run more benchmarks or generate a report?");

        builder.setPositiveButton("Run More", (dialog, which) -> dialog.dismiss());

        builder.setNegativeButton("Generate Report", (dialog, which) -> {
            dialog.dismiss();
            generateReport();
        });

        builder.create().show();
    }

    // Display device information on screen
private void displayDeviceInfo() {
        if (deviceInfo == null || deviceInfo.isEmpty()) {
            deviceInfoText.setText("Device information not available");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Model: ").append(deviceInfo.getOrDefault("model", "Unknown")).append("\n");
        sb.append("CPU: ").append(deviceInfo.getOrDefault("cpuCores", "Unknown") + " cores").append("\n");
        sb.append("RAM: ").append(deviceInfo.getOrDefault("totalMemory", "Unknown")).append("\n");
        sb.append("Android: ").append(deviceInfo.getOrDefault("androidVersion", "Unknown"))
                .append(" (API ").append(deviceInfo.getOrDefault("apiLevel", "Unknown")).append(")");

        // Add emulator detection
        if (isEmulated) {
            sb.append("\nRunning on emulator");
        }

        deviceInfoText.setText(sb.toString());
    }

    // Generate and save benchmark report
private void generateReport() {
        // Show progress
        statusText.setText("Generating report...");
        benchmarkProgress.setVisibility(View.VISIBLE);

        // Generate report in background
        new Thread(() -> {
            Uri reportUri = reportGenerator.generateReport();

            // Update UI
            runOnUiThread(() -> {
                benchmarkProgress.setVisibility(View.GONE);

                if (reportUri != null) {
                    statusText.setText("Report generated successfully");

                    // Ask if user wants to view the report
                    askToViewReport(reportUri);
                } else {
                    statusText.setText("Failed to generate report");
                    Toast.makeText(this, "Error generating report", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // Ask user if they want to view the generated report
private void askToViewReport(Uri reportUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report Generated");
        builder.setMessage("The benchmark report has been generated. Would you like to view it?");

        builder.setPositiveButton("View", (dialog, which) -> {
            dialog.dismiss();

            // Open report with external viewer
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(reportUri, "text/csv");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivity(Intent.createChooser(intent, "Open Report"));
            } catch (Exception e) {
                Toast.makeText(this, "No app found to open CSV files", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Later", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    // Share benchmark results
private void shareResults() {
        // Generate report
        Uri reportUri = reportGenerator.generateReport();

        if (reportUri != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "PQC Benchmark Results");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Here are my PQC benchmark results");
            shareIntent.putExtra(Intent.EXTRA_STREAM, reportUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Benchmark Results"));
        } else {
            Toast.makeText(this, "Error sharing results", Toast.LENGTH_SHORT).show();
        }
    }

    private void showResults(BenchmarkResult result) {
        // Display results in the results TextView
        StringBuilder sb = new StringBuilder();
        sb.append("Algorithm: ").append(result.getAlgorithm()).append("\n");
        sb.append("Category: ").append(result.getCategory()).append("\n");
        sb.append("Device: ").append(result.getDeviceTier()).append("\n");
        sb.append("Quantum Resistant: ").append(result.isQuantumResistant() ? "Yes" : "No").append("\n\n");

        for (OperationMetric metric : result.getMetrics()) {
            sb.append(metric.getOperation()).append(":\n");
            sb.append("  Time: ").append(String.format("%.2f ms", metric.getExecutionTime())).append("\n");
            sb.append("  Memory: ").append(String.format("%.2f MB", metric.getMemoryUsage())).append("\n");
            sb.append("  CPU: ").append(String.format("%.2f%%", metric.getCpuUtilization())).append("\n");
            sb.append("  Est. Power: ").append(String.format("%.2f", metric.getPowerUsage())).append("\n\n");
        }

        resultsText.setText(sb.toString());
    }

    // Check if the device is an emulator
private boolean isEmulator() {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator");
    }
}