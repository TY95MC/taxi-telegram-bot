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

@Component
@Slf4j
public class CustomerSendMessageCallbackDataHandler extends CustomerHandler {
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;

    public CustomerSendMessageCallbackDataHandler(CustomerRepository customerRepository,
                                                  OrderRepository orderRepository, EmployeeRepository employeeRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Customer client, String message) {
        if (!message.startsWith("/send_message ") && next != null) {
            return next.handleRequest(client, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();
        //Поиск текущего активного заказа
        Optional<Order> optional = orderRepository.findClientCurrentOrder(client.getId());

        if (optional.isEmpty()) {
            return List.of(BotUtil.createMessage(client.getId(), "Заказ уже числится завершенным"));
        } else {
            Order order = optional.get();

            //Проверка на завершенность заказа
            if (order.getStatus().equals(OrderStatus.COMPLETED) || order.getStatus().equals(OrderStatus.CANCELED)) {
                return List.of(BotUtil.createMessage(client.getId(),
                        "Заказ <code>" + order.getId() + "</code> уже числится завершенным"));
            }

            //проверка текущего статуса клиента
            if (client.getState().equals(CustomerState.SEND_MESSAGE)) {
                return List.of(BotUtil.createMessage(client.getId(), "Вы уже можете писать сообщение водителю"));
            }

            //изменение статуса клиента и водителя, а также их оповещение
            order.getClient().setState(CustomerState.SEND_MESSAGE);
            order.getDriver().setState(EmployeeState.DRIVER_SEND_MESSAGE);
            customerRepository.saveAndFlush(order.getClient());
            employeeRepository.saveAndFlush(order.getDriver());
            messages.add(BotUtil.createMessage(client.getId(), "Введите ваше сообщение"));
            messages.add(BotUtil.createMessage(order.getDriver().getId(), "Ожидайте сообщение от клиента"));
            log.info("Клиент {} первым пишет водителю {}:{}",
                    client.getId(), order.getDriver().getLastName(), order.getDriver().getId());
        }

        return messages;
    }
}
