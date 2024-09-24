package ru.rozhdestveno.taxi.service.hadler.employee.driver.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.entity.order.Order;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;
import java.util.Optional;

import static ru.rozhdestveno.taxi.entity.order.OrderStatus.CANCELED;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.COMPLETED;

@Component
@Slf4j
public class DriverCloseToClientCallbackHandler extends EmployeeHandler {
    private final OrderRepository orderRepository;

    public DriverCloseToClientCallbackHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DRIVER) && message.startsWith("/close_to_client ");

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        long orderId = Long.parseLong(message.replace("/close_to_client ", ""));
        Order order;
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isPresent()) {
            order = orderOpt.get();
        } else {
            log.error("Заказ с id: " + orderId + " не найден");
            return List.of(BotUtil.createMessage(employee.getId(), "Что-то пошло не так, заказ не найден"));
        }

        if (order.getStatus().equals(COMPLETED) || order.getStatus().equals(CANCELED)) {
            return List.of(BotUtil.createMessage(employee.getId(),
                    "Заказ <code>" + order.getId() + "</code> уже числится завершенным"));
        }

        return List.of(BotUtil.createMessage(order.getClient().getId(), "Водитель прибудет в течение 5 минут"));
    }
}
