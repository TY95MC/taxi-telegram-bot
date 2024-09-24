package ru.rozhdestveno.taxi.service.hadler.employee.common;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;
import java.util.Map;

import static ru.rozhdestveno.taxi.constants.Constants.ADMIN_START_MENU;
import static ru.rozhdestveno.taxi.constants.Constants.DISPATCHER_START_MENU;

@Component
public class BackCallbackHandler extends EmployeeHandler {
    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        if (!message.equals("/back") && next != null) {
            return next.handleRequest(employee, message);
        }

        Map<String, String> menu = switch (employee.getStatus()) {
            case ADMIN -> ADMIN_START_MENU;
            case DISPATCHER -> DISPATCHER_START_MENU;
            default -> null;
        };

        return List.of(BotUtil.createMessage(employee.getId(), "Главное меню", menu));
    }
}
