package com.payflow.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payflow.domain.model.IdempotencyKey;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    
}
