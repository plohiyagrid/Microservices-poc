package plohiya.inventory_service.util;

import plohiya.inventory_service.exception.InventoryNotFoundException;
import plohiya.inventory_service.exception.ProductOutOfStockException;
import plohiya.inventory_service.model.Inventory;

import java.util.Map;
import java.util.Optional;

public final class InventoryValidator {

    private InventoryValidator() {}

    public static void validateSkuCode(String skuCode) {
        if (skuCode == null || skuCode.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU code cannot be null or empty");
        }
    }

    public static Inventory validateInventoryExists(Optional<Inventory> inventory, String skuCode) {
        return inventory.orElseThrow(() ->
                new InventoryNotFoundException("Inventory with SKU code " + skuCode + " not found"));
    }

    public static void validateSufficientInventory(String skuCode, int currentQuantity, int requiredQuantity) {
        if (currentQuantity < requiredQuantity) {
            throw new ProductOutOfStockException(
                    String.format("Insufficient inventory for SKU: %s. Available: %d, Required: %d",
                            skuCode, currentQuantity, requiredQuantity));
        }
    }

    public static void validateInventoriesForReservation(
            Map<String, Inventory> inventoryMap,
            Map<String, Integer> skuQuantityMap) {

        for (Map.Entry<String, Integer> entry : skuQuantityMap.entrySet()) {
            String skuCode = entry.getKey();
            Integer quantityToReserve = entry.getValue();

            Inventory inventory = inventoryMap.get(skuCode);
            if (inventory == null) {
                throw new InventoryNotFoundException(
                        "Inventory with SKU code " + skuCode + " not found");
            }

            int currentQuantity = inventory.getQuantity();
            validateSufficientInventory(skuCode, currentQuantity, quantityToReserve);
        }
    }
}