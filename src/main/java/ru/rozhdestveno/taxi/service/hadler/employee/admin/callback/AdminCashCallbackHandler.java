package ru.rozhdestveno.taxi.service.hadler.employee.admin.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.time.LocalDate;
import java.util.List;

import static ru.rozhdestveno.taxi.constants.Constants.REPORT_BY_DATE_TEXT;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeStatus.ADMIN;

@Component
@Slf4j
public class AdminCashCallbackHandler extends EmployeeHandler {

    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;

    public AdminCashCallbackHandler(OrderRepository orderRepository, EmployeeRepository employeeRepository) {
        this.orderRepository = orderRepository;
        this.employeeRepository = employeeRepository;
    }


    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(ADMIN) &&
                (message.equals("/report_by_date") || message.equals("/report_today"));

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        //установка статуса администратора для обработки текстового сообщения,
        //содержащего дату, за которую администратор хочет получить данные
        if (message.equals("/report_by_date")) {
            employee.setState(EmployeeState.ADMIN_GET_CASH);
            employeeRepository.saveAndFlush(employee);
            return List.of(BotUtil.createMessage(employee.getId(), REPORT_BY_DATE_TEXT));
        }

        //обработка данных по заказам за текущий день
        Integer sumComplete = orderRepository.getSumCompleteByDate(LocalDate.now());
        Integer sumCancel = orderRepository.getSumCancelByDate(LocalDate.now());

        if (sumComplete == null && sumCancel == null) {
            log.info("Заказы по дате {} не найдены", LocalDate.now());
            return List.of(BotUtil.createMessage(employee.getId(),
                    String.format("Заказы по дате %s не найдены", LocalDate.now())));
        }

        if (sumComplete == null) {
            sumComplete = 0;
        }

        if (sumCancel == null) {
            sumCancel = 0;
        }

        return List.of(BotUtil.createMessage(employee.getId(),
                "Выполнено заказов на сумму %d" + sumComplete + "\nОтменено заказов на сумму %d" + sumCancel));
    }
}
