package ru.rozhdestveno.taxi.service.hadler.customer;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.customer.Customer;

import java.util.List;

public abstract class CustomerHandler {
    protected CustomerHandler next;

    public void setNextHandler(CustomerHandler next) {
        this.next = next;
    }

    public abstract List<PartialBotApiMethod<?>> handleRequest(Customer client, String message);
}
