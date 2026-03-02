package plohiya.order_service.utils;

import plohiya.order_service.dto.OrderLineItemsDto;
import plohiya.order_service.dto.OrderRequest;
import plohiya.order_service.exception.InvalidOrderRequestException;

import java.math.BigDecimal;

public final class OrderValidator {

    private OrderValidator() {}

    public static void validateOrderRequest(OrderRequest orderRequest) {
        if (orderRequest == null) {
            throw new InvalidOrderRequestException("Order request cannot be null");
        } if (orderRequest.getOrderLineItemsDtoList() == null || orderRequest.getOrderLineItemsDtoList().isEmpty()) {
            throw new InvalidOrderRequestException("Order must contain at least one item");
        }
        orderRequest.getOrderLineItemsDtoList().forEach(OrderValidator::validateOrderLineItem);
    }

    public static void validateOrderLineItem(OrderLineItemsDto item) {
        if (item == null) {
            throw new InvalidOrderRequestException("Order item cannot be null");
        } if (item.getSkuCode() == null || item.getSkuCode().trim().isEmpty()) {
            throw new InvalidOrderRequestException("Order item must have a valid SKU code");
        } if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new InvalidOrderRequestException("Order item must have a valid quantity greater than zero");
        } if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderRequestException("Order item must have a valid price greater than zero");
        }
    }
}