package ru.rozhdestveno.taxi.service.hadler.customer.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.entity.customer.CustomerRepository;
import ru.rozhdestveno.taxi.entity.customer.CustomerState;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.entity.order.Order;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.entity.order.OrderStatus;
import ru.rozhdestveno.taxi.service.hadler.customer.CustomerHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ru.rozhdestveno.taxi.constants.Constants.DEFAULT_ERROR_MESSAGE;

@Component
@Slf4j
public class CustomerCancelOrderCallbackDataHandler extends CustomerHandler {
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;

    public CustomerCancelOrderCallbackDataHandler(CustomerRepository customerRepository,
                                                  OrderRepository orderRepository, EmployeeRepository employeeRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Customer client, String message) {
        if (!message.startsWith("/cancel_order ") && next != null) {
            return next.handleRequest(client, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();
        message = message.replace("/cancel_order ", "");//получение id заказа из входящего сообщения
        client.setState(CustomerState.START);
        customerRepository.saveAndFlush(client);
        Optional<Order> order = orderRepository.findById(Long.valueOf(message));

        if (order.isPresent()) {
            //Проверка на завершенность заказа
            if (order.get().getStatus().equals(OrderStatus.COMPLETED)
                    || order.get().getStatus().equals(OrderStatus.CANCELED)) {
                return List.of(BotUtil.createMessage(client.getId(),
                        "Заказ <code>" + order.get().getId() + "</code> уже числится завершенным"));
            }

            //смена статуса заказа и оповещение клиента об успешной отмене
            order.get().setStatus(OrderStatus.CANCELED);
            orderRepository.saveAndFlush(order.get());
            messages.add(
                    BotUtil.createMessage(client.getId(), "Заказ " + order.get().getId() + " отменен!")
            );

            //Оповещение водителя об отмене заказа клиентом
            if (order.get().getDriver() != null) {
                messages.add(
                        BotUtil.createMessage(order.get().getDriver().getId(),
                                "Клиент отменил поездку <code>" + order.get().getId() + "</code>")
                );
                order.get().getDriver().setState(EmployeeState.EMPLOYEE_ON_DUTY);
                employeeRepository.saveAndFlush(order.get().getDriver());
            }

            log.warn("Клиент {}, отменил поездку {}", client.getId(), order.get().getId());
        } else {
            return List.of(BotUtil.createMessage(client.getId(), DEFAULT_ERROR_MESSAGE));
        }

        return messages;
    }
}
