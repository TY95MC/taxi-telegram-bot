package ru.rozhdestveno.taxi.service.hadler.employee.common;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;

import static ru.rozhdestveno.taxi.constants.Constants.FEEDBACK_REPORT_MENU;
import static ru.rozhdestveno.taxi.constants.Constants.LOST_REPORT_MENU;
import static ru.rozhdestveno.taxi.constants.Constants.REPORT_BY_MONTH_TEXT;
import static ru.rozhdestveno.taxi.constants.Constants.REPORT_BY_YEAR_TEXT;

@Component
public class ReportCallbackHandler extends EmployeeHandler {

    private final EmployeeRepository employeeRepository;

    public ReportCallbackHandler(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = message.equals("/get_lost") || message.equals("/get_feedback") ||
                message.equals("/lost_report_by_year") || message.equals("/lost_report_by_month") ||
                message.equals("/feedback_report_by_year") || message.equals("/feedback_report_by_month");

        if (next != null && !condition) {
            return next.handleRequest(employee, message);
        }

        if (message.equals("/get_lost")) {
            return List.of(BotUtil.createMessage(employee.getId(), "Выберите период отчета", LOST_REPORT_MENU));
        }

        if (message.equals("/get_feedback")) {
            return List.of(BotUtil.createMessage(employee.getId(), "Выберите период отчета", FEEDBACK_REPORT_MENU));
        }

        EmployeeState state = null;
        String msg = REPORT_BY_YEAR_TEXT; //значение текста сообщения по умолчанию

        //установление статуса(и текста сообщения) для последующей обработки текстового сообщения
        switch (message) {
            case "/lost_report_by_year" -> state = EmployeeState.LOST_YEAR_REPORT;
            case "/lost_report_by_month" -> {
                state = EmployeeState.LOST_MONTH_REPORT;
                msg = REPORT_BY_MONTH_TEXT;
            }
            case "/feedback_report_by_year" -> state = EmployeeState.FEEDBACK_YEAR_REPORT;
            case "/feedback_report_by_month" -> {
                state = EmployeeState.FEEDBACK_MONTH_REPORT;
                msg = REPORT_BY_MONTH_TEXT;
            }
        }

        employee.setState(state);
        employeeRepository.saveAndFlush(employee);
        return List.of(BotUtil.createMessage(employee.getId(), msg));
    }
}
