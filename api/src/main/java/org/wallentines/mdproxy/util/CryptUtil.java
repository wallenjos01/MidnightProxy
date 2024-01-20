package org.wallentines.mdproxy.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;

public class CryptUtil {

    public static byte[] encryptData(Key key, byte[] data) {

        try {
            return getCipher(Cipher.ENCRYPT_MODE, key).doFinal(data);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to encrypt data!", ex);
        }
    }

    public static byte[] decryptData(Key key, byte[] data) {

        try {
            return getCipher(Cipher.DECRYPT_MODE, key).doFinal(data);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to encrypt data!", ex);
        }
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create key pair!", ex);
        }
    }

    public static Cipher getCipher(int mode, Key key) {
        try {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(mode, key);
            return cipher;
        }  catch (Exception ex) {

            throw new IllegalStateException("Unable to create cipher!", ex);
        }
    }

    public static byte[] hashData(byte[]... data) {

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            for (byte[] cs : data) {
                messageDigest.update(cs);
            }
            return messageDigest.digest();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash data!", ex);
        }
    }

}
