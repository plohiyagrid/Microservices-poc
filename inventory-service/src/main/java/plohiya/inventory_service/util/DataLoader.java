package plohiya.inventory_service.util;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import plohiya.inventory_service.model.Inventory;
import plohiya.inventory_service.repository.InventoryRepository;

// @Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final InventoryRepository inventoryRepository;
    @Override
    public void run(String... args) throws Exception {
        Inventory inventory = new Inventory();
        inventory.setSkuCode("iphone_13");
        inventory.setQuantity(100);

        Inventory inventory1 = new Inventory();
        inventory1.setSkuCode("iphone_13_red");
        inventory1.setQuantity(0);

        Inventory inventory2 = new Inventory();
        inventory2.setSkuCode("iphone_16");
        inventory2.setQuantity(100);

        inventoryRepository.save(inventory);
        inventoryRepository.save(inventory1);
        inventoryRepository.save(inventory2);

    }
}