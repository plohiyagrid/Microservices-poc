package plohiya.order_service.utils;

import plohiya.order_service.exception.InvalidOrderRequestException;
import plohiya.order_service.model.OrderLineItems;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class OrderUtils {

    private OrderUtils() {}

    public static String generateOrderNumber() {
        return UUID.randomUUID().toString();
    }

    public static Map<String, Integer> buildSkuQuantityMap(List<OrderLineItems> orderLineItems) {
        if (orderLineItems == null || orderLineItems.isEmpty()) {
            throw new InvalidOrderRequestException("Order must contain at least one valid product with SKU code");
        }
        return orderLineItems.stream()
                .collect(Collectors.toMap(
                        OrderLineItems::getSkuCode,
                        OrderLineItems::getQuantity
                ));
    }
}