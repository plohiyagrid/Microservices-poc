// package plohiya.order_service.repository;

// import org.springframework.data.jpa.repository.JpaRepository;
// import plohiya.order_service.model.Order;

// public interface OrderRepository extends JpaRepository<Order , Long> {
// }

package plohiya.order_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import plohiya.order_service.model.Order;
import plohiya.order_service.model.OrderLineItems;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;
    private final OrderLineItemsRepository orderLineItemsRepository;

    public OrderRepository(JdbcTemplate jdbcTemplate, OrderLineItemsRepository orderLineItemsRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderLineItemsRepository = orderLineItemsRepository;
    }

    public Order save(Order order) {
        String sql = "INSERT INTO t_orders (order_number) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, order.getOrderNumber());
            return ps;
        }, keyHolder);
        
        Long orderId = keyHolder.getKey().longValue();
        order.setId(orderId);
        
        // Save order line items
        if (order.getOrderLineItemsList() != null) {
            orderLineItemsRepository.saveAll(order.getOrderLineItemsList(), orderId);
        }
        
        return order;
    }

    public List<Order> findAll() {
        String sql = "SELECT id, order_number FROM t_orders";
        List<Order> orders = jdbcTemplate.query(sql, orderRowMapper());
        
        // Load order line items for each order
        orders.forEach(order -> {
            List<OrderLineItems> lineItems = orderLineItemsRepository.findByOrderId(order.getId());
            order.setOrderLineItemsList(lineItems);
        });
        
        return orders;
    }

    public Optional<Order> findById(Long id) {
        String sql = "SELECT id, order_number FROM t_orders WHERE id = ?";
        List<Order> orders = jdbcTemplate.query(sql, orderRowMapper(), id);
        
        if (orders.isEmpty()) {
            return Optional.empty();
        }
        
        Order order = orders.get(0);
        List<OrderLineItems> lineItems = orderLineItemsRepository.findByOrderId(id);
        order.setOrderLineItemsList(lineItems);
        
        return Optional.of(order);
    }

    private RowMapper<Order> orderRowMapper() {
        return (rs, rowNum) -> {
            Order order = new Order();
            order.setId(rs.getLong("id"));
            order.setOrderNumber(rs.getString("order_number"));
            return order;
        };
    }
}