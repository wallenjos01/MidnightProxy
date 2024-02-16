package org.wallentines.mdproxy.jwt;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.DecodeException;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdcfg.serializer.SerializeResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class JWESerializer {

    private final KeyCodec<?, ?> keyCodec;
    private final CryptCodec<?> contentCodec;

    public JWESerializer(KeyCodec<?, ?> keyCodec, CryptCodec<?> cryptCodec) {
        this.keyCodec = keyCodec;
        this.contentCodec = cryptCodec;
    }

    public @NotNull SerializeResult<String> writeString(JWT jwt) {

        if(!keyCodec.canEncode()) {
            return SerializeResult.failure("Unable to encode JWE key with this codec!");
        }

        StringBuilder out = new StringBuilder();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        // Header
        String headerB64;
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            JSONCodec.minified().encode(ConfigContext.INSTANCE, jwt.header()
                    .with("alg", KeyCodec.Algorithm.REGISTRY.getId(keyCodec.getAlgorithm()))
                    .with("enc", CryptCodec.Algorithm.REGISTRY.getId(contentCodec.getAlgorithm())),
                    bos,
                    StandardCharsets.UTF_8
            );
            headerB64 = encoder.encodeToString(bos.toByteArray());
            out.append(headerB64);
        } catch(IOException ex) {
            return SerializeResult.failure("An error occurred while writing a JWE header!");
        }

        // Encrypted Key
        out.append(".").append(encoder.encodeToString(keyCodec.encode(contentCodec.getEncodedKey())));

        // Initialization Vector
        byte[] iv = contentCodec.getIV();
        out.append(".").append(encoder.encodeToString(iv));

        // Payload
        CryptCodec.CryptOutput output;
        byte[] payload;
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            JSONCodec.minified().encode(ConfigContext.INSTANCE, jwt.payload(), bos);
            payload = bos.toByteArray();

        } catch(IOException ex) {
            return SerializeResult.failure("An error occurred while writing JWE ciphertext!");
        }
        output = contentCodec.encrypt(payload, headerB64.getBytes(StandardCharsets.US_ASCII));

        out.append(".").append(encoder.encodeToString(output.cipherText()));

        // Authentication Tag
        out.append(".").append(encoder.encodeToString(output.authTag()));

        return SerializeResult.success(out.toString());
    }


    public static SerializeResult<JWT> read(String jwe, KeyCodec<?, ?> codec) {
        return read(jwe, KeySupplier.of(codec));
    }

    public static SerializeResult<JWT> read(String jwe, KeySupplier supp) {

        // Split into parts
        String[] values = jwe.split("\\.", 5);
        if(values.length != 5) {
            return SerializeResult.failure("JWE is malformed! Expected 5 parts!");
        }

        Base64.Decoder decoder = Base64.getUrlDecoder();
        JSONCodec json = JSONCodec.minified();

        // Read Header
        ConfigSection header;
        try {
            header = json.decode(ConfigContext.INSTANCE, new ByteArrayInputStream(decoder.decode(values[0])), StandardCharsets.UTF_8).asSection();
            if (!header.hasString("enc")) {
                return SerializeResult.failure("Expected header parameter with name enc!");
            }
            if (!header.hasString("alg")) {
                return SerializeResult.failure("Expected header parameter with name alg!");
            }

        } catch (IOException | DecodeException ex) {
            return SerializeResult.failure("An exception occurred while reading a JWE header!" + ex.getMessage());
        }

        // Find relevant algorithms
        KeyCodec.Algorithm<?,?> keyAlg = KeyCodec.Algorithm.REGISTRY.get(header.getString("alg"));
        if(keyAlg == null) {
            return SerializeResult.failure("Key encryption algorithm " + header.getString("alg") + " not found!");
        }

        CryptCodec.Algorithm<?> cryptAlg = CryptCodec.Algorithm.REGISTRY.get(header.getString("enc"));
        if(cryptAlg == null) {
            return SerializeResult.failure("Encryption algorithm " + header.getString("enc") + " not found!");
        }

        // Decode IV
        byte[] iv = decoder.decode(values[2]);

        // Find the CEK
        KeyCodec<?,?> codec;
        CryptCodec<?> crypt;
        if(keyAlg == KeyCodec.Algorithm.DIRECT) {
            codec = KeyCodec.direct();
            crypt = cryptAlg.createCodec(header, supp, iv);
        } else {
            codec = keyAlg.createCodec(header, supp);
            crypt = cryptAlg.createCodec(codec.decode(decoder.decode(values[1])), iv);
        }

        // Decode other parts
        byte[] cipherText = decoder.decode(values[3]);
        byte[] auth = decoder.decode(values[4]);

        // Decrypt the payload
        byte[] payloadBytes = crypt.decrypt(cipherText);

        // Verify authentication tag
        byte[] newAuth = crypt.encrypt(payloadBytes, values[0].getBytes(StandardCharsets.US_ASCII)).authTag();

        if(!Arrays.equals(newAuth, auth)) {
            return SerializeResult.failure("The JWE authentication tag could not be verified!");
        }

        // Assemble the payload
        ConfigSection payload;
        try {
            payload = json.decode(ConfigContext.INSTANCE, new ByteArrayInputStream(payloadBytes), StandardCharsets.UTF_8).asSection();
        } catch (IOException ex) {
            return SerializeResult.failure("Unable to parse decrypted payload!");
        }

        return SerializeResult.success(new JWE(codec, crypt, header, payload));
    }

    public record JWE(KeyCodec<?,?> decodeKeyCodec, CryptCodec<?> cryptCodec, ConfigSection header, ConfigSection payload) implements JWT {

        public SerializeResult<String> asString() {
            return asString(decodeKeyCodec);
        }

        public SerializeResult<String> asString(KeyCodec<?,?> encryptionCodec) {
            if(!encryptionCodec.canEncode()) {
                return SerializeResult.failure("Unable to encode JWE with this codec!");
            }
            return new JWESerializer(encryptionCodec, cryptCodec).writeString(this);
        }

        @Override
        public boolean isEncrypted() {
            return true;
        }

        @Override
        public boolean isUnprotected() {
            return false;
        }
    }



}
