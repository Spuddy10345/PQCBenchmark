package com.example.pqcbenchmark;

// Base interface for all cryptographic algorithms
interface CryptoAlgorithm {
    // Generate cryptographic keys
void generateKeys();

    // Get the algorithm name @return The standard name of this algorithm
String getAlgorithmName();

    // Get the algorithm category @return Category description (e.g., "Lattice-based KEM", "Hash-based Signature")
String getCategory();

    // Get the security level @return Description of security level (e.g., "AES-128 equivalent")
String getSecurityLevel();

    // Get whether the algorithm is quantum-resistant @return true if the algorithm is resistant to quantum attacks
boolean isQuantumResistant();
}

// Interface for Key Encapsulation Mechanism (KEM) and encryption algorithms
interface KEMAlgorithm extends CryptoAlgorithm {
    // Encapsulate a shared secret (for KEM) or encrypt data @return The ciphertext or encapsulated data
byte[] encapsulate();

    // Decapsulate a shared secret (for KEM) or decrypt data @return The shared secret or decrypted data
byte[] decapsulate();
}

// Interface for digital signature algorithms
interface SignatureAlgorithm extends CryptoAlgorithm {
    // Sign a message @return The signature
byte[] sign();

    // Verify a signature @return true if the signature is valid
boolean verify();
}

// Interface for hybrid algorithms supporting both KEM and signatures (like RSA which can do both encryption and signing)
interface HybridAlgorithm extends KEMAlgorithm, SignatureAlgorithm {
    // Inherits methods from both parent interfaces
}