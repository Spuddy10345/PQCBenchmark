package com.example.pqcbenchmark;

import android.util.Log;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.sphincsplus.SPHINCSPlusKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.sphincsplus.SPHINCSPlusKeyPairGenerator;
import org.bouncycastle.pqc.crypto.sphincsplus.SPHINCSPlusParameters;
import org.bouncycastle.pqc.crypto.sphincsplus.SPHINCSPlusPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.sphincsplus.SPHINCSPlusPublicKeyParameters;
import org.bouncycastle.pqc.crypto.sphincsplus.SPHINCSPlusSigner;

import java.security.SecureRandom;

// Implementation of SPHINCS+, a stateless hash-based digital signature algorithm. SPHINCS+ is a NIST PQC standardized algorithm for digital signatures with high security guarantees based on hash functions (no reliance on complex mathematical problems).
public class SphincsPlusAlgorithm implements SignatureAlgorithm {
    private static final String TAG = "SphincsPlusAlgorithm";
    private static final SPHINCSPlusParameters PARAMETER_SET = SPHINCSPlusParameters.sha2_256s;

    private AsymmetricCipherKeyPair keyPair;
    private SPHINCSPlusPublicKeyParameters publicKey;
    private SPHINCSPlusPrivateKeyParameters privateKey;
    private byte[] signature;
    private final byte[] message = new byte[1024]; // 1KB message for testing

    @Override
    public void generateKeys() {
        try {
            SecureRandom random = new SecureRandom();
            SPHINCSPlusKeyPairGenerator keyPairGen = new SPHINCSPlusKeyPairGenerator();
            SPHINCSPlusKeyGenerationParameters params = new SPHINCSPlusKeyGenerationParameters(
                    random, PARAMETER_SET);

            keyPairGen.init(params);
            keyPair = keyPairGen.generateKeyPair();

            publicKey = (SPHINCSPlusPublicKeyParameters) keyPair.getPublic();
            privateKey = (SPHINCSPlusPrivateKeyParameters) keyPair.getPrivate();

            Log.d(TAG, "SPHINCS+ key pair generated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error generating SPHINCS+ keys", e);
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

            SPHINCSPlusSigner signer = new SPHINCSPlusSigner();
            signer.init(true, privateKey);
            signature = signer.generateSignature(message);

            Log.d(TAG, "SPHINCS+ signature generated, length: " + signature.length);
            return signature;
        } catch (Exception e) {
            Log.e(TAG, "Error in SPHINCS+ signing", e);
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

            SPHINCSPlusSigner signer = new SPHINCSPlusSigner();
            signer.init(false, publicKey);
            boolean isValid = signer.verifySignature(message, signature);

            Log.d(TAG, "SPHINCS+ signature verification result: " + isValid);
            return isValid;
        } catch (Exception e) {
            Log.e(TAG, "Error in SPHINCS+ verification", e);
            return false;
        }
    }

    @Override
    public String getAlgorithmName() {
        return "SPHINCS+";
    }

    @Override
    public String getCategory() {
        return "Hash-based Signature";
    }

    @Override
    public String getSecurityLevel() {
        return "Level 5 (AES-256 equivalent)";
    }

    @Override
    public boolean isQuantumResistant() {
        return true;
    }
}