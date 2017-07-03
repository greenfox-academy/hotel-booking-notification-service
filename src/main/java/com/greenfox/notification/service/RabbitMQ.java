package com.greenfox.notification.service;

import com.greenfox.notification.model.classes.Event;
import com.greenfox.notification.model.interfaces.MessageQueue;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.sendgrid.Request;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Getter
@Setter
public class RabbitMQ implements MessageQueue {

  private Connection connection;
  private ConnectionFactory connectionFactory;
  private Channel channel;
  private Consumer consumer;
  private final Log log;
  private Request requestMail;
  private AMQP.BasicProperties.Builder props;
  private int actualDelayTime;

  @Autowired
  public RabbitMQ(Log log)
      throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException, IOException, TimeoutException {
    this.connectionFactory = new ConnectionFactory();
    this.connectionFactory.setUri(System.getenv("RABBITMQ_BIGWIG_RX_URL"));
    this.connection = connectionFactory.newConnection();
    this.log = log;
    this.requestMail = new Request();
    this.props = new AMQP.BasicProperties.Builder();
    this.actualDelayTime = Integer.valueOf(System.getenv("DELAY_TIME"));
  }

  public void consume(String request, String queue) throws Exception {
    channel = connection.createChannel();
    channel.queueDeclare(queue, false, false, false, null);
    consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope,
          AMQP.BasicProperties properties, byte[] body)
          throws IOException {
        String message = new String(body, "UTF-8");
        log.info(request, " [x] Received '" + message + "'");
      }
    };
    channel.basicConsume(queue, true, consumer);
  }

  boolean isQueueEmpty(String queue) throws IOException {
    return channel.queueDeclarePassive(queue).getMessageCount() == 0;
  }

  @Override
  public void push(String request, Object queue, Object message) {
    int count = 0;
    try {
      channel = connection.createChannel();
      Event event = new Event(message);
      channel.basicPublish((String) queue, String.valueOf(queue), props.build(),
          Event.asJsonString(event).getBytes());
      count++;
      System.out.println("publish utan " + count);
      log.info(request, " [x] Sent '" + Event.asJsonString(event) + "'");
    } catch (IOException ex) {
      log.error(request, ex.getMessage());
      try {
        while (Integer.valueOf(System.getenv("TRY_NUMBERS")) != count) {
          Map<String, Object> args = new HashMap<>();
          args.put("x-delayed-type", "direct");
          channel.exchangeDeclare((String) queue, "x-delayed-message", true, false, args);
          Map<String, Object> headers = new HashMap<>();
          headers.put("x-delay", actualDelayTime);
          props.headers(headers);
          System.out.println("basic elott" + actualDelayTime);
          channel.basicRecover(true);
          actualDelayTime *= 2;
          System.out.println("szorzas utan " + actualDelayTime);
        }
      } catch (IOException e) {
        log.error(request, e.getMessage());
      }
    } finally {
      actualDelayTime = Integer.valueOf(System.getenv("DELAY_TIME"));
    }
  }
}
