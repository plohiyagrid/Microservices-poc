package plohiya.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReservationResponse {
    private Map<String, Integer> updatedQuantities; // SKU -> new quantity after reservation
}