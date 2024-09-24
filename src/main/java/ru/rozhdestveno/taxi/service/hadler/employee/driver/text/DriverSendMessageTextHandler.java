package ru.rozhdestveno.taxi.service.hadler.employee.driver.text;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.entity.order.Order;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;

import static ru.rozhdestveno.taxi.entity.order.OrderStatus.CANCELED;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.COMPLETED;

@Component
@Slf4j
public class DriverSendMessageTextHandler extends EmployeeHandler {

    private final OrderRepository orderRepository;

    public DriverSendMessageTextHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DRIVER) &&
                employee.getState().equals(EmployeeState.DRIVER_SEND_MESSAGE);

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        Order order = orderRepository.findDriverLastOrder(employee.getId());

        if (order.getStatus().equals(COMPLETED) || order.getStatus().equals(CANCELED)) {
            return List.of(BotUtil.createMessage(employee.getId(),
                    "Заказ <code>" + order.getId() + "</code> уже числится завершенным"));
        }

        log.info("Заказ {}, водитель {}:{}, текст: {}",
                order.getId(), employee.getLastName(), employee.getId(), message);
        return List.of(BotUtil.createMessage(order.getClient().getId(), message));
    }
}
