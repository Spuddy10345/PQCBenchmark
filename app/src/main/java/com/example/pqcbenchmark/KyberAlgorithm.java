package com.example.pqcbenchmark;

import android.util.Log;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberKEMExtractor;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberKEMGenerator;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberKeyPairGenerator;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberParameters;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberPublicKeyParameters;

import java.security.SecureRandom;

// Implementation of CRYSTALS-Kyber, a lattice-based Key Encapsulation Mechanism (KEM). Kyber is a NIST PQC standardized algorithm for key exchange, resistant to quantum attacks.
public class KyberAlgorithm implements KEMAlgorithm {
    private static final String TAG = "KyberAlgorithm";
    private static final KyberParameters PARAMETER_SET = KyberParameters.kyber768;

    private AsymmetricCipherKeyPair keyPair;
    private KyberPublicKeyParameters publicKey;
    private KyberPrivateKeyParameters privateKey;
    private byte[] ciphertext;
    private byte[] sharedSecret;

    @Override
    public void generateKeys() {
        try {
            SecureRandom random = new SecureRandom();
            KyberKeyPairGenerator keyPairGen = new KyberKeyPairGenerator();
            KyberKeyGenerationParameters params = new KyberKeyGenerationParameters(random, PARAMETER_SET);

            keyPairGen.init(params);
            keyPair = keyPairGen.generateKeyPair();

            publicKey = (KyberPublicKeyParameters) keyPair.getPublic();
            privateKey = (KyberPrivateKeyParameters) keyPair.getPrivate();

            Log.d(TAG, "Kyber key pair generated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error generating Kyber keys", e);
        }
    }

    @Override
    public byte[] encapsulate() {
        try {
            if (publicKey == null) {
                generateKeys();
            }

            KyberKEMGenerator kemGen = new KyberKEMGenerator(new SecureRandom());

            // Generate encapsulated secret
            SecretWithEncapsulation secretWithEncapsulation = kemGen.generateEncapsulated(publicKey);
            sharedSecret = secretWithEncapsulation.getSecret();
            ciphertext = secretWithEncapsulation.getEncapsulation();

            Log.d(TAG, "Kyber encapsulation successful, ciphertext length: " + ciphertext.length);
            return ciphertext;
        } catch (Exception e) {
            Log.e(TAG, "Error in Kyber encapsulation", e);
            return new byte[0];
        }
    }

    @Override
    public byte[] decapsulate() {
        try {
            if (privateKey == null || ciphertext == null) {
                Log.e(TAG, "Keys or ciphertext not initialized for decapsulation");
                return new byte[0];
            }

            // Decapsulate to recover the shared secret
            KyberKEMExtractor kemExtractor = new KyberKEMExtractor(privateKey);
            byte[] decryptedSecret = kemExtractor.extractSecret(ciphertext);

            Log.d(TAG, "Kyber decapsulation successful, secret length: " + decryptedSecret.length);
            return decryptedSecret;
        } catch (Exception e) {
            Log.e(TAG, "Error in Kyber decapsulation", e);
            return new byte[0];
        }
    }

    @Override
    public String getAlgorithmName() {
        return "CRYSTALS-Kyber";
    }

    @Override
    public String getCategory() {
        return "Lattice-based KEM";
    }

    @Override
    public String getSecurityLevel() {
        // Kyber-768 provides approximately AES-192 equivalent security
        return "Level 3 (AES-192 equivalent)";
    }

    @Override
    public boolean isQuantumResistant() {
        return true;
    }
}