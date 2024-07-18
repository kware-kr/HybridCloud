package kware.app.clsel;

import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class RabbitMQYmlConsumer {

    private final static String QUEUE_NAME = "your-queue-name";
    private final static String HOST = "localhost"; // RabbitMQ 서버 호스트

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);

        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            // 큐 선언
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("Received: " + message);

                // 여기서 데이터 파싱 및 처리 로직을 추가
                parseAndProcessData(message);
            };

            // 큐에 대기하고 있는 메시지 처리
            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseAndProcessData(String message) {
    	// SnakeYAML 객체 생성
        Yaml yaml = new Yaml();

        // YAML 문자열 파싱
        Map<String, Object> data = yaml.load(message);

    }
}


