// package plohiya.order_service.model;

// import jakarta.persistence.*;
// import lombok.AllArgsConstructor;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;

// import java.util.List;

// @Entity
// @Table(name = "t_orders")
// @Getter
// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
// public class Order {

//     @Id
//     @GeneratedValue(strategy = GenerationType.AUTO)
//     private Long id;
//     private String OrderNumber;
//     @OneToMany(cascade = CascadeType.ALL)
//     private List<OrderLineItems> orderLineItemsList;

// }

package plohiya.order_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;
    private String orderNumber;
    private List<OrderLineItems> orderLineItemsList;
}