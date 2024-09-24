package ru.rozhdestveno.taxi.service.hadler.employee.common;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;

import static ru.rozhdestveno.taxi.constants.Constants.DEFAULT_ERROR_MESSAGE;

/**
 * Always should be last handler in chain
 */
@Component
public class DefaultEmployeeErrorHandler extends EmployeeHandler {
    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        return List.of(BotUtil.createMessage(employee.getId(), DEFAULT_ERROR_MESSAGE));
    }
}
