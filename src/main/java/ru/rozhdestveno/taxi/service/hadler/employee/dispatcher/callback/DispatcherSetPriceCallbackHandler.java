package ru.rozhdestveno.taxi.service.hadler.employee.dispatcher.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.entity.order.Order;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;
import java.util.Optional;

import static ru.rozhdestveno.taxi.constants.Constants.DISPATCHER_SET_PRICE_TEXT;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeState.DISPATCHER_SET_PRICE;

@Component
@Slf4j
public class DispatcherSetPriceCallbackHandler extends EmployeeHandler {

    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;

    public DispatcherSetPriceCallbackHandler(EmployeeRepository employeeRepository, OrderRepository orderRepository) {
        this.employeeRepository = employeeRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DISPATCHER) && message.startsWith("/set_price ");

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        String orderId = message.replace("/set_price ", "");
        Optional<Order> optional = orderRepository.findById(Long.parseLong(orderId));

        if (optional.isEmpty()) {
            return List.of(BotUtil.createMessage(employee.getId(), "Что-то пошло не так"));//если заказ не найден
        } else {
            employee.setState(DISPATCHER_SET_PRICE);
            employeeRepository.saveAndFlush(employee);
            log.info("Изменение диспетчером {} цены заказа {}, старая цена {}",
                    employee.getId(), optional.get().getId(), optional.get().getPrice());
            return List.of(BotUtil.createMessage(employee.getId(), String.format(DISPATCHER_SET_PRICE_TEXT, orderId)));
        }
    }
}
