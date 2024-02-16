package org.wallentines.mdproxy.jwt;

import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigPrimitive;

import java.util.HashMap;
import java.util.Map;

public class JWTVerifier {

    private final Map<String, ConfigObject> verify;
    private boolean allowExpired;
    private boolean allowUnprotected;
    private boolean requireEncrypted;

    public JWTVerifier() {
        this.verify = new HashMap<>();
    }

    public JWTVerifier withClaim(String claim, String value) {
        verify.put(claim, new ConfigPrimitive(value));
        return this;
    }

    public JWTVerifier withClaim(String claim, Number value) {
        verify.put(claim, new ConfigPrimitive(value));
        return this;
    }

    public JWTVerifier withClaim(String claim, Boolean value) {
        verify.put(claim, new ConfigPrimitive(value));
        return this;
    }

    public JWTVerifier withClaim(String claim, ConfigObject value) {
        verify.put(claim, value);
        return this;
    }

    public JWTVerifier allowExpired() {
        this.allowExpired = true;
        return this;
    }

    public JWTVerifier allowUnprotected() {
        this.allowUnprotected = true;
        return this;
    }

    public JWTVerifier requireEncrypted() {
        this.requireEncrypted = true;
        return this;
    }

    public boolean verify(JWT jwt) {

        if(!allowExpired && (jwt.isExpired() || !jwt.isValid())) {
            return false;
        }

        if(!allowUnprotected && jwt.isUnprotected()) {
            return false;
        }

        if(requireEncrypted && !jwt.isEncrypted()) {
            return false;
        }

        for(Map.Entry<String, ConfigObject> ent : verify.entrySet()) {
            ConfigObject obj = jwt.getClaim(ent.getKey());
            if(obj == null || !obj.equals(ent.getValue())) {
                return false;
            }
        }
        return false;
    }

}
