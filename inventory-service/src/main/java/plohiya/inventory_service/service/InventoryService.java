package plohiya.inventory_service.service;

import lombok.RequiredArgsConstructor;
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
import plohiya.inventory_service.util.InventoryEventHelper;
import plohiya.inventory_service.util.InventoryMapper;
import plohiya.inventory_service.util.InventoryValidator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, InventoryUpdatedEvent> kafkaTemplate;

    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> skuCodes) {
        log.info("Checking Inventory for SKU codes: {}", skuCodes);

        List<Inventory> inventories = inventoryRepository.findBySkuCodeIn(skuCodes);
        Map<String, Inventory> skuMap = InventoryMapper.buildSkuMap(inventories);

        return skuCodes.stream()
                .map(skuCode -> {
                    Inventory inventory = skuMap.get(skuCode);
                    return InventoryMapper.toInventoryResponse(skuCode,
                            inventory != null ? inventory.getQuantity() : null);
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

        if (inventory != null) {
            int oldQuantity = inventory.getQuantity();
            int newQuantity = oldQuantity + inventoryRequest.getQuantity();
            inventory.setQuantity(newQuantity);
            log.info("Inventory with SKU code {} already exists. Incrementing quantity from {} to {}",
                    inventoryRequest.getSkuCode(), oldQuantity, newQuantity);
            event = InventoryEventHelper.createUpdatedEvent(
                    inventoryRequest.getSkuCode(), newQuantity, oldQuantity);
        } else {
            inventory = new Inventory();
            inventory.setSkuCode(inventoryRequest.getSkuCode());
            inventory.setQuantity(inventoryRequest.getQuantity());
            log.info("Inventory with SKU code {} does not exist. Creating new inventory with quantity: {}",
                    inventoryRequest.getSkuCode(), inventoryRequest.getQuantity());
            event = InventoryEventHelper.createCreatedEvent(
                    inventoryRequest.getSkuCode(), inventoryRequest.getQuantity());
        }

        Inventory savedInventory = inventoryRepository.save(inventory);
        publishEvent(event);

        return InventoryMapper.toInventoryResponse(savedInventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory with id " + id + " not found"));

        return InventoryMapper.toInventoryResponse(inventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryBySkuCode(String skuCode) {
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory with SKU code " + skuCode + " not found"));

        return InventoryMapper.toInventoryResponse(inventory);
    }

    @Transactional
    public void deleteInventoryBySkuCode(String skuCode) {
        InventoryValidator.validateSkuCode(skuCode);

        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory with SKU code " + skuCode + " not found"));

        log.info("Deleting inventory record with SKU code: {} (quantity: {})", skuCode, inventory.getQuantity());
        inventoryRepository.deleteBySkuCode(skuCode);
        log.info("Successfully deleted inventory with SKU code: {}", skuCode);
    }

    @Transactional
    public InventoryReservationResponse reserveInventory(Map<String, Integer> skuQuantityMap) {
        log.info("Reserving inventory for SKU quantities: {}", skuQuantityMap);

        Map<String, Inventory> inventoryMap = loadInventories(skuQuantityMap);
        InventoryValidator.validateInventoriesForReservation(inventoryMap, skuQuantityMap);

        Map<String, Integer> updatedQuantities = reduceInventories(inventoryMap, skuQuantityMap);

        log.info("Successfully reserved inventory for all SKUs");
        return InventoryReservationResponse.builder()
                .updatedQuantities(updatedQuantities)
                .build();
    }

    private Map<String, Inventory> loadInventories(Map<String, Integer> skuQuantityMap) {
        Map<String, Inventory> inventoryMap = new HashMap<>();

        for (String skuCode : skuQuantityMap.keySet()) {
            Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                    .orElseThrow(() -> new InventoryNotFoundException(
                            "Inventory with SKU code " + skuCode + " not found"));
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

            publishEvent(InventoryEventHelper.createReservedEvent(skuCode, newQuantity, oldQuantity));
        }

        return updatedQuantities;
    }

    private void publishEvent(InventoryUpdatedEvent event) {
        try {
            kafkaTemplate.send("inventoryTopic", event);
        } catch (Exception e) {
            log.error("Failed to publish inventory event for SKU {}: {}", event.getSkuCode(), e.getMessage());
        }
    }

    @Transactional
    public void clearInventory() {
        log.info("Clearing all inventory records");
        inventoryRepository.deleteAll();
    }
}