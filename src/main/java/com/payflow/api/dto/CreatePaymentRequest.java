package com.payflow.api.dto;

import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CreatePaymentRequest {
    @Min(value = 1, message = "Amount must be greater than 0")
    private long amountCents;
    
    @NotBlank
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter ISO 4217 code")
    private String currency;
    
    @NotBlank
    @Size(max = 64)
    private String merchantId;
    
    @NotBlank
    @Size(max = 64)
    private String customerId;
    

    private Map<String, Object> metadata;
}
