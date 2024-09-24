package ru.rozhdestveno.taxi.service.hadler.employee;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;

import java.util.List;

public abstract class EmployeeHandler {
    protected EmployeeHandler next;

    public void setNextHandler(EmployeeHandler next) {
        this.next = next;
    }

    public abstract List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message);
}
