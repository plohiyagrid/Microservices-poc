package plohiya.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}