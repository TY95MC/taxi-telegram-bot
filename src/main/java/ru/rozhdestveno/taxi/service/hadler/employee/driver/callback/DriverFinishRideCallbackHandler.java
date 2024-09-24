package ru.rozhdestveno.taxi.service.hadler.employee.driver.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.customer.CustomerRepository;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.entity.order.Order;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ru.rozhdestveno.taxi.entity.customer.CustomerState.START;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeState.EMPLOYEE_ON_DUTY;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.CANCELED;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.COMPLETED;

@Component
@Slf4j
public class DriverFinishRideCallbackHandler extends EmployeeHandler {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public DriverFinishRideCallbackHandler(OrderRepository orderRepository, CustomerRepository customerRepository,
                                           EmployeeRepository employeeRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DRIVER) && message.startsWith("/finish_ride ");

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();
        message = message.replace("/finish_ride ", "");
        long orderId = Long.parseLong(message);

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

        order.setStatus(COMPLETED);
        orderRepository.save(order);

        //изменение статуса клиента и водителя по завершению поездки
        if (order.getClient() != null) {
            order.getClient().setState(START);
            customerRepository.saveAndFlush(order.getClient());
            messages.add(BotUtil.createMessage(order.getClient().getId(), "Заказ завершен"));
        }

        employee.setState(EMPLOYEE_ON_DUTY);
        employeeRepository.saveAndFlush(employee);
        log.info("Водитель {}:{} завершил поездку {}, стоимость {}",
                employee.getLastName(), employee.getId(), order.getId(), order.getPrice());
        messages.add(BotUtil.createMessage(employee.getId(), "Поездка <code>" + orderId + "</code> завершена"));
        return messages;
    }
}
