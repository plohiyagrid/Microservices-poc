package plohiya.inventory_service.service;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plohiya.inventory_service.dto.InventoryRequest;
import plohiya.inventory_service.dto.InventoryReservationResponse;
import plohiya.inventory_service.dto.InventoryResponse;
import plohiya.inventory_service.event.InventoryUpdatedEvent;
import plohiya.inventory_service.exception.InventoryNotFoundException;
import plohiya.inventory_service.model.Inventory;
import plohiya.inventory_service.repository.InventoryRepository;
import plohiya.order_service.exception.ProductOutOfStockException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, InventoryUpdatedEvent> kafkaTemplate;

    @Transactional(readOnly = true)
    @SneakyThrows
    public List<InventoryResponse> isInStock(List<String> skuCodes) {
        log.info("Checking Inventory for SKU codes: {}", skuCodes);

        List<Inventory> inventories = inventoryRepository.findBySkuCodeIn(skuCodes);
        
        Map<String, Inventory> skuMap = inventories.stream()
                .collect(Collectors.toMap(
                        Inventory::getSkuCode,
                        i -> i
                ));
                
        return skuCodes.stream()
                .map(skuCode -> {
                    Inventory inventory = skuMap.get(skuCode);
                    int quantity = inventory != null ? inventory.getQuantity() : 0;
                    return InventoryResponse.builder()
                            .skuCode(skuCode)
                            .quantity(quantity)
                            .inStock(quantity > 0)
                            .build();
                })
                .toList();
    }

    @Transactional
    public InventoryResponse addInventory(InventoryRequest inventoryRequest) {
        log.info("Adding inventory for SKU code: {} with quantity: {}", 
                inventoryRequest.getSkuCode(), inventoryRequest.getQuantity());

        Inventory inventory = inventoryRepository.findBySkuCode(inventoryRequest.getSkuCode())
                .orElse(null);

        InventoryUpdatedEvent event;
        int oldQuantity = 0;

        if (inventory != null) {
            oldQuantity = inventory.getQuantity();
            int newQuantity = oldQuantity + inventoryRequest.getQuantity();
            inventory.setQuantity(newQuantity);
            log.info("Inventory with SKU code {} already exists. Incrementing quantity from {} to {}", 
                    inventoryRequest.getSkuCode(), oldQuantity, newQuantity);
            event = new InventoryUpdatedEvent(
                inventoryRequest.getSkuCode(), 
                newQuantity, 
                oldQuantity, 
                "UPDATED"
            );
        }   else {
            inventory = new Inventory();
            inventory.setSkuCode(inventoryRequest.getSkuCode());
            inventory.setQuantity(inventoryRequest.getQuantity());
            log.info("Inventory with SKU code {} does not exist. Creating new inventory with quantity: {}", 
                    inventoryRequest.getSkuCode(), inventoryRequest.getQuantity());
            event = new InventoryUpdatedEvent(
                inventoryRequest.getSkuCode(), 
                inventoryRequest.getQuantity(), 
                null, 
                "CREATED"
            );
        }
        Inventory savedInventory = inventoryRepository.save(inventory);

        try {
            kafkaTemplate.send("inventoryTopic", event);
        } catch (Exception e) {
            log.error("Failed to send inventory updated event to Kafka: {}", e.getMessage());
        }
        return InventoryResponse.builder()
                .skuCode(savedInventory.getSkuCode())
                .quantity(savedInventory.getQuantity())
                .inStock(savedInventory.getQuantity() > 0)
                .build();
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory with id " + id + " not found"));
        
        return InventoryResponse.builder()
                .skuCode(inventory.getSkuCode())
                .quantity(inventory.getQuantity())
                .inStock(inventory.getQuantity() > 0)
                .build();
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryBySkuCode(String skuCode) {
        Inventory inventory = inventoryRepository.findBySkuCodeIn(List.of(skuCode))
                .stream()
                .findFirst()
                .orElseThrow(() -> new InventoryNotFoundException("Inventory with SKU code " + skuCode + " not found"));
        
        return InventoryResponse.builder()
                .skuCode(inventory.getSkuCode())
                .quantity(inventory.getQuantity())
                .inStock(inventory.getQuantity() > 0)
                .build();
    }

    @Transactional
    public void deleteInventoryBySkuCode(String skuCode) {
        if (skuCode == null || skuCode.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU code cannot be null or empty");
        }
        
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory with SKU code " + skuCode + " not found"));
        
        log.info("Deleting {} inventory record with SKU code: {}", skuCode , inventory.getQuantity());
        inventoryRepository.deleteBySkuCode(skuCode);
        log.info("Successfully deleted inventory with SKU code: {}", skuCode);
    }

    @Transactional
    public InventoryReservationResponse reserveInventory(Map<String, Integer> skuQuantityMap) {
        log.info("Reserving inventory for SKU quantities: {}", skuQuantityMap);

        Map<String, Inventory> inventoryMap = validateAndLoadInventories(skuQuantityMap);
        Map<String, Integer> updatedQuantities = reduceInventories(inventoryMap, skuQuantityMap);
        
        log.info("Successfully reserved inventory for all SKUs");
        return InventoryReservationResponse.builder()
                .updatedQuantities(updatedQuantities)
                .build();
    }
    
    @Transactional(readOnly = true)
    private Map<String, Inventory> validateAndLoadInventories(Map<String, Integer> skuQuantityMap) {
        Map<String, Inventory> inventoryMap = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : skuQuantityMap.entrySet()) {
            String skuCode = entry.getKey();
            Integer quantityToReserve = entry.getValue();
            
            Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                    .orElseThrow(() -> new InventoryNotFoundException(
                            "Inventory with SKU code " + skuCode + " not found"));
            
            int currentQuantity = inventory.getQuantity();
            
            if (currentQuantity < quantityToReserve) {
                throw new ProductOutOfStockException(
                        String.format("Insufficient inventory for SKU: %s. Available: %d, Required: %d", 
                                skuCode, currentQuantity, quantityToReserve));
            }
            
            inventoryMap.put(skuCode, inventory);
        }
        
        return inventoryMap;
    }
    
    private Map<String, Integer> reduceInventories(Map<String, Inventory> inventoryMap, 
                                                   Map<String, Integer> skuQuantityMap) {
        Map<String, Integer> updatedQuantities = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : skuQuantityMap.entrySet()) {
            String skuCode = entry.getKey();
            Integer quantityToReserve = entry.getValue();
            
            Inventory inventory = inventoryMap.get(skuCode);
            int oldQuantity = inventory.getQuantity();
            int newQuantity = oldQuantity - quantityToReserve;
            
            inventory.setQuantity(newQuantity);
            inventoryRepository.save(inventory);
            
            updatedQuantities.put(skuCode, newQuantity);
            
            log.info("Reserved inventory for SKU: {}. Old: {}, Reserved: {}, New: {}", 
                    skuCode, oldQuantity, quantityToReserve, newQuantity);
            
            publishInventoryUpdateEvent(skuCode, newQuantity, oldQuantity, "RESERVED");
        }
        
        return updatedQuantities;
    }
    
    private void publishInventoryUpdateEvent(String skuCode, int newQuantity, int oldQuantity, String eventType) {
        try {
            InventoryUpdatedEvent event = new InventoryUpdatedEvent(
                    skuCode, newQuantity, oldQuantity, eventType);
            kafkaTemplate.send("inventoryTopic", event);
        } catch (Exception e) {
            log.error("Failed to publish inventory event for SKU {}: {}", skuCode, e.getMessage());
        }
    }

    @Transactional
    public void clearInventory() {
        log.info("Clearing all inventory records");
        inventoryRepository.deleteAll();
    }
}
