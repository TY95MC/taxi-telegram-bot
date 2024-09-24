package ru.rozhdestveno.taxi.service.hadler.customer.text;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.entity.customer.CustomerRepository;
import ru.rozhdestveno.taxi.entity.customer.CustomerState;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.lost.Lost;
import ru.rozhdestveno.taxi.entity.lost.LostRepository;
import ru.rozhdestveno.taxi.service.hadler.customer.CustomerHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class CustomerLostTextHandler extends CustomerHandler {

    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final LostRepository lostRepository;

    public CustomerLostTextHandler(CustomerRepository customerRepository,
                                   EmployeeRepository employeeRepository, LostRepository lostRepository) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.lostRepository = lostRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Customer client, String message) {
        if (!client.getState().equals(CustomerState.LOST) && next != null) {
            return next.handleRequest(client, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();

        //проверка на ограничение количества заявок о потерянных вещах в день во избежание спама
        List<Lost> losts = lostRepository.findLastLosts(client.getId(), LocalDate.now());

        if (losts != null && losts.size() >= 3) {
            messages.add(BotUtil.createMessage(client.getId(), "В день можно оставлять не более 3 заявок."));
            return messages;
        }

        //создание заявки о потерянной вещи
        Lost lost = new Lost();
        lost.setClient(client);
        lost.setText(message);
        lostRepository.saveAndFlush(lost);
        client.setState(CustomerState.START);
        customerRepository.saveAndFlush(client);
        Long dispatcherId = employeeRepository.findDispatcherOnDuty();

        //уведомление диспетчера по потерянной вещи
        if (dispatcherId != null) {
            messages.add(BotUtil.createMessage(dispatcherId,
                    "Заявка о потерянной вещи " + lost.getId() + " " + message));
        }

        messages.add(BotUtil.createMessage(client.getId(), "Принято в обработку. Номер заявки " + lost.getId()));
        return messages;
    }
}
