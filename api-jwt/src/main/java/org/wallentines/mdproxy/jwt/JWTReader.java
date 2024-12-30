package org.wallentines.mdproxy.jwt;

import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdcfg.serializer.SerializeResult;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class JWTReader {

    public static SerializeResult<JWT> readAny(String s, KeySupplier keySupplier) {

        int firstPeriod = s.indexOf('.');
        if(firstPeriod == -1) {
            return SerializeResult.failure("Found malformed JWT!");
        }

        Base64.Decoder decoder = Base64.getUrlDecoder();
        JSONCodec json = JSONCodec.minified();

        String headerStr = s.substring(0, s.indexOf('.'));

        ConfigSection header;
        try {
            header = json.decode(ConfigContext.INSTANCE, new ByteArrayInputStream(decoder.decode(headerStr))).asSection();
        } catch (Exception ex) {
            return SerializeResult.failure("Unable to read JWT header!", ex);
        }

        // JWE
        if(header.hasString("enc")) {
            return JWESerializer.read(s, keySupplier);
        }
        // JWS
        else {
            return JWSSerializer.read(s, keySupplier);
        }
    }

}
