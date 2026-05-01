package com.example.pqcbenchmark;

import android.util.Log;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyPairGenerator;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPublicKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumSigner;

import java.security.SecureRandom;

// Implementation of CRYSTALS-Dilithium, a lattice-based digital signature algorithm. Dilithium is a NIST PQC standardized algorithm for digital signatures, resistant to quantum attacks.
public class DilithiumAlgorithm implements SignatureAlgorithm {
    private static final String TAG = "DilithiumAlgorithm";
    private static final DilithiumParameters PARAMETER_SET = DilithiumParameters.dilithium3;

    private AsymmetricCipherKeyPair keyPair;
    private DilithiumPublicKeyParameters publicKey;
    private DilithiumPrivateKeyParameters privateKey;
    private byte[] signature;
    private final byte[] message = new byte[1024]; // 1KB message for testing

    @Override
    public void generateKeys() {
        try {
            SecureRandom random = new SecureRandom();
            DilithiumKeyPairGenerator keyPairGen = new DilithiumKeyPairGenerator();
            DilithiumKeyGenerationParameters params = new DilithiumKeyGenerationParameters(
                    random, PARAMETER_SET);

            keyPairGen.init(params);
            keyPair = keyPairGen.generateKeyPair();

            publicKey = (DilithiumPublicKeyParameters) keyPair.getPublic();
            privateKey = (DilithiumPrivateKeyParameters) keyPair.getPrivate();

            Log.d(TAG, "Dilithium key pair generated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error generating Dilithium keys", e);
        }
    }

    @Override
    public byte[] sign() {
        try {
            if (privateKey == null) {
                generateKeys();
            }

            // Generate random message data
            new SecureRandom().nextBytes(message);

            DilithiumSigner signer = new DilithiumSigner();
            signer.init(true, privateKey);
            signature = signer.generateSignature(message);

            Log.d(TAG, "Dilithium signature generated, length: " + signature.length);
            return signature;
        } catch (Exception e) {
            Log.e(TAG, "Error in Dilithium signing", e);
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

            DilithiumSigner signer = new DilithiumSigner();
            signer.init(false, publicKey);
            boolean isValid = signer.verifySignature(message, signature);

            Log.d(TAG, "Dilithium signature verification result: " + isValid);
            return isValid;
        } catch (Exception e) {
            Log.e(TAG, "Error in Dilithium verification", e);
            return false;
        }
    }

    @Override
    public String getAlgorithmName() {
        return "CRYSTALS-Dilithium";
    }

    @Override
    public String getCategory() {
        return "Lattice-based Signature";
    }

    @Override
    public String getSecurityLevel() {
        // Dilithium3 provides approximately AES-192 equivalent security
        return "Level 3 (AES-192 equivalent)";
    }

    @Override
    public boolean isQuantumResistant() {
        return true;
    }
}