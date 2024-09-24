package ru.rozhdestveno.taxi.service.hadler.employee.common;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.entity.feedback.FeedbackRepository;
import ru.rozhdestveno.taxi.entity.lost.LostRepository;
import ru.rozhdestveno.taxi.entity.util.ClientRequest;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import static ru.rozhdestveno.taxi.constants.Constants.WRONG_FORMAT_TEXT;

@Component
public class ReportTextHandler extends EmployeeHandler {

    private final EmployeeRepository employeeRepository;
    private final LostRepository lostRepository;
    private final FeedbackRepository feedbackRepository;

    private static Long lostReport = 1L;
    private static Long feedbackReport = 1L;

    public ReportTextHandler(EmployeeRepository employeeRepository, LostRepository lostRepository,
                             FeedbackRepository feedbackRepository) {
        this.employeeRepository = employeeRepository;
        this.lostRepository = lostRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getState().equals(EmployeeState.LOST_YEAR_REPORT) ||
                employee.getState().equals(EmployeeState.LOST_MONTH_REPORT) ||
                employee.getState().equals(EmployeeState.FEEDBACK_YEAR_REPORT) ||
                employee.getState().equals(EmployeeState.FEEDBACK_MONTH_REPORT);

        if (next != null && !condition) {
            return next.handleRequest(employee, message);
        }

        int year, month;
        LocalDate start, end;
        List<? extends ClientRequest> list;
        String fileName;

        try {
            if (employee.getState().equals(EmployeeState.LOST_YEAR_REPORT) //годовые отчеты
                    || employee.getState().equals(EmployeeState.FEEDBACK_YEAR_REPORT)) {
                year = Integer.parseInt(message);
                start = LocalDate.of(year, 1, 1);
                end = LocalDate.of(year, 12, 31);
            } else { //месячные отчеты
                String[] tmp = message.split(" ");//преобразование строки вида "2024 9"
                year = Integer.parseInt(tmp[0]);
                month = Integer.parseInt(tmp[1]);
                YearMonth yearMonth = YearMonth.of(year, month);
                start = LocalDate.of(year, month, 1);
                end = LocalDate.of(year, month, yearMonth.atEndOfMonth().getDayOfMonth());
            }
        } catch (PatternSyntaxException | NumberFormatException | IndexOutOfBoundsException e) {
            return List.of(BotUtil.createMessage(employee.getId(), WRONG_FORMAT_TEXT));
        }

        if (employee.getState().equals(EmployeeState.LOST_YEAR_REPORT) ||
                employee.getState().equals(EmployeeState.LOST_MONTH_REPORT)) {
            list = lostRepository.findByPeriod(start, end);
            fileName = "lostReport_" + LocalDate.now() + "_" + (lostReport++) + ".xlsx";
        } else {
            list = feedbackRepository.findByPeriod(start, end);
            fileName = "feedbackReport_" + LocalDate.now() + "_" + (feedbackReport++) + ".xlsx";
        }

        employee.setState(EmployeeState.EMPLOYEE_ON_DUTY);
        employeeRepository.saveAndFlush(employee);

        if (list.isEmpty()) {
            return List.of(BotUtil.createMessage(employee.getId(), "За выбранный период нет данных"));
        }

        return List.of(BotUtil.createDocument(employee.getId(), BotUtil.createExcelReport(list, message, fileName)));
    }
}
