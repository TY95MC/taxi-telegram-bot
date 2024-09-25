package ru.rozhdestveno.taxi.service.hadler.employee.admin.text;

import lombok.extern.slf4j.Slf4j;
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
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

import static ru.rozhdestveno.taxi.constants.Constants.WRONG_FORMAT_TEXT;

@Component
@Slf4j
public class AdminEmployeeHandler extends EmployeeHandler {

    private final EmployeeRepository employeeRepository;

    public AdminEmployeeHandler(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.ADMIN) &&
                (employee.getState().equals(EmployeeState.ADMIN_ADD_DELETE_ADMIN) ||
                        employee.getState().equals(EmployeeState.ADMIN_ADD_DELETE_DISPATCHER) ||
                        employee.getState().equals(EmployeeState.ADMIN_ADD_DELETE_DRIVER));

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();
        EmployeeStatus status = switch (employee.getState()) {
            case ADMIN_ADD_DELETE_ADMIN -> EmployeeStatus.ADMIN;
            case ADMIN_ADD_DELETE_DISPATCHER -> EmployeeStatus.DISPATCHER;
            case ADMIN_ADD_DELETE_DRIVER -> EmployeeStatus.DRIVER;
            default -> null;
        };

        String[] tmp;
        String firstName, lastName;
        long newEmployeeId;

        try {
            //преобразование строки вида "Иван Иванов, 999999999", "удалить сотрудника, 999999999"
            tmp = message.split(",");
            newEmployeeId = Long.parseLong(tmp[1].trim());
            tmp = tmp[0].trim().split(" ");//преобразование строки "Иван Иванов" или "удалить сотрудника"
            firstName = tmp[0];
            lastName = tmp[1];
        } catch (PatternSyntaxException | NumberFormatException | IndexOutOfBoundsException e) {
            return List.of(BotUtil.createMessage(employee.getId(), WRONG_FORMAT_TEXT));
        }

        Optional<Employee> optional = employeeRepository.findById(newEmployeeId);

        if (optional.isEmpty()) { //добавление нового сотрудника
            Employee newEmployee = new Employee();
            newEmployee.setId(newEmployeeId);
            newEmployee.setFirstName(firstName);
            newEmployee.setLastName(lastName);
            newEmployee.setStatus(status);
            employeeRepository.saveAndFlush(newEmployee);
            messages.add(BotUtil.createMessage(employee.getId(), "Сотрудник добавлен!"));
            log.info("{} добавляет {} {}", employee.getId(), status, newEmployeeId);
        } else {
            if (status.equals(EmployeeStatus.ADMIN)
                    && employeeRepository.findAllByStatus(EmployeeStatus.ADMIN).size() == 1) {
                return List.of(BotUtil.createMessage(employee.getId(), "Нельзя удалить последнего администратора"));
            }

            employeeRepository.delete(optional.get());
            messages.add(BotUtil.createMessage(employee.getId(), "Сотрудник удален!"));
            log.info("{} удаляет {} {}", employee.getId(), status, newEmployeeId);
        }

        employee.setState(EmployeeState.EMPLOYEE_ON_DUTY);
        employeeRepository.saveAndFlush(employee);
        return messages;
    }
}
