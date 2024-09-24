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

import static ru.rozhdestveno.taxi.entity.customer.CustomerState.SEND_MESSAGE;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeState.DRIVER_SEND_MESSAGE;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.CANCELED;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.COMPLETED;

@Component
@Slf4j
public class DriverSendMessageCallbackHandler extends EmployeeHandler {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public DriverSendMessageCallbackHandler(OrderRepository orderRepository, CustomerRepository customerRepository,
                                            EmployeeRepository employeeRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DRIVER) && message.startsWith("/send_message ");

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();
        long orderId = Long.parseLong(message.replace("/send_message ", ""));
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

        //изменение статусов водителя и клиента для возможности обмена сообщениями
        employee.setState(DRIVER_SEND_MESSAGE);
        employeeRepository.saveAndFlush(employee);
        order.getClient().setState(SEND_MESSAGE);
        customerRepository.saveAndFlush(order.getClient());
        messages.add(BotUtil.createMessage(employee.getId(), "Введите в свободной форме текст сообщения"));
        messages.add(BotUtil.createMessage(order.getClient().getId(), "Ожидайте сообщение от водителя"));
        log.info("Водитель {}:{} первым пишет клиенту {}",
                employee.getLastName(), employee.getId(), order.getClient().getId());
        return messages;
    }
}
