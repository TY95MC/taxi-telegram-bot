package ru.rozhdestveno.taxi.service.hadler.employee.common;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;

import static ru.rozhdestveno.taxi.constants.Constants.WEEKEND_TEXT;

/**
 * Always should be thirst handler in chain
 */
@Component
public class DayOffCheckHandler extends EmployeeHandler {
    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        if (employee.getState() != null && employee.getState().equals(EmployeeState.EMPLOYEE_ON_WEEKEND)) {
            return List.of(BotUtil.createMessage(employee.getId(), WEEKEND_TEXT));
        }

        return next.handleRequest(employee, message);
    }
}
