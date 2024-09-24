package ru.rozhdestveno.taxi.service.hadler.employee.admin.text;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.contact.CustomContact;
import ru.rozhdestveno.taxi.entity.contact.CustomContactRepository;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static ru.rozhdestveno.taxi.constants.Constants.PHONE_REGEX;
import static ru.rozhdestveno.taxi.constants.Constants.WRONG_FORMAT_TEXT;

@Component
public class AdminContactHandler extends EmployeeHandler {

    private final CustomContactRepository contactRepository;
    private final EmployeeRepository employeeRepository;

    public AdminContactHandler(CustomContactRepository contactRepository, EmployeeRepository employeeRepository) {
        this.contactRepository = contactRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.ADMIN) &&
                employee.getState().equals(EmployeeState.ADMIN_ADD_DELETE_CONTACT);

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();
        String command, phoneNumber;
        CustomContact contact;

        try {
            //разбиение строки вида "Добавить 8 (999) 888-77-66" или "удалить 8 (999) 888-77-66"
            //на "добавить/удалить" и "8 (999) 888-77-66"
            String[] tmp = message.split(" ", 2);
            command = tmp[0].toLowerCase();
            phoneNumber = tmp[1];
            Pattern pattern = Pattern.compile(PHONE_REGEX); //паттерн на соответствие номера телефона
            Matcher phoneMatcher = pattern.matcher(phoneNumber);

            if (!phoneMatcher.matches()) {
                //при несовпадении паттерна, высылается текст о некорректности формата введенных данных
                return List.of(BotUtil.createMessage(employee.getId(), WRONG_FORMAT_TEXT));
            }

            contact = contactRepository.findByPhoneNumber(phoneNumber);

            if (command.equals("удалить")) {
                if (contact == null) {
                    return List.of(BotUtil.createMessage(employee.getId(),
                            "Контакта нет в базе данных. Попробуйте еще раз."));
                }

                contactRepository.delete(contact);
                messages.add(BotUtil.createMessage(employee.getId(), "Контакт " + phoneNumber + " удален."));
            } else if (command.equals("добавить")) {
                if (contact != null) {
                    return List.of(BotUtil.createMessage(employee.getId(), "Контакт уже есть в базе данных."));
                }

                CustomContact newContact = new CustomContact();
                newContact.setPhoneNumber(phoneNumber);
                contactRepository.saveAndFlush(newContact);
                messages.add(BotUtil.createMessage(employee.getId(), "Контакт " + phoneNumber + " добавлен."));
            } else {
                return List.of(BotUtil.createMessage(employee.getId(), WRONG_FORMAT_TEXT));
            }
        } catch (PatternSyntaxException | NumberFormatException | IndexOutOfBoundsException e) {
            return List.of(BotUtil.createMessage(employee.getId(), WRONG_FORMAT_TEXT));
        }

        employee.setState(EmployeeState.EMPLOYEE_ON_DUTY);
        employeeRepository.saveAndFlush(employee);
        return messages;
    }
}
