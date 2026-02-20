package plohiya.inventory_service.service;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plohiya.inventory_service.dto.InventoryRequest;
import plohiya.inventory_service.dto.InventoryResponse;
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
        
        Inventory inventory = new Inventory();
        inventory.setSkuCode(inventoryRequest.getSkuCode());
        inventory.setQuantity(inventoryRequest.getQuantity());
        
        Inventory savedInventory = inventoryRepository.save(inventory);
        
        return InventoryResponse.builder()
                .skuCode(savedInventory.getSkuCode())
                .inStock(savedInventory.getQuantity() > 0)
                .build();
    }

    @Transactional
    public void clearInventory() {
        log.info("Clearing all inventory records");
        inventoryRepository.deleteAll();
    }
}
