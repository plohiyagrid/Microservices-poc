package plohiya.order_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import plohiya.order_service.model.OrderLineItems;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class OrderLineItemsRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderLineItemsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveAll(List<OrderLineItems> orderLineItems, Long orderId) {
        String sql = "INSERT INTO t_order_line_items (order_id, sku_code, price, quantity) VALUES (?, ?, ?, ?)";
        
        for (OrderLineItems item : orderLineItems) {
            jdbcTemplate.update(sql, 
                orderId,
                item.getSkuCode(),
                item.getPrice(),
                item.getQuantity()
            );
        }
    }

    public List<OrderLineItems> findByOrderId(Long orderId) {
        String sql = "SELECT id, order_id, sku_code, price, quantity FROM t_order_line_items WHERE order_id = ?";
        return jdbcTemplate.query(sql, orderLineItemsRowMapper(), orderId);
    }

    private RowMapper<OrderLineItems> orderLineItemsRowMapper() {
        return (rs, rowNum) -> {
            OrderLineItems item = new OrderLineItems();
            item.setId(rs.getLong("id"));
            item.setOrderId(rs.getLong("order_id"));
            item.setSkuCode(rs.getString("sku_code"));
            item.setPrice(rs.getBigDecimal("price"));
            item.setQuantity(rs.getInt("quantity"));
            return item;
        };
    }
}