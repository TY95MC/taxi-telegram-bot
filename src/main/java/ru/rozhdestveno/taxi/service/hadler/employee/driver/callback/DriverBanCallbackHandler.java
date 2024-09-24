package ru.rozhdestveno.taxi.service.hadler.employee.driver.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.entity.customer.CustomerBanStatus;
import ru.rozhdestveno.taxi.entity.customer.CustomerRepository;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.entity.order.Order;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class DriverBanCallbackHandler extends EmployeeHandler {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public DriverBanCallbackHandler(OrderRepository orderRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DRIVER) && message.startsWith("/ban ");

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        long orderId = Long.parseLong(message.replace("/ban ", ""));
        Order order;
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isPresent()) {
            order = orderOpt.get();
        } else {
            log.error("Заказ с id: " + orderId + " не найден");
            return List.of(BotUtil.createMessage(employee.getId(), "Что-то пошло не так, заказ не найден"));
        }

        Customer customer = order.getClient();

        //повышение уровней предупреждения клиентам
        if (customer.getStatus().equals(CustomerBanStatus.SECOND_WARN)) {
            //защита от более чем однократного повышения уровня предупреждения клиента
            if (customer.getSecondWarn().isAfter(LocalDate.now())) {
                return List.of(BotUtil.createMessage(employee.getId(),
                        "Невозможно выполнить действие до: " + customer.getSecondWarn()));
            }

            customer.setStatus(CustomerBanStatus.BANNED);
            customer.setBannedOn(LocalDate.now());
            customerRepository.saveAndFlush(customer);
            log.info("Клиенту {} вынесено предупреждение {} водителем {}",
                    customer.getId(), CustomerBanStatus.BANNED, employee.getId());
            return List.of(BotUtil.createMessage(employee.getId(), "Пользователю <code>" + customer.getId()
                    + "</code> ограничен доступ до: " + customer.getBannedOn()));
        }

        if (customer.getStatus().equals(CustomerBanStatus.FIRST_WARN)) {
            //защита от более чем однократного повышения уровня предупреждения клиента
            if (customer.getFirstWarn().isAfter(LocalDate.now())) {
                return List.of(BotUtil.createMessage(employee.getId(),
                        "Невозможно выполнить действие до: " + customer.getFirstWarn()));
            }

            customer.setStatus(CustomerBanStatus.SECOND_WARN);
            customer.setSecondWarn(LocalDate.now().plusMonths(1));
            customerRepository.saveAndFlush(customer);
            log.info("Клиенту {} вынесено предупреждение {} водителем {}",
                    customer.getId(), CustomerBanStatus.SECOND_WARN, employee.getId());
            return List.of(BotUtil.createMessage(employee.getId(), "Пользователю <code>" + customer.getId()
                    + "</code> ограничен доступ до " + customer.getSecondWarn()));
        }

        if (customer.getStatus().equals(CustomerBanStatus.NO_WARN)) {
            customer.setStatus(CustomerBanStatus.FIRST_WARN);
            customer.setFirstWarn(LocalDate.now().plusDays(7));
            customerRepository.saveAndFlush(customer);
            log.info("Клиенту {} вынесено предупреждение {} водителем {}",
                    customer.getId(), CustomerBanStatus.FIRST_WARN, employee.getId());
            return List.of(BotUtil.createMessage(employee.getId(), "Пользователю <code>" + customer.getId()
                    + "</code> ограничен доступ до " + customer.getFirstWarn()));
        }

        return null;
    }
}
