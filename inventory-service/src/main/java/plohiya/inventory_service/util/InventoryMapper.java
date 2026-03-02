package plohiya.inventory_service.util;

import plohiya.inventory_service.dto.InventoryResponse;
import plohiya.inventory_service.model.Inventory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class InventoryMapper {

    private InventoryMapper() {}

    public static InventoryResponse toInventoryResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        return InventoryResponse.builder()
                .skuCode(inventory.getSkuCode())
                .quantity(inventory.getQuantity())
                .inStock(inventory.getQuantity() > 0)
                .build();
    }

    public static InventoryResponse toInventoryResponse(String skuCode, Integer quantity) {
        int qty = quantity != null ? quantity : 0;
        return InventoryResponse.builder()
                .skuCode(skuCode)
                .quantity(qty)
                .inStock(qty > 0)
                .build();
    }

    public static Map<String, Inventory> buildSkuMap(List<Inventory> inventories) {
        return inventories.stream()
                .collect(Collectors.toMap(
                        Inventory::getSkuCode,
                        inventory -> inventory
                ));
    }
}