package plohiya.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import plohiya.order_service.dto.InventoryReservationRequest;
import plohiya.order_service.dto.InventoryReservationResponse;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void placeOrder(OrderRequest orderRequest) {
        validateOrderRequest(orderRequest);
        
        Order order = buildOrder(orderRequest);
        Map<String, Integer> skuQuantityMap = buildSkuQuantityMap(order.getOrderLineItemsList());
        
        log.info("Placing order {} with {} items", order.getOrderNumber(), skuQuantityMap.size());
        
        reserveInventory(skuQuantityMap);
        saveOrderAndPublishEvent(order);
        
        log.info("Order {} placed successfully", order.getOrderNumber());
    }

    private Order buildOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        
        List<OrderLineItems> orderLineItemsList = orderRequest.getOrderLineItemsDtoList().stream()
                .map(this::mapToDto)
                .toList();
        
        order.setOrderLineItemsList(orderLineItemsList);
        return order;
    }

    private Map<String, Integer> buildSkuQuantityMap(List<OrderLineItems> orderLineItems) {
        return orderLineItems.stream()
                .filter(item -> item.getSkuCode() != null && !item.getSkuCode().trim().isEmpty())
                .collect(Collectors.toMap(
                        OrderLineItems::getSkuCode,
                        OrderLineItems::getQuantity
                ));
    }

    private void reserveInventory(Map<String, Integer> skuQuantityMap) {
        if (skuQuantityMap.isEmpty()) {
            throw new InvalidOrderRequestException("Order must contain at least one valid product with SKU code");
        }

        try {
            InventoryReservationResponse response = webClientBuilder.build()
                    .post()
                    .uri("http://inventory-service/api/inventory/reserve")
                    .bodyValue(new InventoryReservationRequest(skuQuantityMap))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            clientResponse -> Mono.error(new ProductOutOfStockException(
                                    "Inventory reservation failed: " + clientResponse.statusCode())))
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ProductNotAvailableException(
                                    "Inventory service error: " + clientResponse.statusCode())))
                    .bodyToMono(InventoryReservationResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientException))
                    .block();
            
            log.info("Inventory reserved successfully. Updated quantities: {}", 
                    response != null ? response.getUpdatedQuantities() : "null");
                    
        } catch (ProductOutOfStockException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to reserve inventory: {}", e.getMessage(), e);
            throw new ProductNotAvailableException("Unable to reserve inventory. Please try again later.");
        }
    }

    private void saveOrderAndPublishEvent(Order order) {
        orderRepository.save(order);
        kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
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

        orderRequest.getOrderLineItemsDtoList().forEach(this::validateOrderLineItem);
    }

    private void validateOrderLineItem(OrderLineItemsDto item) {
        if (item == null) {
            throw new InvalidOrderRequestException("Order item cannot be null");
        }
        if (item.getSkuCode() == null || item.getSkuCode().trim().isEmpty()) {
            throw new InvalidOrderRequestException("Order item must have a valid SKU code");
        }
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new InvalidOrderRequestException("Order item must have a valid quantity greater than zero");
        }
        if (item.getPrice() == null || item.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderRequestException("Order item must have a valid price greater than zero");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}