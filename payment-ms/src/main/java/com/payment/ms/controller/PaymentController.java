package com.payment.ms.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.ms.dto.CustomerOrder;
import com.payment.ms.dto.OrderEvent;
import com.payment.ms.dto.PaymentEvent;
import com.payment.ms.entity.Payment;
import com.payment.ms.entity.PaymentRepository;

@Controller
public class PaymentController {

	private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

	@Autowired
	private PaymentRepository repository;

	@Autowired
	private KafkaTemplate<String, PaymentEvent> kafkaPaymentTemplate;

	@Autowired
	private KafkaTemplate<String, OrderEvent> kafkaOrderTemplate;

	@KafkaListener(topics = "new-order", groupId = "orders-group")
	public void processPayment(String event) throws JsonMappingException, JsonProcessingException {
		logger.info("Recieved event for payment " + event);
		OrderEvent orderEvent = new ObjectMapper().readValue(event, OrderEvent.class);

		CustomerOrder order = orderEvent.getOrder();
		Payment payment = new Payment();

		try {
			payment.setAmount(order.getAmount());
			payment.setMode(order.getPaymentMode());
			payment.setOrderId(order.getOrderId());
			payment.setStatus("SUCCESS");
			repository.save(payment);

			PaymentEvent paymentEvent = new PaymentEvent();
			paymentEvent.setOrder(orderEvent.getOrder());
			paymentEvent.setType("PAYMENT_CREATED");
			kafkaPaymentTemplate.send("new-payment", paymentEvent);
		} catch (Exception e) {

			logger.error("Error while doing payment : {}  " + event);

			payment.setOrderId(order.getOrderId());
			payment.setStatus("FAILED");
			repository.save(payment);

			OrderEvent oe = new OrderEvent();
			oe.setOrder(order);
			oe.setType("ORDER_REVERSE");
			kafkaOrderTemplate.send("reverse-order", orderEvent);
		}
	}

	@KafkaListener(topics = "reverse-payment", groupId = "payments-group")
	public void reversePayment(String event) {
		logger.info("Inside reverse payment for order " + event);

		try {
			PaymentEvent paymentEvent = new ObjectMapper().readValue(event, PaymentEvent.class);

			CustomerOrder order = paymentEvent.getOrder();

			Iterable<Payment> payments = this.repository.findByOrderId(order.getOrderId());

			payments.forEach(p -> {
				p.setStatus("FAILED");
				repository.save(p);
			});

			OrderEvent orderEvent = new OrderEvent();
			orderEvent.setOrder(paymentEvent.getOrder());
			orderEvent.setType("ORDER_REVERSE");
			kafkaOrderTemplate.send("reverse-order", orderEvent);
		} catch (Exception e) {
			logger.error("Error while doing payment reverse :  {}" + event);
		}
	}
}
