package com.payflow.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payflow.domain.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, String> {

}
