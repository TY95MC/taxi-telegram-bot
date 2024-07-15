package ru.rozhdestveno.taxi.entity.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT SUM(o.price) " +
            "FROM Order o " +
            "WHERE o.orderDate = ?1 " +
            "AND o.status = 'COMPLETED'")
    Integer getSumCompleteByDate(LocalDate date);

    @Query("SELECT SUM(o.price) " +
            "FROM Order o " +
            "WHERE o.orderDate = ?1 " +
            "AND o.status = 'CANCELED'")
    Integer getSumCancelByDate(LocalDate date);

    @Query("SELECT o " +
            "FROM Order o " +
            "WHERE o.driver.id = ?1 " +
            "ORDER BY o.id DESC " +
            "LIMIT 1")
    Order findDriverLastOrder(long driverId);

    @Query("SELECT o " +
            "FROM Order o " +
            "WHERE o.client.id = ?1 " +
            "ORDER BY o.id DESC " +
            "LIMIT 1")
    Optional<Order> findClientLastOrder(long clientId);

    @Query("SELECT o " +
            "FROM Order o " +
            "WHERE o.client.id = ?1 " +
            "AND o.status = 'ACCEPTED'" +
            "ORDER BY o.id DESC " +
            "LIMIT 1")
    Optional<Order> findClientCurrentOrder(long clientId);
}
