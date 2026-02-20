package plohiya.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import plohiya.order_service.dto.InventoryResponse;
import plohiya.order_service.dto.OrderLineItemsDto;
import plohiya.order_service.dto.OrderRequest;
import plohiya.order_service.event.OrderPlacedEvent;
import plohiya.order_service.model.Order;
import plohiya.order_service.model.OrderLineItems;
import plohiya.order_service.repository.OrderRepository;

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
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems>orderLineItemsList =  orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItemsList);

        List<String>skuCodes = orderLineItemsList.stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> {
                            skuCodes.forEach(skuCode -> uriBuilder.queryParam("skuCode", skuCode));
                            return uriBuilder.build();
                        })
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block(Duration.ofSeconds(5));
        if (inventoryResponseArray == null || inventoryResponseArray.length != skuCodes.size()) {
            throw new IllegalArgumentException("Some products are not available in inventory");
        }
        boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                .allMatch(r -> Boolean.TRUE.equals(r.getInStock()));

        if(allProductsInStock){
            orderRepository.save(order);
            kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
        } else {
            throw new IllegalArgumentException("Product is not in stock, please try again later");
        }

    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());

        return orderLineItems;
    }


}
