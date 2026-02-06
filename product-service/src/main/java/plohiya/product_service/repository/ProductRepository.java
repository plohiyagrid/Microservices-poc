package plohiya.product_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import plohiya.product_service.model.Product;

public interface ProductRepository extends MongoRepository<Product , String> {
}
