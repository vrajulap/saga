package com.payment.ms.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Payment {

	@Id
	@GeneratedValue
	private Long id;

	@Column
	private String mode;

	@Column
	private Long orderId;

	@Column
	private double amount;

	@Column
	private String status;

}
