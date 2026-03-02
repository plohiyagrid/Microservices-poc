package plohiya.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import plohiya.order_service.dto.InventoryReservationRequest;
import plohiya.order_service.dto.InventoryReservationResponse;
import plohiya.order_service.dto.OrderRequest;
import plohiya.order_service.event.OrderPlacedEvent;
import plohiya.order_service.exception.OrderNotFoundException;
import plohiya.order_service.exception.ProductNotAvailableException;
import plohiya.order_service.exception.ProductOutOfStockException;
import plohiya.order_service.model.Order;
import plohiya.order_service.repository.OrderRepository;
import plohiya.order_service.utils.OrderMapper;
import plohiya.order_service.utils.OrderUtils;
import plohiya.order_service.utils.OrderValidator;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void placeOrder(OrderRequest orderRequest) {
        OrderValidator.validateOrderRequest(orderRequest);

        Order order = buildOrder(orderRequest);
        Map<String, Integer> skuQuantityMap = OrderUtils.buildSkuQuantityMap(order.getOrderLineItemsList());

        log.info("Placing order {} with {} items", order.getOrderNumber(), skuQuantityMap.size());

        reserveInventory(skuQuantityMap);
        saveOrderAndPublishEvent(order);

        log.info("Order {} placed successfully", order.getOrderNumber());
    }

    private Order buildOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(OrderUtils.generateOrderNumber());
        order.setOrderLineItemsList(OrderMapper.toOrderLineItemsList(orderRequest.getOrderLineItemsDtoList()));
        return order;
    }

    private void reserveInventory(Map<String, Integer> skuQuantityMap) {
        InventoryReservationResponse response = callInventoryService(skuQuantityMap);
        log.info("Inventory reserved successfully. Updated quantities: {}", response.getUpdatedQuantities());
    }

    private InventoryReservationResponse callInventoryService(Map<String, Integer> skuQuantityMap) {
        try {
            return webClientBuilder.build()
                    .post()
                    .uri("http://inventory-service/api/inventory/reserve")
                    .bodyValue(new InventoryReservationRequest(skuQuantityMap))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("Inventory service returned 4xx error: {}", body);
                                    return Mono.error(new ProductOutOfStockException(
                                            "Inventory reservation failed: " + body));
                                });
                    })
                    .bodyToMono(InventoryReservationResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientException
                                    && !(throwable instanceof WebClientResponseException)))
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("WebClient error response: Status={}, Body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getStatusCode().is4xxClientError()) {
                throw new ProductOutOfStockException("Inventory reservation failed: " + ex.getResponseBodyAsString());
            }
            throw new ProductNotAvailableException("Inventory service error: " + ex.getMessage());
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
}