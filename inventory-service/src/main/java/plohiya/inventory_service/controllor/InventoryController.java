package plohiya.inventory_service.controllor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import plohiya.inventory_service.dto.InventoryRequest;
import plohiya.inventory_service.dto.InventoryResponse;
import plohiya.inventory_service.service.InventoryService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory")
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    // http://localhost:8082/api/inventory?skuCode=iphone-13&skuCode=iphone13-red
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> isInStock(@RequestParam List<String> skuCode) {
        log.info("Received inventory check request for skuCode: {}", skuCode);
        return inventoryService.isInStock(skuCode);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse addInventory(@RequestBody InventoryRequest inventoryRequest) {
        log.info("Received request to add inventory: {}", inventoryRequest);
        return inventoryService.addInventory(inventoryRequest);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearInventory() {
        log.info("Received request to clear inventory");
        inventoryService.clearInventory();
    }

}
