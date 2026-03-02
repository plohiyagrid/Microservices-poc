package plohiya.order_service.utils;

import plohiya.order_service.dto.OrderLineItemsDto;
import plohiya.order_service.model.OrderLineItems;

import java.util.List;
import java.util.stream.Collectors;

public final class OrderMapper {

    private OrderMapper() {}

    public static OrderLineItems toOrderLineItems(OrderLineItemsDto dto) {
        if (dto == null) {
            return null;
        }

        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(dto.getPrice());
        orderLineItems.setQuantity(dto.getQuantity());
        orderLineItems.setSkuCode(dto.getSkuCode());
        return orderLineItems;
    }

    public static List<OrderLineItems> toOrderLineItemsList(List<OrderLineItemsDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(OrderMapper::toOrderLineItems)
                .collect(Collectors.toList());
    }
}