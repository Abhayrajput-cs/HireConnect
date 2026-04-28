package com.hireconnect.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class AmqpConfig {

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
    Exchange notificationExchange(MessagingProperties properties) {
        return ExchangeBuilder.topicExchange(properties.exchange()).durable(true).build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
    Queue notificationQueue(MessagingProperties properties) {
        return QueueBuilder.durable(properties.queue()).build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
    Binding notificationBinding(Queue notificationQueue, Exchange notificationExchange, MessagingProperties properties) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(properties.routingKey()).noargs();
    }
}
