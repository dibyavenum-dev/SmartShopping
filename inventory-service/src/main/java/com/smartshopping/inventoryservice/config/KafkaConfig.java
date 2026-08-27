package com.smartshopping.inventoryservice.config;

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

import com.smartshopping.inventoryservice.event.OrderPaymentFailedEvent;

@Configuration
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, OrderPaymentFailedEvent>
    orderPaymentFailedConsumerFactory() {

        Map<String, Object> props = new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092");

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "inventory-payment-failed-group");

        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest");

        JsonDeserializer<OrderPaymentFailedEvent> deserializer =
                new JsonDeserializer<>(
                        OrderPaymentFailedEvent.class);

        deserializer.addTrustedPackages(
                "com.smartshopping.inventoryservice.event");

        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<
            String, OrderPaymentFailedEvent>
    orderPaymentFailedKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<
                String, OrderPaymentFailedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                orderPaymentFailedConsumerFactory());

        return factory;
    }
}