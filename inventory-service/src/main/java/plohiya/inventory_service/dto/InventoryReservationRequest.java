package plohiya.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReservationRequest {
    private Map<String, Integer> skuQuantityMap; // SKU -> quantity to reserve/reduce
}