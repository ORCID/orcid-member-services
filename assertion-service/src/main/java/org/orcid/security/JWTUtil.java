package org.orcid.security;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

import javax.annotation.PostConstruct;

import org.orcid.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;

@Component
public class JWTUtil {
    
    private static final Logger LOG = LoggerFactory.getLogger(JWTUtil.class);
    
    @Autowired
    private ApplicationProperties applicationProperties;

    private JWSVerifier verifier;

    @PostConstruct
    private void setSignature() {
        try {
            JWKSet publicKeys = JWKSet.load(new URL(applicationProperties.getJwtSignatureUrl()));
            RSAKey rsaJWK = RSAKey.parse(publicKeys.getKeys().get(0).toJSONObject());
            RSAKey rsaPublicJWK = rsaJWK.toPublicJWK();
            this.verifier = new RSASSAVerifier(rsaPublicJWK);
        } catch (IOException | ParseException | JOSEException e) {
            LOG.warn(
                    "Error loading public keys from registry. If you are running in development mode without integrating with the ORCID registry you can ignore this message",
                    e);
        }
    }

    public SignedJWT getSignedJWT(String jwt) {
        try {
            SignedJWT s = SignedJWT.parse(jwt);
            if (s.verify(verifier)) {
                return s;
            }
            throw new IllegalArgumentException("The provided JWT is not signed");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
