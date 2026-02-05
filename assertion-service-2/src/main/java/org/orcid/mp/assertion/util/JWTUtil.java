package org.orcid.mp.assertion.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

@Component
public class JWTUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JWTUtil.class);

    @Value("${application.jwt.signatureUrl}")
    private String jwtSignatureUrl;

    private JWSVerifier verifier;

    @PostConstruct
    private void setSignature() {
        try {
            JWKSet publicKeys = JWKSet.load(new URL(jwtSignatureUrl));
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
            LOG.error("Error signing JWT", e);
            throw new RuntimeException(e);
        }
    }
}