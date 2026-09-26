package com.toollix.common.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final byte[] secret;
    private final long accessTokenValiditySeconds;

    public JwtTokenProvider(@Value("${security.jwt.secret:change-me-in-prod}") String secret,
            @Value("${security.jwt.access-token-seconds:900}") long accessTokenValiditySeconds) {
        this.secret = secret.getBytes();
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public String createAccessToken(String subject, String tokenVersion, String email) {
        try {
            JWSSigner signer = new MACSigner(secret);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer("toollix")
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusSeconds(accessTokenValiditySeconds)))
                    .claim("tv", tokenVersion)
                    .claim("email", email)
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT signedJWT = new SignedJWT(new com.nimbusds.jose.JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to create JWT", e);
        }
    }

    public SignedJWT parse(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            // verify signature
            if (!jwt.verify(new com.nimbusds.jose.crypto.MACVerifier(secret))) {
                return null;
            }
            return jwt;
        } catch (Exception e) {
            return null;
        }
    }
}
