package ru.rozhdestveno.taxi.service.hadler.customer;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.entity.customer.CustomerBanStatus;
import ru.rozhdestveno.taxi.entity.customer.CustomerRepository;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.time.LocalDate;
import java.util.List;

/**
 * Customer's ban status checking handler
 * Always should be thirst handler in chain
 */
@Component
public class BanCheckHandler extends CustomerHandler {
    private final CustomerRepository customerRepository;

    public BanCheckHandler(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Customer client, String message) {
        //проверка для третьего(наивысшего) уровня предупреждения
        if (client.getStatus().equals(CustomerBanStatus.BANNED)) {
            //Если прошло 3 месяца с момента вынесения третьего предупреждения, уровень предупреждения понижается
            if (client.getBannedOn().isBefore(LocalDate.now().minusMonths(3))) {
                client.setStatus(CustomerBanStatus.SECOND_WARN);
                client.setSecondWarn(LocalDate.now().minusDays(1));
                customerRepository.saveAndFlush(client);
                return next.handleRequest(client, message);
            }

            return List.of(BotUtil.createMessage(client.getId(),
                    "Сервис приостановлен до: " + client.getBannedOn().plusMonths(3)));
        }

        //проверка для второго уровня предупреждения
        if (client.getStatus().equals(CustomerBanStatus.SECOND_WARN)) {
            if (client.getSecondWarn().isAfter(LocalDate.now())) {
                return List.of(BotUtil.createMessage(client.getId(),
                        "Сервис приостановлен до: " + client.getSecondWarn()));
            }

            //Если прошло 2 месяца с момента вынесения второго предупреждения, уровень предупреждения понижается
            if (client.getSecondWarn().isBefore(LocalDate.now().minusMonths(2))) {
                client.setStatus(CustomerBanStatus.FIRST_WARN);
                client.setFirstWarn(LocalDate.now().minusDays(1));
                customerRepository.save(client);
                return next.handleRequest(client, message);
            }
        }

        //проверка для первого уровня предупреждения
        if (client.getStatus().equals(CustomerBanStatus.FIRST_WARN)) {
            if (client.getFirstWarn().isAfter(LocalDate.now())) {
                return List.of(BotUtil.createMessage(client.getId(),
                        "Сервис приостановлен до: " + client.getFirstWarn()));
            }

            //Если прошел 1 месяц с момента вынесения первого предупреждения, уровень предупреждения понижается
            if (client.getFirstWarn().isBefore(LocalDate.now().minusMonths(1))) {
                client.setStatus(CustomerBanStatus.NO_WARN);
                customerRepository.save(client);
                return next.handleRequest(client, message);
            }
        }

        return next.handleRequest(client, message);
    }
}
