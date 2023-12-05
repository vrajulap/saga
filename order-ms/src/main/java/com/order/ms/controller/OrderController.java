package com.order.ms.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.ms.dto.CustomerOrder;
import com.order.ms.dto.OrderEvent;
import com.order.ms.entity.Order;
import com.order.ms.entity.OrderRepository;

@RestController
@RequestMapping("/order-api")
public class OrderController {

	@Autowired
	private OrderRepository repository;

	@Autowired
	private KafkaTemplate<String, OrderEvent> kafkaOrderTemplate;
	
	private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

	@PostMapping("/order")
	public void createOrder(@RequestBody CustomerOrder customerOrder) {
		
		Order order = new Order();

		try {
			order.setAmount(customerOrder.getAmount());
			order.setItem(customerOrder.getItem());
			order.setQuantity(customerOrder.getQuantity());
			order.setStatus("CREATED");
			order = repository.save(order);

			customerOrder.setOrderId(order.getId());

			OrderEvent event = new OrderEvent();
			event.setOrder(customerOrder);
			event.setType("ORDER_CREATED");
			kafkaOrderTemplate.send("new-order", event);
		} catch (Exception e) {
			order.setStatus("FAILED");
			repository.save(order);
		}
	}
	
	
	@KafkaListener(topics = "reverse-order", groupId = "orders-group")
	public void reverseOrder(String event) {
		logger.info("Inside reverse order for order "+event);
		
		try {
			OrderEvent orderEvent = new ObjectMapper().readValue(event, OrderEvent.class);

			Optional<Order> order = repository.findById(orderEvent.getOrder().getOrderId());

			order.ifPresent(o -> {
				o.setStatus("FAILED");
				this.repository.save(o);
			});
		} catch (Exception e) {
			logger.error("Error while doing order reverse : {} " + event);
		}
	}
}
