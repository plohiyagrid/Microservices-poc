package plohiya.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import plohiya.order_service.dto.InventoryResponse;
import plohiya.order_service.dto.OrderLineItemsDto;
import plohiya.order_service.dto.OrderRequest;
import plohiya.order_service.event.OrderPlacedEvent;
import plohiya.order_service.exception.InvalidOrderRequestException;
import plohiya.order_service.exception.OrderNotFoundException;
import plohiya.order_service.exception.ProductNotAvailableException;
import plohiya.order_service.exception.ProductOutOfStockException;
import plohiya.order_service.model.Order;
import plohiya.order_service.model.OrderLineItems;
import plohiya.order_service.repository.OrderRepository;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void placeOrder(OrderRequest orderRequest){
        // Validate order request FIRST - before any processing
        validateOrderRequest(orderRequest);
        
        log.info("Placing order with {} items", orderRequest.getOrderLineItemsDtoList().size());
        
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems>orderLineItemsList =  orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItemsList);

        List<String>skuCodes = orderLineItemsList.stream()
                .map(OrderLineItems::getSkuCode)
                .filter(skuCode -> skuCode != null && !skuCode.trim().isEmpty())
                .toList();

        if (skuCodes.isEmpty()) {
            throw new InvalidOrderRequestException("Order must contain at least one valid product with SKU code");
        }

        log.info("Checking inventory for SKU codes: {}", skuCodes);
        
        // Call inventory service with improved error handling
        InventoryResponse[] inventoryResponseArray;
        try {
            inventoryResponseArray = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> {
                                skuCodes.forEach(skuCode -> uriBuilder.queryParam("skuCode", skuCode));
                                return uriBuilder.build();
                            })
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .timeout(Duration.ofSeconds(10)) // Increased timeout
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .filter(throwable -> {
                                boolean isRetryable = throwable instanceof WebClientException || 
                                                     (throwable.getMessage() != null && 
                                                      throwable.getMessage().contains("timeout"));
                                if (isRetryable) {
                                    log.warn("Retrying inventory service call due to: {}", throwable.getMessage());
                                }
                                return isRetryable;
                            })
                            .doBeforeRetry(retrySignal -> 
                                log.info("Retrying inventory service call, attempt: {}", retrySignal.totalRetries() + 1)))
                    .doOnError(error -> log.error("Error calling inventory service: {}", error.getMessage()))
                    .block();
                    
            log.info("Received inventory response for {} products", 
                    inventoryResponseArray != null ? inventoryResponseArray.length : 0);
                    
        } catch (Exception ex) {
            log.error("Failed to call inventory service after retries: {}", ex.getMessage(), ex);
            if (ex.getMessage() != null && ex.getMessage().contains("Timeout")) {
                throw new ProductNotAvailableException("Inventory service is not responding. Please try again later.");
            }
            throw new ProductNotAvailableException("Unable to check inventory. Please try again later.");
        }

        if (inventoryResponseArray == null || inventoryResponseArray.length != skuCodes.size()) {
            log.warn("Inventory response mismatch. Expected: {}, Got: {}", 
                    skuCodes.size(), 
                    inventoryResponseArray != null ? inventoryResponseArray.length : 0);
            throw new ProductNotAvailableException("Some products are not available in inventory");
        } 
        
        boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                .allMatch(r -> Boolean.TRUE.equals(r.getInStock()));

        if(allProductsInStock){
            log.info("All products in stock. Saving order: {}", order.getOrderNumber());
            orderRepository.save(order);
            kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
            log.info("Order {} placed successfully", order.getOrderNumber());
        } else {
            log.warn("Some products are out of stock");
            throw new ProductOutOfStockException("Product is not in stock, please try again later");
        }

    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order with id " + id + " not found"));
    }

    private void validateOrderRequest(OrderRequest orderRequest) {
        if (orderRequest == null) {
            throw new InvalidOrderRequestException("Order request cannot be null");
        }

        if (orderRequest.getOrderLineItemsDtoList() == null || orderRequest.getOrderLineItemsDtoList().isEmpty()) {
            throw new InvalidOrderRequestException("Order must contain at least one item");
        }

        // Validate each order line item
        for (int i = 0; i < orderRequest.getOrderLineItemsDtoList().size(); i++) {
            OrderLineItemsDto item = getOrderLineItemsDto(orderRequest, i);
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new InvalidOrderRequestException("Order item at position " + i + " must have a valid quantity greater than zero");
            }
        }
    }

    private static OrderLineItemsDto getOrderLineItemsDto(OrderRequest orderRequest, int i) {
        OrderLineItemsDto item = orderRequest.getOrderLineItemsDtoList().get(i);
        if (item == null) {
            throw new InvalidOrderRequestException("Order item at position " + i + " cannot be null");
        }
        if (item.getSkuCode() == null || item.getSkuCode().trim().isEmpty()) {
            throw new InvalidOrderRequestException("Order item at position " + i + " must have a valid SKU code");
        }
        if (item.getPrice() == null || item.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderRequestException("Order item at position " + i + " must have a valid price greater than zero");
        }
        return item;
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());

        return orderLineItems;
    }


}