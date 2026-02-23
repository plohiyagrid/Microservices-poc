package plohiya.notification_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryUpdatedEvent {
    private String skuCode;
    private Integer quantity;
    private Integer oldQuantity;
    private String eventType;
}