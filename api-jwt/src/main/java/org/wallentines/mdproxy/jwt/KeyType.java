package org.wallentines.mdproxy.jwt;

import org.wallentines.mdcfg.serializer.SerializeResult;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public interface KeyType<T> {

    SerializeResult<T> create(byte[] bytes);

    SerializeResult<byte[]> serialize(T key);

    KeyType<PrivateKey> RSA_PRIVATE = new KeyType<>() {
        @Override
        public SerializeResult<PrivateKey> create(byte[] bytes) {
            try {
                return SerializeResult.success(KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes, "RSA")));
            } catch (GeneralSecurityException ex) {
                return SerializeResult.failure("Unable to read RSA private key!");
            }
        }
        @Override
        public SerializeResult<byte[]> serialize(PrivateKey key) {
            if(key.getAlgorithm().equals("RSA")) {
                return SerializeResult.success(key.getEncoded());
            }
            return SerializeResult.failure("Expected an RSA Key!");
        }
    };

    KeyType<PublicKey> RSA_PUBLIC = new KeyType<>() {
        @Override
        public SerializeResult<PublicKey> create(byte[] bytes) {
            try {
                return SerializeResult.success(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes, "RSA")));
            } catch (GeneralSecurityException ex) {
                return SerializeResult.failure("Unable to read RSA private key!");
            }
        }
        @Override
        public SerializeResult<byte[]> serialize(PublicKey key) {
            if(key.getAlgorithm().equals("RSA")) {
                return SerializeResult.success(key.getEncoded());
            }
            return SerializeResult.failure("Expected an RSA Key!");
        }
    };


    Secret AES = new Secret("AES");
    Raw HMAC = new Raw();

    class Raw implements KeyType<byte[]> {
        @Override
        public SerializeResult<byte[]> create(byte[] bytes) {
            return SerializeResult.success(bytes);
        }
        @Override
        public SerializeResult<byte[]> serialize(byte[] key) {
            return SerializeResult.success(key);
        }
    }

    class Secret implements KeyType<SecretKey> {
        private final String algorithm;

        public Secret(String algorithm) {
            this.algorithm = algorithm;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        @Override
        public SerializeResult<SecretKey> create(byte[] bytes) {
            return SerializeResult.success(new SecretKeySpec(bytes, algorithm));
        }
        @Override
        public SerializeResult<byte[]> serialize(SecretKey key) {
            if(key.getAlgorithm().equals(algorithm)) {
                return SerializeResult.success(key.getEncoded());
            }
            return SerializeResult.failure("Expected secret key with algorithm " + key.getAlgorithm());
        }
    }

}
