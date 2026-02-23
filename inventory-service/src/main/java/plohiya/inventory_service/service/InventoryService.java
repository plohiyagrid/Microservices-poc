package plohiya.inventory_service.service;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plohiya.inventory_service.dto.InventoryRequest;
import plohiya.inventory_service.dto.InventoryResponse;
import plohiya.inventory_service.event.InventoryUpdatedEvent;
import plohiya.inventory_service.exception.InventoryNotFoundException;
import plohiya.inventory_service.model.Inventory;
import plohiya.inventory_service.repository.InventoryRepository;

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
                    boolean inStock = inventory != null && inventory.getQuantity() > 0;
                    return InventoryResponse.builder()
                            .skuCode(skuCode)
                            .inStock(inStock)
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
            throw new RuntimeException("Failed to send inventory updated event to Kafka", e);
        }
        return InventoryResponse.builder()
                .skuCode(savedInventory.getSkuCode())
                .inStock(savedInventory.getQuantity() > 0)
                .build();
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory with id " + id + " not found"));
        
        return InventoryResponse.builder()
                .skuCode(inventory.getSkuCode())
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
    public void clearInventory() {
        log.info("Clearing all inventory records");
        inventoryRepository.deleteAll();
    }
}
