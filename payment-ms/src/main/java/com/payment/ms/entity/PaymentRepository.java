package com.payment.ms.entity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	public List<Payment> findByOrderId(long orderId);
}
