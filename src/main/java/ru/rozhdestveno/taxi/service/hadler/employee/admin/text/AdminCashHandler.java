package ru.rozhdestveno.taxi.service.hadler.employee.admin.text;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static ru.rozhdestveno.taxi.constants.Constants.WRONG_FORMAT_TEXT;

@Slf4j
@Component
public class AdminCashHandler extends EmployeeHandler {

    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;

    public AdminCashHandler(EmployeeRepository employeeRepository, OrderRepository orderRepository) {
        this.employeeRepository = employeeRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.ADMIN) &&
                employee.getState().equals(EmployeeState.ADMIN_GET_CASH);

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        LocalDate date;

        try {
            //преобразование даты из сообщения вида "2024 09"
            //формат сделан таковым для удобства пользователя при введении с экранной клавиатуры
            date = LocalDate.parse(message.replace(" ", "-"));
        } catch (DateTimeParseException | IndexOutOfBoundsException e) {
            return List.of(BotUtil.createMessage(employee.getId(), WRONG_FORMAT_TEXT));
        }

        Integer sumComplete = orderRepository.getSumCompleteByDate(date);//сумма успешно завершенных заказов
        Integer sumCancel = orderRepository.getSumCancelByDate(date);//сумма отмененных заказов

        if (sumComplete == null && sumCancel == null) {
            log.info("Заказы по дате {} не найдены", date);
            return List.of(BotUtil.createMessage(employee.getId(), String.format("Заказы по дате %s не найдены", date)));
        }

        if (sumComplete == null) {
            sumComplete = 0;
        }

        if (sumCancel == null) {
            sumCancel = 0;
        }

        employee.setState(EmployeeState.EMPLOYEE_ON_DUTY);
        employeeRepository.saveAndFlush(employee);

        return List.of(BotUtil.createMessage(employee.getId(),
                "Выполнено заказов на сумму %d" + sumComplete + "\nОтменено заказов на сумму %d" + sumCancel));
    }
}
