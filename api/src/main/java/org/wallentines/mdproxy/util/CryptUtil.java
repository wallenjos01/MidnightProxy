package org.wallentines.mdproxy.util;

import javax.crypto.Cipher;
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


    public static byte[] hashServerId(byte[] secret, PublicKey key) {

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(secret);
            messageDigest.update(key.getEncoded());
            return messageDigest.digest();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash data!", ex);
        }
    }

}
