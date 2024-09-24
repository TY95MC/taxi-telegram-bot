package ru.rozhdestveno.taxi.service.hadler.customer.text;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.entity.customer.CustomerState;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.order.Order;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.entity.order.OrderStatus;
import ru.rozhdestveno.taxi.service.hadler.customer.CustomerHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ru.rozhdestveno.taxi.constants.Constants.CLIENT_ORDER_MENU;
import static ru.rozhdestveno.taxi.constants.Constants.DRIVER_ACCEPT_ORDER_MENU;

@Component
public class CustomerOrderTextHandler extends CustomerHandler {

    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;

    public CustomerOrderTextHandler(OrderRepository orderRepository, EmployeeRepository employeeRepository) {
        this.orderRepository = orderRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Customer client, String message) {
        if (!client.getState().equals(CustomerState.ORDER) && next != null) {
            return next.handleRequest(client, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();
        //поиск последнего заказа клиента
        Optional<Order> optional = orderRepository.findClientLastOrder(client.getId());

        if (optional.isPresent()) {
            OrderStatus status = optional.get().getStatus();

            //проверка на незавершенный заказ
            if (status.equals(OrderStatus.WAITING) || status.equals(OrderStatus.ACCEPTED)) {
                messages.add(BotUtil.createMessage(client.getId(),
                        "Нельзя создать новый заказ, не завершив текущий"));
                return messages;
            }
        }

        List<Long> drivers = employeeRepository.findWaitingDriversIds();//id водителей на смене

        if (drivers.isEmpty()) {
            messages.add(BotUtil.createMessage(client.getId(), "Все водители заняты, попробуйте позже!"));
            return messages;
        }

        //создание нового заказа
        Order order = new Order();
        order.setClient(client);
        order.setOrderDate(LocalDate.now());
        order.setAddress(message);
        order = orderRepository.saveAndFlush(order);

        Order finalOrder = order;
        drivers.forEach(
                //создание для каждого водителя сообщения о заказе с номером заказа
                //и адресом(пункт А и пункт Б), введенным клиентом
                driverId -> messages.add(
                        BotUtil.createMessage(
                                driverId,
                                "Заказ <code>" + finalOrder.getId() + "</code> " +
                                        " клиента <code>" + client.getId() + "</code>: " + finalOrder.getAddress(),
                                DRIVER_ACCEPT_ORDER_MENU,
                                finalOrder.getId()
                        )
                )
        );

        //уведомление клиента об обработке заказа
        messages.add(BotUtil.createMessage(
                client.getId(),
                "Заказ " + order.getId() + " обрабатывается. " +
                        "Если в течение 5 минут ваш заказ не приняли, отмените его и создайте заново.",
                CLIENT_ORDER_MENU, order.getId()));

        return messages;
    }
}
