package com.example.pqcbenchmark;

import android.util.Log;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import javax.crypto.KeyAgreement;

// Implementation of ECDH (Elliptic Curve Diffie-Hellman), a traditional key exchange mechanism. This is used for comparison with PQC KEM algorithms.
public class ECDHAlgorithm implements KEMAlgorithm {
    private static final String TAG = "ECDHAlgorithm";
    private static final String CURVE = "secp256r1"; // P-256 curve

    private KeyPair keyPair;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private KeyPair otherKeyPair;  // For simulating the other party
    private byte[] sharedSecret;

    @Override
    public void generateKeys() {
        try {
            SecureRandom random = new SecureRandom();
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("EC");
            keyPairGen.initialize(256, random); // P-256 curve

            // Generate our key pair
            keyPair = keyPairGen.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();

            // Generate other party's key pair for simulation
            otherKeyPair = keyPairGen.generateKeyPair();

            Log.d(TAG, "ECDH key pairs generated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error generating ECDH keys", e);
        }
    }

    @Override
    public byte[] encapsulate() {
        try {
            if (privateKey == null || otherKeyPair == null) {
                generateKeys();
            }

            // Perform ECDH key agreement
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(otherKeyPair.getPublic(), true);

            // This would normally be sent to the other party
            sharedSecret = keyAgreement.generateSecret();

            Log.d(TAG, "ECDH key agreement completed, shared secret length: " + sharedSecret.length);

            // Return the public key bytes (in a real scenario, this would be sent to the other party)
            return publicKey.getEncoded();
        } catch (Exception e) {
            Log.e(TAG, "Error in ECDH key agreement", e);
            return new byte[0];
        }
    }

    @Override
    public byte[] decapsulate() {
        try {
            if (privateKey == null || otherKeyPair == null) {
                Log.e(TAG, "Keys not initialized for ECDH");
                return new byte[0];
            }

            // In a real scenario, this would use the received public key
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(otherKeyPair.getPrivate());
            keyAgreement.doPhase(publicKey, true);

            byte[] otherSharedSecret = keyAgreement.generateSecret();

            Log.d(TAG, "ECDH verification: other shared secret matches: " +
                    java.util.Arrays.equals(sharedSecret, otherSharedSecret));

            return otherSharedSecret;
        } catch (Exception e) {
            Log.e(TAG, "Error in ECDH verification", e);
            return new byte[0];
        }
    }

    @Override
    public String getAlgorithmName() {
        return "ECDH";
    }

    @Override
    public String getCategory() {
        return "Traditional - Elliptic Curve Key Exchange";
    }

    @Override
    public String getSecurityLevel() {
        return "256-bit (Classical Security Only)";
    }

    @Override
    public boolean isQuantumResistant() {
        return false;
    }
}

