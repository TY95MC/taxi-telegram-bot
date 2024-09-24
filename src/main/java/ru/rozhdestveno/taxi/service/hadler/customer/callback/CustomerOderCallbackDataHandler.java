package ru.rozhdestveno.taxi.service.hadler.customer.callback;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.entity.customer.CustomerRepository;
import ru.rozhdestveno.taxi.entity.customer.CustomerState;
import ru.rozhdestveno.taxi.service.hadler.customer.CustomerHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;

import static ru.rozhdestveno.taxi.constants.Constants.ADDRESS_TEXT;

@Component
public class CustomerOderCallbackDataHandler extends CustomerHandler {
    private final CustomerRepository customerRepository;

    public CustomerOderCallbackDataHandler(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Customer client, String message) {
        if (!message.equals("/order") && next != null) {
            return next.handleRequest(client, message);
        }

        //смена статуса клиента для возможности введения текста адреса поездки
        client.setState(CustomerState.ORDER);
        customerRepository.saveAndFlush(client);
        return List.of(BotUtil.createMessage(client.getId(), ADDRESS_TEXT));
    }
}
