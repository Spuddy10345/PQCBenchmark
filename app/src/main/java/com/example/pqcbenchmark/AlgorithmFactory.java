package com.example.pqcbenchmark;

// Factory for creating cryptographic algorithm instances
public class AlgorithmFactory {

    // Create a cryptographic algorithm instance by:
    // name @param algorithmName The name of the algorithm to create
    // @return The algorithm instance, or null if not found
public static CryptoAlgorithm createAlgorithm(String algorithmName) {
        switch (algorithmName) {
            // Post-Quantum KEM Algorithms
            case "CRYSTALS-Kyber":
                return new KyberAlgorithm();

            // Post-Quantum Signature Algorithms
            case "CRYSTALS-Dilithium":
                return new DilithiumAlgorithm();
            case "SPHINCS+":
                return new SphincsPlusAlgorithm();
            case "Falcon":
                return new FalconAlgorithm();

            // Traditional Cryptography Algorithms
            case "RSA":
                return new RSAAlgorithm();
            case "ECDH":
                return new ECDHAlgorithm();
            case "ECDSA":
                return new ECDSAAlgorithm();

            default:
                return null;
        }
    }
}

