package com.smartshopping.orderservice.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.smartshopping.orderservice.event.InventoryFailedEvent;
import com.smartshopping.orderservice.event.PaymentProcessedEvent;

@Configuration
public class KafkaConfig {

    private Map<String, Object> consumerProperties(
            String groupId) {

        Map<String, Object> props = new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092");

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId);

        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "latest");

        return props;
    }


    // =====================================================
    // Payment Processed Consumer
    // =====================================================

    @Bean
    public ConsumerFactory<String, PaymentProcessedEvent>
    paymentConsumerFactory() {

        JsonDeserializer<PaymentProcessedEvent> deserializer =
                new JsonDeserializer<>(
                        PaymentProcessedEvent.class);

        deserializer.addTrustedPackages(
                "com.smartshopping.paymentservice.event");

        // Use PaymentProcessedEvent.class directly
        // instead of the type header
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                consumerProperties("order-payment-group"),
                new StringDeserializer(),
                deserializer);
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<
            String, PaymentProcessedEvent>
    paymentKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<
                String, PaymentProcessedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                paymentConsumerFactory());

        return factory;
    }


    // =====================================================
    // Inventory Failed Consumer
    // =====================================================

    @Bean
    public ConsumerFactory<String, InventoryFailedEvent>
    inventoryFailedConsumerFactory() {

        JsonDeserializer<InventoryFailedEvent> deserializer =
                new JsonDeserializer<>(
                        InventoryFailedEvent.class);

        deserializer.addTrustedPackages(
                "com.smartshopping.orderservice.event");

        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                consumerProperties(
                        "order-inventory-failed-group"),
                new StringDeserializer(),
                deserializer);
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<
            String, InventoryFailedEvent>
    inventoryFailedKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<
                String, InventoryFailedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                inventoryFailedConsumerFactory());

        return factory;
    }
}