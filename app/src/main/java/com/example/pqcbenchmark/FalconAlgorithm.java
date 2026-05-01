package com.example.pqcbenchmark;

import android.util.Log;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.falcon.FalconKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.falcon.FalconKeyPairGenerator;
import org.bouncycastle.pqc.crypto.falcon.FalconParameters;
import org.bouncycastle.pqc.crypto.falcon.FalconPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.falcon.FalconPublicKeyParameters;
import org.bouncycastle.pqc.crypto.falcon.FalconSigner;

import java.security.SecureRandom;

// Implementation of Falcon, a lattice-based digital signature algorithm. Falcon is a NIST PQC standardized algorithm for digital signatures, known for its relatively small signatures compared to other lattice-based signature schemes.
public class FalconAlgorithm implements SignatureAlgorithm {
    private static final String TAG = "FalconAlgorithm";
    private static final FalconParameters PARAMETER_SET = FalconParameters.falcon_512;

    private AsymmetricCipherKeyPair keyPair;
    private FalconPublicKeyParameters publicKey;
    private FalconPrivateKeyParameters privateKey;
    private byte[] signature;
    private final byte[] message = new byte[1024]; // 1KB message for testing

    @Override
    public void generateKeys() {
        try {
            SecureRandom random = new SecureRandom();
            FalconKeyPairGenerator keyPairGen = new FalconKeyPairGenerator();
            FalconKeyGenerationParameters params = new FalconKeyGenerationParameters(
                    random, PARAMETER_SET);

            keyPairGen.init(params);
            keyPair = keyPairGen.generateKeyPair();

            publicKey = (FalconPublicKeyParameters) keyPair.getPublic();
            privateKey = (FalconPrivateKeyParameters) keyPair.getPrivate();

            Log.d(TAG, "Falcon key pair generated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error generating Falcon keys", e);
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

            FalconSigner signer = new FalconSigner();
            signer.init(true, privateKey);
            signature = signer.generateSignature(message);

            Log.d(TAG, "Falcon signature generated, length: " + signature.length);
            return signature;
        } catch (Exception e) {
            Log.e(TAG, "Error in Falcon signing", e);
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

            FalconSigner signer = new FalconSigner();
            signer.init(false, publicKey);
            boolean isValid = signer.verifySignature(message, signature);

            Log.d(TAG, "Falcon signature verification result: " + isValid);
            return isValid;
        } catch (Exception e) {
            Log.e(TAG, "Error in Falcon verification", e);
            return false;
        }
    }

    @Override
    public String getAlgorithmName() {
        return "Falcon";
    }

    @Override
    public String getCategory() {
        return "Lattice-based Signature";
    }

    @Override
    public String getSecurityLevel() {
        return "Level 1 (AES-128 equivalent)";
    }

    @Override
    public boolean isQuantumResistant() {
        return true;
    }
}