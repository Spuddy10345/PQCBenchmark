package com.example.pqcbenchmark;

import android.util.Log;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature; // Implementation of ECDSA (Elliptic Curve Digital Signature Algorithm), a traditional digital signature algorithm. This is used for comparison with PQC signature algorithms.
public class ECDSAAlgorithm implements SignatureAlgorithm {
    private static final String TAG = "ECDSAAlgorithm";

    private KeyPair keyPair;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private byte[] signature;
    private final byte[] message = new byte[1024]; // 1KB for fair comparison

    @Override
    public void generateKeys() {
        try {
            SecureRandom random = new SecureRandom();
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("EC");
            keyPairGen.initialize(256, random); // P-256 curve

            keyPair = keyPairGen.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();

            Log.d(TAG, "ECDSA key pair generated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error generating ECDSA keys", e);
        }
    }

    @Override
    public byte[] sign() {
        try {
            if (privateKey == null) {
                generateKeys();
            }

            // Generate random data
            new SecureRandom().nextBytes(message);

            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(privateKey);
            signer.update(message);
            signature = signer.sign();

            Log.d(TAG, "ECDSA signature generated, length: " + signature.length);
            return signature;
        } catch (Exception e) {
            Log.e(TAG, "Error in ECDSA signing", e);
            return new byte[0];
        }
    }

    @Override
    public boolean verify() {
        try {
            if (publicKey == null || signature == null) {
                Log.e(TAG, "Keys or signature not initialized for verification");
                return false;
            }

            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(message);
            boolean isValid = verifier.verify(signature);

            Log.d(TAG, "ECDSA signature verification result: " + isValid);
            return isValid;
        } catch (Exception e) {
            Log.e(TAG, "Error in ECDSA verification", e);
            return false;
        }
    }

    @Override
    public String getAlgorithmName() {
        return "ECDSA";
    }

    @Override
    public String getCategory() {
        return "Traditional - Elliptic Curve Signature";
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
