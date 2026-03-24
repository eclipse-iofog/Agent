/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.utils;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public class JwtManager {
    private static final String MODULE_NAME = "JWT Manager";
    private static final int JWT_EXPIRATION = 10 * 60 * 1000; // 10 minutes
    private static Ed25519Signer signer;
    private static OctetKeyPair keyPair;

    /**
     * Resets the JWT Manager static state to allow re-initialization with new credentials
     */
    public static void reset() {
        LoggingService.logDebug(MODULE_NAME, "Resetting JWT Manager static state");
        signer = null;
        keyPair = null;
        LoggingService.logDebug(MODULE_NAME, "JWT Manager static state reset completed");
    }

    public static String generateJwt() {
        try {
            // Get and validate private key
            String base64Key = Configuration.getPrivateKey();
            if (base64Key == null || base64Key.isEmpty()) {
                LoggingService.logError(MODULE_NAME, "Private key is not configured", new Exception("Private key is not configured"));
                return null;
            }

            // Initialize signer if not already done
            if (signer == null || keyPair == null) {
                try {
                    // Parse the base64-encoded JWK
                    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
                    String jwkJson = new String(keyBytes);
                    // LoggingService.logDebug(MODULE_NAME, "Parsing JWK: " + jwkJson);
                    
                    // Parse and validate the JWK
                    keyPair = OctetKeyPair.parse(jwkJson);
                    if (!"OKP".equals(keyPair.getKeyType().getValue())) {
                        LoggingService.logError(MODULE_NAME, "Invalid key type", new Exception("Key must be OKP type"));
                        return null;
                    }
                    if (!"Ed25519".equals(keyPair.getCurve().getName())) {
                        LoggingService.logError(MODULE_NAME, "Invalid curve", new Exception("Key must use Ed25519 curve"));
                        return null;
                    }
                    
                    // Generate a key ID if one isn't provided
                    if (keyPair.getKeyID() == null || keyPair.getKeyID().isEmpty()) {
                        String generatedKid = UUID.randomUUID().toString();
                        keyPair = new OctetKeyPair.Builder(keyPair)
                                .keyID(generatedKid)
                                .build();
                        LoggingService.logDebug(MODULE_NAME, "Generated key ID: " + generatedKid);
                    }
                    
                    signer = new Ed25519Signer(keyPair);
                    LoggingService.logDebug(MODULE_NAME, "Successfully initialized Ed25519 signer with key ID: " + keyPair.getKeyID());
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Failed to initialize signer: " + e.getMessage(), e);
                    return null;
                }
            }

            // Create JWT claims
            String uuid = Configuration.getIofogUuid();
            if (uuid == null || uuid.isEmpty()) {
                LoggingService.logError(MODULE_NAME, "UUID is not configured", new Exception("UUID is not configured"));
                return null;
            }

            // Create JWT with required claims
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(uuid)
                    .issuer("iofog-agent")
                    .expirationTime(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                    .issueTime(new Date())
                    .jwtID(UUID.randomUUID().toString()) // Add unique JWT ID
                    .claim("kid", keyPair.getKeyID()) // Add key ID as a claim
                    .build();

            // Create JWS header with EdDSA algorithm and key ID
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                    .keyID(keyPair.getKeyID())
                    .build();

            // Create and sign JWT
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(signer);

            String jwt = signedJWT.serialize();
            LoggingService.logDebug(MODULE_NAME, "Generated JWT with key ID: " + keyPair.getKeyID());
            return jwt;
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Failed to generate JWT: " + e.getMessage(), e);
            return null;
        }
    }
} 