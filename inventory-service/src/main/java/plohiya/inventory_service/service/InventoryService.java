package plohiya.inventory_service.service;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plohiya.inventory_service.dto.InventoryRequest;
import plohiya.inventory_service.dto.InventoryResponse;
import plohiya.inventory_service.exception.InventoryNotFoundException;
import plohiya.inventory_service.model.Inventory;
import plohiya.inventory_service.repository.InventoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    @SneakyThrows
    public List<InventoryResponse> isInStock(List<String> skuCode) {
        log.info("Checking Inventory");
        return inventoryRepository.findBySkuCodeIn(skuCode).stream()
                .map(inventory ->
                        InventoryResponse.builder()
                                .skuCode(inventory.getSkuCode())
                                .inStock(inventory.getQuantity() > 0)
                                .build()
                ).toList();
    }

    @Transactional
    public InventoryResponse addInventory(InventoryRequest inventoryRequest) {
        log.info("Adding inventory for SKU code: {} with quantity: {}", 
                inventoryRequest.getSkuCode(), inventoryRequest.getQuantity());

        Inventory inventory = inventoryRepository.findBySkuCode(inventoryRequest.getSkuCode())
                .orElse(null);
        if (inventory != null) {
            int oldQuantity = inventory.getQuantity();
            int newQuantity = inventory.getQuantity() + inventoryRequest.getQuantity();
            inventory.setQuantity(newQuantity);
            log.info("Inventory with SKU code {} already exists. Incrementing quantity from {} to {}", 
                    inventoryRequest.getSkuCode(), oldQuantity, newQuantity);
        }   else {
            inventory = new Inventory();
            inventory.setSkuCode(inventoryRequest.getSkuCode());
            inventory.setQuantity(inventoryRequest.getQuantity());
            log.info("Inventory with SKU code {} does not exist. Creating new inventory with quantity: {}", 
                    inventoryRequest.getSkuCode(), inventoryRequest.getQuantity());
        }
        Inventory savedInventory = inventoryRepository.save(inventory);
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
        
        List<Inventory> inventories = inventoryRepository.findAllBySkuCode(skuCode);
        
        if (inventories.isEmpty()) {
            throw new InventoryNotFoundException("Inventory with SKU code " + skuCode + " not found");
        }
        
        log.info("Deleting {} inventory record(s) with SKU code: {}", inventories.size(), skuCode);
        inventoryRepository.deleteBySkuCode(skuCode);
        log.info("Successfully deleted inventory with SKU code: {}", skuCode);
    }

    @Transactional
    public void clearInventory() {
        log.info("Clearing all inventory records");
        inventoryRepository.deleteAll();
    }
}
