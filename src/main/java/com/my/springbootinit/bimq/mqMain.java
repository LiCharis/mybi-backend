package com.my.springbootinit.bimq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author 黎海旭
 **/

/**
 * 用于创建测试用的交换机和队列，只需要执行一次
 */
public class mqMain {
    public static void main(String[] args) {
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setHost("localhost");
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();
            //创建一个交换机
            String EXCHANGE_NAME = "bi_exchange";
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            //创建消息队列
            String QUEUE_NAME = "bi_queue";
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "my_routeKey");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
