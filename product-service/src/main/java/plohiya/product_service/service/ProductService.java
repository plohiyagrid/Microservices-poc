package plohiya.product_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import plohiya.product_service.dto.ProductRequest;
import plohiya.product_service.dto.ProductResponse;
import plohiya.product_service.exception.InvalidProductRequestException;
import plohiya.product_service.exception.ProductNotFoundException;
import plohiya.product_service.model.Product;
import plohiya.product_service.repository.ProductRepository;

import java.security.PublicKey;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public void createProduct(ProductRequest productRequest){
        validateProductRequest(productRequest);

        Product product = Product.builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .build();

        productRepository.save(product);
        log.info("PRODUCT {} is saved" , product.getId());
    }

    private void validateProductRequest(ProductRequest productRequest) {
        if (productRequest == null) {
            throw new InvalidProductRequestException("Product request cannot be null");
        }

        if (productRequest.getName() == null || productRequest.getName().trim().isEmpty()) {
            throw new InvalidProductRequestException("Product name is required");
        }

        if (productRequest.getPrice() == null) {
            throw new InvalidProductRequestException("Product price is required");
        }

        if (productRequest.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new InvalidProductRequestException("Product price must be greater than zero");
        }
    }
    
    public List<ProductResponse> getAllProducts() {
        List<Product>products =  productRepository.findAll();
        return products.stream().map(this::mapToProductResponse).toList();
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with id " + id + " not found"));
        return mapToProductResponse(product);
    }

    private ProductResponse mapToProductResponse(Product product){
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .build();

    }
}
