package kware.app.clsel;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

public class RabbitMQYmlProducer {

    private final static String QUEUE_NAME = "your-queue-name";
    private final static String HOST = "localhost"; // RabbitMQ 서버 호스트

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);

        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            // 큐 선언
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);

            // 데이터 생성
            String data = "Your YAML data here..."; // YAML 데이터를 생성

            // 데이터를 큐로 보내기
            channel.basicPublish("", QUEUE_NAME, null, data.getBytes());
            System.out.println("Sent: " + data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
