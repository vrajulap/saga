package com.delivery.ms.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DeliveryEvent {

	private String type;

	private CustomerOrder order;

}
