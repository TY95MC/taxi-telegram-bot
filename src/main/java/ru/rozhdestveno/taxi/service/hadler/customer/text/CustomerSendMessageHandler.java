package ru.rozhdestveno.taxi.service.hadler.customer.text;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.entity.customer.CustomerState;
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
public class CustomerSendMessageHandler extends CustomerHandler {

    private final OrderRepository orderRepository;

    public CustomerSendMessageHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Customer client, String message) {
        if (!client.getState().equals(CustomerState.SEND_MESSAGE) && next != null) {
            return next.handleRequest(client, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();

        //поиск текущего принятого заказа
        Optional<Order> optional = orderRepository.findClientCurrentOrder(client.getId());

        if (optional.isEmpty()) {
            messages.add(BotUtil.createMessage(client.getId(), "Дождитесь подтверждения заказа"));
        } else {
            Order order = optional.get();

            if (order.getStatus().equals(OrderStatus.ACCEPTED)) {
                //создание сообщения для водителя с текстом, написанным клиентом
                messages.add(BotUtil.createMessage(order.getDriver().getId(), message));
                log.info("Заказ {}, клиент {}, текст: {}", order.getId(), client.getId(), message);
            } else {
                messages.add(BotUtil.createMessage(client.getId(), "Дождитесь подтверждения заказа"));
            }
        }

        return messages;
    }
}
