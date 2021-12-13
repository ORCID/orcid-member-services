package org.orcid.memberportal.service.user.security;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;

@Component
public class EncryptUtil {

    private String keyValue = "Abcdefghijklmnop";

    private String salt = "dc0da04af8fee58593442bf834b30739";

    private final SecretKeyFactory factory;

    private final KeySpec spec = new PBEKeySpec(keyValue.toCharArray(), hex(salt), 1000, 128);

    {
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String encrypt(String toEncrypt) {
        try {
            Key key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(hex(salt)));

            byte[] encVal = c.doFinal(toEncrypt.getBytes());
            return new String(Base64.encodeBase64URLSafe(encVal));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException n) {
            throw new RuntimeException(n);
        }
    }

    public String decrypt(String toDecrypt) {
        try {
            Key key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(hex(salt)));
            return new String(c.doFinal(Base64.decodeBase64(toDecrypt)));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException n) {
            throw new RuntimeException(n);
        }
    }

    private static byte[] hex(String str) {
        try {
            return Hex.decodeHex(str.toCharArray());
        } catch (DecoderException e) {
            throw new IllegalStateException(e);
        }
    }
}
