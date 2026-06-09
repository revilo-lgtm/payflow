package com.payflow.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.payflow.api.dto.CreatePaymentRequest;

public class RequestHasher {
    private static final ObjectMapper CANONICAL_MAPPER = new ObjectMapper()
    .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    public static String hash(CreatePaymentRequest request) {
        try {
            String canonicalJson = CANONICAL_MAPPER.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to hash payment request", ex);
        }
    }
}
