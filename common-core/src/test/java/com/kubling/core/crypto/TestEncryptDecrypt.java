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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SuppressWarnings("nls")
public class TestEncryptDecrypt {

    // Some strings for testing...
    private static final String ALPHA_U = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String ALPHA_L = ALPHA_U.toLowerCase();
    private static final String NUMBERS = "0123456789";
    private static final String MISC_CHAR = "<>,.:;'{}[][]|`~!@#$%^&*()_+-=";

    /**
     * String to encrypt and decrypt.
     */
    private static final String CLEARTEXT = ALPHA_U + ALPHA_L + NUMBERS + MISC_CHAR;

    private static Cryptor cryptor;

    @BeforeAll
    public static void oneTimeSetup() throws CryptoException, IOException {
        cryptor = SymmetricCryptor.getSymmectricCryptor(TestEncryptDecrypt.class.getResource("/kubling.keystore"));
    }

    // =========================================================================
    //                      H E L P E R    M E T H O D S
    // =========================================================================

    /**
     * Test encryption (and decryption) for specified string.
     */
    public void helpTestEncryptDecrypt(String cleartext) throws CryptoException {
//      Encrypt the cleartext into ciphertext
        byte[] ciphertext = cryptor.encrypt(cleartext.getBytes(Charset.forName("UTF-8")));
        byte[] cleartext2 = cryptor.decrypt(ciphertext);

        assertArrayEquals(cleartext.getBytes(Charset.forName("UTF-8")), cleartext2);
    }

    // =========================================================================
    //                         T E S T     C A S E S
    // =========================================================================

    /**
     * Test the {@link Cryptor#encrypt} method.
     *
     * @throws CryptoException
     */
    @Test
    public void testPos_EncryptDecryptLongString() throws CryptoException {
        helpTestEncryptDecrypt(CLEARTEXT);
    }

    /**
     * Test the {@link Cryptor#encrypt} method.
     *
     * @throws CryptoException
     */
    @Test
    public void testPos_EncryptDecryptHalfLongString() throws CryptoException {
        helpTestEncryptDecrypt(CLEARTEXT.substring(0, CLEARTEXT.length() / 2));
    }

    /**
     * Test the {@link Cryptor#encrypt} method.
     *
     * @throws CryptoException
     */
    @Test
    public void testPos_EncryptDecryptStringsOfVariousLengths() throws CryptoException {
        for (int k = 1; k < CLEARTEXT.length() / 4; k++) {
            // Use substring starting at index k, and 'k' characters long
            String cleartext = CLEARTEXT.substring(k, k + k);
            helpTestEncryptDecrypt(cleartext);
        }
    }

    /**
     * Test the {@link Cryptor#encrypt} method.
     *
     * @throws CryptoException
     */
    @Test
    public void testPos_EncryptDecryptStringsOfBlanks() throws CryptoException {
        String BLANKS = "          ";
        for (int k = 1; k < BLANKS.length(); k++) {
            // Use substring starting at index 0, and 'k' characters long
            String cleartext = BLANKS.substring(0, k);
            helpTestEncryptDecrypt(cleartext);
        }
    }

    /**
     * Test the {@link Cryptor#encrypt} method.
     */
    @Test
    public void testNeg_DecryptNullString() throws Exception {
        // Decrypt the Base64 encoded ciphertext back to the original cleartext
        try {
            cryptor.decrypt(null);
            fail("expected exception");
        } catch (CryptoException e) {
            //expected
        }
    }

    @Test
    public void testLongEncryption() throws Exception {
        helpTestEncryptDecrypt(CLEARTEXT + CLEARTEXT + CLEARTEXT);
    }

    @Test
    public void testSymmetricEncryptionWithRandomKey() throws Exception {

        SymmetricCryptor randomSymCryptor = SymmetricCryptor.getSymmectricCryptor(true);

        ArrayList<String> test = new ArrayList<>(
                List.of(ALPHA_L, ALPHA_U, CLEARTEXT, NUMBERS));

        Object result = randomSymCryptor.sealObject(test);

        //ensure that we can serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(result);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        result = ois.readObject();

        Object clearObject = randomSymCryptor.unsealObject(result);

        assertEquals(test, clearObject);

        SymmetricCryptor cryptor1 = SymmetricCryptor.getSymmectricCryptor(randomSymCryptor.getEncodedKey(), true);

        clearObject = cryptor1.unsealObject(result);

        assertEquals(test, clearObject);
    }

    @Test
    public void testConstructorDoesNotInvokeOverridableCipherInitializers() throws Exception {
        TrackingCryptor trackingCryptor = new TrackingCryptor(SymmetricCryptor.generateKey());

        assertFalse(trackingCryptor.encryptInitializerInvoked);
        assertFalse(trackingCryptor.decryptInitializerInvoked);

        byte[] cleartext = CLEARTEXT.getBytes(Charset.forName("UTF-8"));
        assertArrayEquals(cleartext, trackingCryptor.decrypt(trackingCryptor.encrypt(cleartext)));
    }

    private static final class TrackingCryptor extends BasicCryptor {

        private boolean encryptInitializerInvoked;
        private boolean decryptInitializerInvoked;

        private TrackingCryptor(SecretKey key) throws CryptoException {
            super(key, key, SymmetricCryptor.ECB_SYM_ALGORITHM, null);
        }

        @Override
        protected void initEncryptCipher() throws CryptoException {
            encryptInitializerInvoked = true;
            super.initEncryptCipher();
        }

        @Override
        protected void initDecryptCipher() throws CryptoException {
            decryptInitializerInvoked = true;
            super.initDecryptCipher();
        }
    }

}
