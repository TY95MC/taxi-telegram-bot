package ru.rozhdestveno.taxi.service.hadler.customer;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;

import static ru.rozhdestveno.taxi.constants.Constants.DEFAULT_ERROR_MESSAGE;

/**
 * Default error message handler
 * Always should be last handler in chain
 */
@Component
public class DefaultCustomerErrorHandler extends CustomerHandler {
    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Customer client, String message) {
        return List.of(BotUtil.createMessage(client.getId(), DEFAULT_ERROR_MESSAGE));
    }
}
