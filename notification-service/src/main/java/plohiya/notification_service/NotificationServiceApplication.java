package plohiya.notification_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import plohiya.notification_service.event.InventoryUpdatedEvent;
import plohiya.notification_service.event.OrderPlacedEvent;

@SpringBootApplication
@Slf4j
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}

    @KafkaListener(topics = "notificationTopic")
    public void handleNotification(OrderPlacedEvent orderPlacedEvent){
        log.info("Received Notification for Order No. {} ", orderPlacedEvent.getOrderNumber());
    }

    @KafkaListener(topics = "inventoryTopic", groupId = "notification-service-inventory")
    public void handleInventoryUpdate(InventoryUpdatedEvent inventoryUpdatedEvent){
        log.info("Received Notification for Inventory Update - SKU Code: {}, Quantity: {}, Event Type: {}", 
                inventoryUpdatedEvent.getSkuCode(), 
                inventoryUpdatedEvent.getQuantity(),
                inventoryUpdatedEvent.getEventType());
    }

}
