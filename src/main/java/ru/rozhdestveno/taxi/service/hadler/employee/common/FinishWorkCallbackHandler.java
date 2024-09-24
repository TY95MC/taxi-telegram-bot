package ru.rozhdestveno.taxi.service.hadler.employee.common;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;

import static ru.rozhdestveno.taxi.entity.employee.EmployeeState.EMPLOYEE_ON_WEEKEND;

@Component
public class FinishWorkCallbackHandler extends EmployeeHandler {
    private final EmployeeRepository employeeRepository;

    public FinishWorkCallbackHandler(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        if (next != null && !(message.equals("/finish_work"))) {
            return next.handleRequest(employee, message);
        }

        employee.setState(EMPLOYEE_ON_WEEKEND);
        employeeRepository.saveAndFlush(employee);
        return List.of(BotUtil.createMessage(employee.getId(), "Смена закрыта"));
    }
}
