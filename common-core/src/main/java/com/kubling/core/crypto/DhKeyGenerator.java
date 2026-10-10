/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file was modified as part of the Kubling project.
 */

package com.kubling.core.crypto;

import com.kubling.core.CorePlugin;
import com.kubling.core.KublingRuntimeException;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Properties;


/**
 * Helper class that supports anonymous ephemeral Diffie-Hellman
 * <p>
 * Parameters are stored in the dh.properties file
 */
public class DhKeyGenerator {

    private static final String ALGORITHM = "DiffieHellman";
    private static final String DIGEST = "SHA-256";
    private static final DHParameterSpec DH_SPEC;
    private static final DHParameterSpec DH_SPEC_2048;

    static {
        DH_SPEC = loadKeySpecification("dh.properties");
        DH_SPEC_2048 = loadKeySpecification("dh-2048.properties");
    }

    private static DHParameterSpec loadKeySpecification(String propsFile) {
        Properties props = new Properties();
        try (InputStream is = DhKeyGenerator.class.getResourceAsStream(propsFile)) {
            try {
                props.load(is);
            } catch (IOException e) {
                throw new KublingRuntimeException(CorePlugin.Event.KBL10000, e);
            }
        } catch (IOException e) {
            // Ignored
        }
        BigInteger p = new BigInteger(props.getProperty("p"));
        BigInteger g = new BigInteger(props.getProperty("g"));
        return new DHParameterSpec(p, g, Integer.parseInt(props
                .getProperty("l")));
    }

    private PrivateKey privateKey;
    private PrivateKey privateKeyLarge;

    public byte[] createPublicKey(boolean large) throws CryptoException {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            if (large) {
                keyGen.initialize(DH_SPEC_2048);
            } else {
                keyGen.initialize(DH_SPEC);
            }
            KeyPair keypair = keyGen.generateKeyPair();

            if (large) {
                privateKeyLarge = keypair.getPrivate();
            } else {
                privateKey = keypair.getPrivate();
            }

            PublicKey publicKey = keypair.getPublic();

            return publicKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(CorePlugin.Event.KBL10001, e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new CryptoException(CorePlugin.Event.KBL10002, e);
        }
    }

    public SymmetricCryptor getSymmetricCryptor(
            byte[] peerPublicKeyBytes,
            boolean useSealedObject,
            ClassLoader classLoader,
            boolean large,
            boolean cbc) throws CryptoException {

        PrivateKey privKey = large ? privateKeyLarge : privateKey;
        if (privKey == null) {
            throw new IllegalStateException(
                    "KeyGenerator did not successfully generate public key");
        }
        try {
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(
                    peerPublicKeyBytes);
            KeyFactory keyFact = KeyFactory.getInstance(ALGORITHM);
            PublicKey publicKey = keyFact.generatePublic(x509KeySpec);

            KeyAgreement ka = KeyAgreement.getInstance(ALGORITHM);
            ka.init(privKey);
            ka.doPhase(publicKey, true);
            byte[] secret = ka.generateSecret();
            //we expect a 1024-bit DH key, but vms handle leading zeros differently
            if (secret.length < 128) {
                byte[] temp = new byte[128];
                System.arraycopy(secret, 0, temp, 128 - secret.length, secret.length);
                secret = temp;
            }
            //convert to expected bit length for AES
            MessageDigest sha = MessageDigest.getInstance(DIGEST);
            byte[] hash = sha.digest(secret);
            /*
             * TODO: add support for configurable key sizes
             */
            int keySize = SymmetricCryptor.DEFAULT_KEY_BITS;
            byte[] symKey = new byte[keySize / 8];
            System.arraycopy(hash, 0, symKey, 0, symKey.length);
            SymmetricCryptor sc = SymmetricCryptor.getSymmectricCryptor(symKey, cbc);
            sc.setUseSealedObject(useSealedObject);
            sc.setClassLoader(classLoader);
            return sc;
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(CorePlugin.Event.KBL10003, e);
        } catch (InvalidKeySpecException e) {
            throw new CryptoException(CorePlugin.Event.KBL10004, e);
        } catch (InvalidKeyException e) {
            throw new CryptoException(CorePlugin.Event.KBL10005, e);
        }
    }

    /**
     * Can be used to generate new parameters
     */
    public static void main(String[] args) throws Exception {
        AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator
                .getInstance(ALGORITHM);
        paramGen.init(2048);

        AlgorithmParameters params = paramGen.generateParameters();

        DHParameterSpec dhSpec = params.getParameterSpec(DHParameterSpec.class);
        System.out.println("l=" + dhSpec.getL());
        System.out.println("g=" + dhSpec.getG());
        System.out.println("p=" + dhSpec.getP());
    }

}
