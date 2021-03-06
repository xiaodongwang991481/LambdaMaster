package com.example.xiaodongwang.lambdamaster;

import com.rabbitmq.client.*;

import java.io.IOException;

public interface RoutingKeyHandler {
    public void handleDelivery(
            Channel channel, String consumerTag, Envelope envelope,
            AMQP.BasicProperties properties, byte[] body
    ) throws IOException;
}
