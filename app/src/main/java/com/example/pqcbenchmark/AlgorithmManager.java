package com.example.pqcbenchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map; // Manager for organizing and accessing available cryptographic algorithms
public class AlgorithmManager {
    private final List<AlgorithmInfo> algorithms = new ArrayList<>();
    private final Map<String, List<AlgorithmInfo>> algorithmsByCategory = new HashMap<>();

    public AlgorithmManager() {
        initializeAlgorithms();
    }

    // Initialize the list of available algorithms
private void initializeAlgorithms() {
        // Post-Quantum KEM Algorithms
        addAlgorithm("CRYSTALS-Kyber", "Lattice-based KEM", true, true, false);

        // Post-Quantum Signature Algorithms
        addAlgorithm("CRYSTALS-Dilithium", "Lattice-based Signature", false, true, true);
        addAlgorithm("SPHINCS+", "Hash-based Signature", false, true, true);
        addAlgorithm("Falcon", "Lattice-based Signature", false, true, true);

        // Traditional Cryptography Algorithms
        addAlgorithm("RSA", "Traditional - Integer Factorization", true, false, true);
        addAlgorithm("ECDH", "Traditional - Elliptic Curve Key Exchange", true, false, false);
        addAlgorithm("ECDSA", "Traditional - Elliptic Curve Signature", false, false, true);
    }

    // Add an algorithm to the manager
private void addAlgorithm(String name, String category, boolean supportsEncryption,
                              boolean isQuantumResistant, boolean supportsSignatures) {
        AlgorithmInfo info = new AlgorithmInfo(
                name, category, supportsEncryption, isQuantumResistant, supportsSignatures);

        algorithms.add(info);

        // Add to category map
        if (!algorithmsByCategory.containsKey(category)) {
            algorithmsByCategory.put(category, new ArrayList<>());
        }
        algorithmsByCategory.get(category).add(info);
    }

    // Get all available algorithms
public List<AlgorithmInfo> getAllAlgorithms() {
        return new ArrayList<>(algorithms);
    }

    // Get all KEM/encryption algorithms
public List<AlgorithmInfo> getEncryptionAlgorithms() {
        List<AlgorithmInfo> result = new ArrayList<>();
        for (AlgorithmInfo info : algorithms) {
            if (info.supportsEncryption()) {
                result.add(info);
            }
        }
        return result;
    }

    // Get all signature algorithms
public List<AlgorithmInfo> getSignatureAlgorithms() {
        List<AlgorithmInfo> result = new ArrayList<>();
        for (AlgorithmInfo info : algorithms) {
            if (info.supportsSignatures()) {
                result.add(info);
            }
        }
        return result;
    }

    // Get all post-quantum algorithms
public List<AlgorithmInfo> getPostQuantumAlgorithms() {
        List<AlgorithmInfo> result = new ArrayList<>();
        for (AlgorithmInfo info : algorithms) {
            if (info.isQuantumResistant()) {
                result.add(info);
            }
        }
        return result;
    }

    // Get all traditional algorithms
public List<AlgorithmInfo> getTraditionalAlgorithms() {
        List<AlgorithmInfo> result = new ArrayList<>();
        for (AlgorithmInfo info : algorithms) {
            if (!info.isQuantumResistant()) {
                result.add(info);
            }
        }
        return result;
    }

    // Get algorithms by category
public List<AlgorithmInfo> getAlgorithmsByCategory(String category) {
        return algorithmsByCategory.getOrDefault(category, new ArrayList<>());
    }

    // Get available operations for an algorithm
public List<String> getAvailableOperations(String algorithmName) {
        List<String> operations = new ArrayList<>();

        // Always add key generation
        operations.add("KeyGen");

        // Get algorithm info
        AlgorithmInfo info = getAlgorithmInfo(algorithmName);
        if (info == null) {
            return operations;
        }

        // Add appropriate operations based on algorithm type
        if (info.supportsEncryption()) {
            operations.add("Encapsulate");
            operations.add("Decapsulate");
        }

        if (info.supportsSignatures()) {
            operations.add("Sign");
            operations.add("Verify");
        }

        return operations;
    }

    // Get algorithm info by name
public AlgorithmInfo getAlgorithmInfo(String algorithmName) {
        for (AlgorithmInfo info : algorithms) {
            if (info.getName().equals(algorithmName)) {
                return info;
            }
        }
        return null;
    }

    // Class to hold algorithm information
public static class AlgorithmInfo {
        private final String name;
        private final String category;
        private final boolean supportsEncryption;
        private final boolean isQuantumResistant;
        private final boolean supportsSignatures;

        public AlgorithmInfo(String name, String category, boolean supportsEncryption,
                             boolean isQuantumResistant, boolean supportsSignatures) {
            this.name = name;
            this.category = category;
            this.supportsEncryption = supportsEncryption;
            this.isQuantumResistant = isQuantumResistant;
            this.supportsSignatures = supportsSignatures;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public boolean supportsEncryption() {
            return supportsEncryption;
        }

        public boolean isQuantumResistant() {
            return isQuantumResistant;
        }

        public boolean supportsSignatures() {
            return supportsSignatures;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
