/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.common;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;

import com.sapienter.jbilling.server.util.Util;

public final class JBCryptoImpl extends JBCrypto {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String ALGORITHM = "PBEWithMD5AndDES";
    private static SecretKeyFactory ourKeyFactory;
    private static final PBEParameterSpec ourPBEParameters;
    
    //private static final Logger LOG = Logger.getLogger(JBCryptoImpl.class);

    private final SecretKey mySecretKey;

    public JBCryptoImpl(String password) throws InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray());
        SecretKeyFactory keyFactory = getSecretKeyFactory();
        mySecretKey = keyFactory.generateSecret(spec);
    }

    public String decrypt(String cryptedText) {
        Cipher cipher = getCipher();
        byte[] crypted = useHexForBinary ? Util.stringToBinary(cryptedText) :
                Base64.decodeBase64(cryptedText.getBytes());
        byte[] result;
        try {
            cipher.init(Cipher.DECRYPT_MODE, mySecretKey, ourPBEParameters);
            result = cipher.doFinal(crypted);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Can not decrypt:" + cryptedText, e);
        }
        return new String(result, UTF8);
    }

    public String encrypt(String text) {
        Cipher cipher = getCipher();
        byte[] crypted;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, mySecretKey, ourPBEParameters);
            byte[] bytes = text.getBytes(UTF8);
            crypted = cipher.doFinal(bytes);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Can not encrypt :" + text, e);
        }
        String cryptedText = useHexForBinary ? Util.binaryToString(crypted) :
                new String(Base64.encodeBase64(crypted));
        return cryptedText;
    }
    
    private static SecretKeyFactory getSecretKeyFactory() {
        if (ourKeyFactory == null) {
            try {
                ourKeyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Algorithm is not supported: "
                        + ALGORITHM, e);
            }
        }
        return ourKeyFactory;
    }

    private static Cipher getCipher() {
        Cipher ourCipher;
        try {
            ourCipher = Cipher.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "Strange. Algorithm was supported a few seconds ago : "
                        + ALGORITHM, e);
        } catch (NoSuchPaddingException e) {
            throw new IllegalStateException(e);
        }
        return ourCipher;
    }
    
    static {
        // DON'T CHANGE THIS
        // IT WOULD BREAK BACKWARD COMPATIBILITY
        ourPBEParameters = new PBEParameterSpec(new byte[] { //
                (byte) 0x3c, (byte) 0x15, // 
                        (byte) 0x27, (byte) 0x7f, //
                        (byte) 0x2d, (byte) 0xda, //
                        (byte) 0xe6, (byte) 0x64, //
                }, 15);
    }
    
    static {
        
    }
}
