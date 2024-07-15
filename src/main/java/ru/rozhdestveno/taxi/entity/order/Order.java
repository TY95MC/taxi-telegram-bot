package ru.rozhdestveno.taxi.entity.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.entity.employee.Employee;

import java.time.LocalDate;

@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Size(max = 250)
    private String address;

    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Customer client;

    @ManyToOne
    @JoinColumn(name = "driver_id", referencedColumnName = "id")
    private Employee driver;

    @Column(name = "order_date")
    private LocalDate orderDate = LocalDate.now();

    private int price = 200;

    @Enumerated(EnumType.STRING)
    OrderStatus status = OrderStatus.WAITING;
}
