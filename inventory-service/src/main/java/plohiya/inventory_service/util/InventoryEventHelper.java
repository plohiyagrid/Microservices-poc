package plohiya.inventory_service.util;

import plohiya.inventory_service.event.InventoryUpdatedEvent;

public final class InventoryEventHelper {

    private InventoryEventHelper() {}

    public static InventoryUpdatedEvent createCreatedEvent(String skuCode, Integer quantity) {
        return new InventoryUpdatedEvent(skuCode, quantity, null, "CREATED");
    }

    public static InventoryUpdatedEvent createUpdatedEvent(String skuCode, Integer newQuantity, Integer oldQuantity) {
        return new InventoryUpdatedEvent(skuCode, newQuantity, oldQuantity, "UPDATED");
    }

    public static InventoryUpdatedEvent createReservedEvent(String skuCode, Integer newQuantity, Integer oldQuantity) {
        return new InventoryUpdatedEvent(skuCode, newQuantity, oldQuantity, "RESERVED");
    }
}