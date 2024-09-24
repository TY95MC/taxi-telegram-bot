package ru.rozhdestveno.taxi.service.hadler.employee.driver.callback;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.ArrayList;
import java.util.List;

import static ru.rozhdestveno.taxi.entity.employee.EmployeeState.EMPLOYEE_ON_DUTY;

@Component
public class DriverBreakCallbackHandler extends EmployeeHandler {
    private final EmployeeRepository employeeRepository;

    public DriverBreakCallbackHandler(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DRIVER) && message.equals("/break");

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();

        if (employee.getState().equals(EMPLOYEE_ON_DUTY)) {
            employee.setState(EmployeeState.DRIVER_ON_PAUSE);
            messages.add(BotUtil.createMessage(employee.getId(), "Перерыв начат"));
        } else {
            employee.setState(EMPLOYEE_ON_DUTY);
            messages.add(BotUtil.createMessage(employee.getId(), "Перерыв окончен"));
        }

        employeeRepository.saveAndFlush(employee);
        return messages;
    }
}
