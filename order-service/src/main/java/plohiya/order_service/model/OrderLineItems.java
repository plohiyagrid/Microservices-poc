// package plohiya.order_service.model;

// import jakarta.persistence.*;
// import lombok.AllArgsConstructor;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;

// import java.math.BigDecimal;

// @Entity
// @Table(name = "t_order_line_items")
// @Getter
// @Setter
// @AllArgsConstructor
// @NoArgsConstructor
// public class OrderLineItems {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;
//     private String skuCode;
//     private BigDecimal price;
//     private Integer quantity;
// }

package plohiya.order_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderLineItems {
    private Long id;
    private Long orderId; // Foreign key to Order
    private String skuCode;
    private BigDecimal price;
    private Integer quantity;
}