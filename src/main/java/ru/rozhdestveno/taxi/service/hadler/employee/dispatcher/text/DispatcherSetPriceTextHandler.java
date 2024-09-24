package ru.rozhdestveno.taxi.service.hadler.employee.dispatcher.text;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.entity.order.Order;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

import static ru.rozhdestveno.taxi.constants.Constants.WRONG_FORMAT_TEXT;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeState.EMPLOYEE_ON_DUTY;

@Component
@Slf4j
public class DispatcherSetPriceTextHandler extends EmployeeHandler {
    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;

    public DispatcherSetPriceTextHandler(OrderRepository orderRepository, EmployeeRepository employeeRepository) {
        this.orderRepository = orderRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DISPATCHER) &&
                employee.getState().equals(EmployeeState.DISPATCHER_SET_PRICE);

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        String[] tmp;
        long orderId;
        int price;

        try {
            tmp = message.split(" "); //преобразование строки вида "*номер заказа* *стоимость заказа*
            orderId = Long.parseLong(tmp[0]);
            price = Integer.parseInt(tmp[1]);
        } catch (PatternSyntaxException | NumberFormatException | IndexOutOfBoundsException e) {
            return List.of(BotUtil.createMessage(employee.getId(), WRONG_FORMAT_TEXT));
        }

        Order order;
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isPresent()) {
            order = orderOpt.get();
        } else {
            log.error("Заказ с id: " + orderId + " не найден");
            return List.of(BotUtil.createMessage(employee.getId(), "Что-то пошло не так, заказ не найден"));
        }

        order.setPrice(price);
        orderRepository.saveAndFlush(order);
        employee.setState(EMPLOYEE_ON_DUTY);
        employeeRepository.saveAndFlush(employee);
        log.info("Изменение диспетчером {} цены заказа {}, новая цена {}",
                employee.getId(), order.getId(), order.getPrice());
        return List.of(BotUtil.createMessage(employee.getId(), "Цена установлена"));
    }
}
