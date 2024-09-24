package ru.rozhdestveno.taxi.service.hadler.employee.admin.callback;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;

import static ru.rozhdestveno.taxi.constants.Constants.ADMIN_ADD_DELETE_VEHICLE_TEXT;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeStatus.ADMIN;

@Component
public class AdminCarCallbackHandler extends EmployeeHandler {
    private final EmployeeRepository repository;

    public AdminCarCallbackHandler(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(ADMIN) && message.equals("/add_delete_vehicle");

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        //изменение статуса администратора для обработки текстовых команд касающихся автомобилей
        employee.setState(EmployeeState.ADMIN_ADD_DELETE_VEHICLE);
        repository.saveAndFlush(employee);
        return List.of(BotUtil.createMessage(employee.getId(), ADMIN_ADD_DELETE_VEHICLE_TEXT));
    }
}
