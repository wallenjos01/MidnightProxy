package org.wallentines.mdproxy.jwt;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdcfg.serializer.SerializeResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

public class JWSSerializer {

    private final HashCodec<?> signCodec;

    public JWSSerializer(HashCodec<?> signCodec) {
        this.signCodec = signCodec;
    }


    public @NotNull SerializeResult<String> writeString(JWT jwt) {

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        JSONCodec json = JSONCodec.minified();

        StringBuilder out = new StringBuilder();

        try(ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            json.encode(
                    ConfigContext.INSTANCE,
                    jwt.header().with("alg", HashCodec.Algorithm.REGISTRY.getId(signCodec.getAlgorithm())),
                    bos);
            out.append(encoder.encodeToString(bos.toByteArray()));
        } catch (IOException ex) {
            return SerializeResult.failure("Unable to encode JWS header!");
        }

        out.append(".");
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            json.encode(ConfigContext.INSTANCE, jwt.payload(), bos);
            out.append(encoder.encodeToString(bos.toByteArray()));
        } catch (IOException ex) {
            return SerializeResult.failure("Unable to encode JWS payload!");
        }

        byte[] sig = signCodec.hash(out.toString().getBytes());
        out.append(".").append(encoder.encodeToString(sig));

        return SerializeResult.success(out.toString());
    }


    public static @NotNull SerializeResult<JWT> read(String jws, KeySupplier keySupplier) {

        String[] parts = jws.split("\\.", 3);

        Base64.Decoder decoder = Base64.getUrlDecoder();
        JSONCodec json = JSONCodec.minified();

        ConfigSection header;
        try {
            header = json.decode(ConfigContext.INSTANCE, new ByteArrayInputStream(decoder.decode(parts[0]))).asSection();
        } catch (Exception ex) {
            return SerializeResult.failure("An error occurred while decoding a JWS header!");
        }

        ConfigSection payload;
        try {
            payload = json.decode(ConfigContext.INSTANCE, new ByteArrayInputStream(decoder.decode(parts[1]))).asSection();
        } catch (Exception ex) {
            return SerializeResult.failure("An error occurred while decoding a JWS payload!");
        }

        String algStr = header.getString("alg");
        HashCodec.Algorithm<?> alg = HashCodec.Algorithm.REGISTRY.get(algStr);
        if(alg == null) {
            return SerializeResult.failure("No such hash algorithm named " + algStr + "exists!");
        }

        HashCodec<?> codec = alg.createCodec(header, keySupplier);

        byte[] sig = codec.hash(parts[0].getBytes(), ".".getBytes(), parts[1].getBytes());
        if(!Arrays.equals(sig, decoder.decode(parts[2]))) {
            return SerializeResult.failure("Unable to verify JWS authTag!");
        }

        return SerializeResult.success(new JWS(codec, header, payload));
    }

    public record JWS(HashCodec<?> signCodec, ConfigSection header, ConfigSection payload) implements JWT {

        @Override
        public boolean isEncrypted() {
            return false;
        }

        @Override
        public boolean isUnprotected() {
            return signCodec.getAlgorithm() == HashCodec.Algorithm.NONE;
        }

        public SerializeResult<String> asString() {
            return new JWSSerializer(signCodec).writeString(this);
        }

    }

}
