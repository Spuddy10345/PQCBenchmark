package com.example.pqcbenchmark;

import android.util.Log;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import javax.crypto.Cipher;

// Implementation of RSA, a traditional cryptography algorithm supporting both encryption and digital signatures. This is included for comparison with post-quantum algorithms.
public class RSAAlgorithm implements HybridAlgorithm {
    private static final String TAG = "RSAAlgorithm";
    private static final int KEY_SIZE = 2048;  // Standard RSA key size

    private KeyPair keyPair;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private byte[] ciphertext;
    private byte[] signature;
    private final byte[] message = new byte[1024]; // 1KB for fair comparison

    @Override
    public void generateKeys() {
        try {
            SecureRandom random = new SecureRandom();
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(KEY_SIZE, random);

            keyPair = keyPairGen.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();

            Log.d(TAG, "RSA key pair generated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error generating RSA keys", e);
        }
    }

    @Override
    public byte[] encapsulate() {
        try {
            if (publicKey == null) {
                generateKeys();
            }

            // Generate random data
            new SecureRandom().nextBytes(message);

            // RSA has size limitations based on key size
            // For 2048-bit keys, we can encrypt up to ~245 bytes
            int maxSize = KEY_SIZE / 8 - 11;  // PKCS#1 padding overhead
            byte[] chunk = new byte[Math.min(message.length, maxSize)];
            System.arraycopy(message, 0, chunk, 0, chunk.length);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            ciphertext = cipher.doFinal(chunk);

            Log.d(TAG, "RSA encryption successful, ciphertext length: " + ciphertext.length);
            return ciphertext;
        } catch (Exception e) {
            Log.e(TAG, "Error in RSA encryption", e);
            return new byte[0];
        }
    }

    @Override
    public byte[] decapsulate() {
        try {
            if (privateKey == null || ciphertext == null) {
                Log.e(TAG, "Keys or ciphertext not initialized for decryption");
                return new byte[0];
            }

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedText = cipher.doFinal(ciphertext);

            Log.d(TAG, "RSA decryption successful, text length: " + decryptedText.length);
            return decryptedText;
        } catch (Exception e) {
            Log.e(TAG, "Error in RSA decryption", e);
            return new byte[0];
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

            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(message);
            signature = signer.sign();

            Log.d(TAG, "RSA signature generated, length: " + signature.length);
            return signature;
        } catch (Exception e) {
            Log.e(TAG, "Error in RSA signing", e);
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

            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(message);
            boolean isValid = verifier.verify(signature);

            Log.d(TAG, "RSA signature verification result: " + isValid);
            return isValid;
        } catch (Exception e) {
            Log.e(TAG, "Error in RSA verification", e);
            return false;
        }
    }

    @Override
    public String getAlgorithmName() {
        return "RSA";
    }

    @Override
    public String getCategory() {
        return "Traditional - Integer Factorization";
    }

    @Override
    public String getSecurityLevel() {
        return "2048-bit (Classical Security Only)";
    }

    @Override
    public boolean isQuantumResistant() {
        return false;
    }
}