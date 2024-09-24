package ru.rozhdestveno.taxi.service.hadler.employee.admin.callback;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;

import static ru.rozhdestveno.taxi.entity.employee.EmployeeStatus.ADMIN;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeStatus.DISPATCHER;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeStatus.DRIVER;

@Component
public class AdminEmployeeInfoCallbackHandler extends EmployeeHandler {
    private final EmployeeRepository repository;

    public AdminEmployeeInfoCallbackHandler(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(ADMIN) &&
                (message.equals("/get_admins") || message.equals("/get_dispatchers")
                        || message.equals("/get_drivers"));

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        EmployeeStatus status = switch (message) {
            case "/get_admins" -> ADMIN;
            case "/get_dispatchers" -> DISPATCHER;
            case "/get_drivers" -> DRIVER;
            default -> null;
        };

        //получение списка персонала с соответствующим статусом
        List<Employee> list = repository.findAllByStatus(status);

        if (list.size() == 0) {
            return List.of(BotUtil.createMessage(employee.getId(), "Список сотрудников пуст!"));
        }

        StringBuilder sb = new StringBuilder();

        //формирование сообщения с именем, фамилией и id сотрудника
        for (Employee emp : list) {
            sb.append(emp.getFirstName()).append(" ").append(emp.getLastName())
                    .append(", id: <code>").append(emp.getId()).append("</code>\n");
        }

        return List.of(BotUtil.createMessage(employee.getId(), sb.toString()));
    }
}
