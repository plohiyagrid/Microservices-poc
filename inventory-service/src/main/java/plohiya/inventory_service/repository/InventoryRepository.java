package plohiya.inventory_service.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import plohiya.inventory_service.model.Inventory;

import java.util.List;
import java.util.Optional;


public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findBySkuCodeIn(List<String> skuCode);
    
    @Query("SELECT i FROM Inventory i WHERE i.skuCode = :skuCode")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findBySkuCode(@Param("skuCode") String skuCode);

    void deleteBySkuCode(String skuCode);

    List<Inventory> findAllBySkuCode(String skuCode);
}
