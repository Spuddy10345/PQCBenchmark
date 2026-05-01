package com.example.pqcbenchmark;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

public class AlgorithmTest {

    @Test
    public void testKyberAlgorithm() {
        KyberAlgorithm kyber = new KyberAlgorithm();
        
        // Test key generation
        kyber.generateKeys();
        
        // Test encapsulation
        byte[] ciphertext = kyber.encapsulate();
        assertNotNull("Ciphertext should not be null", ciphertext);
        assertTrue("Ciphertext should not be empty", ciphertext.length > 0);
        
        // Test decapsulation
        byte[] sharedSecret = kyber.decapsulate();
        assertNotNull("Shared secret should not be null", sharedSecret);
        assertTrue("Shared secret should not be empty", sharedSecret.length > 0);
    }

    @Test
    public void testDilithiumAlgorithm() {
        DilithiumAlgorithm dilithium = new DilithiumAlgorithm();
        
        // Test key generation
        dilithium.generateKeys();
        
        // Test signing
        byte[] signature = dilithium.sign();
        assertNotNull("Signature should not be null", signature);
        assertTrue("Signature should not be empty", signature.length > 0);
        
        // Test verification
        boolean isValid = dilithium.verify();
        assertTrue("Signature should be valid", isValid);
    }

    @Test
    public void testFalconAlgorithm() {
        FalconAlgorithm falcon = new FalconAlgorithm();
        
        // Test key generation
        falcon.generateKeys();
        
        // Test signing
        byte[] signature = falcon.sign();
        assertNotNull("Signature should not be null", signature);
        assertTrue("Signature should not be empty", signature.length > 0);
        
        // Test verification
        boolean isValid = falcon.verify();
        assertTrue("Signature should be valid", isValid);
    }

    @Test
    public void testSphincsPlusAlgorithm() {
        SphincsPlusAlgorithm sphincs = new SphincsPlusAlgorithm();
        
        // Test key generation
        sphincs.generateKeys();
        
        // Test signing
        byte[] signature = sphincs.sign();
        assertNotNull("Signature should not be null", signature);
        assertTrue("Signature should not be empty", signature.length > 0);
        
        // Test verification
        boolean isValid = sphincs.verify();
        assertTrue("Signature should be valid", isValid);
    }

    @Test
    public void testRSAAlgorithm() {
        RSAAlgorithm rsa = new RSAAlgorithm();
        
        // Test key generation
        rsa.generateKeys();
        
        // Test encryption/decapsulation (RSA here implements KEMAlgorithm for simplicity in benchmark)
        byte[] ciphertext = rsa.encapsulate();
        assertNotNull(ciphertext);
        
        byte[] sharedSecret = rsa.decapsulate();
        assertNotNull(sharedSecret);
        
        // Test signing/verification
        byte[] signature = rsa.sign();
        assertNotNull(signature);
        
        boolean isValid = rsa.verify();
        assertTrue(isValid);
    }
}
