package ru.rozhdestveno.taxi.service.hadler.employee.admin.callback;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.contact.CustomContact;
import ru.rozhdestveno.taxi.entity.contact.CustomContactRepository;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;

import static ru.rozhdestveno.taxi.constants.Constants.ADMIN_ADD_DELETE_CONTACT_TEXT;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeStatus.ADMIN;

@Component
public class AdminContactCallbackHandler extends EmployeeHandler {
    private final EmployeeRepository repository;
    private final CustomContactRepository contactRepository;

    public AdminContactCallbackHandler(EmployeeRepository repository, CustomContactRepository contactRepository) {
        this.repository = repository;
        this.contactRepository = contactRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(ADMIN) &&
                (message.equals("/add_delete_contact") || message.equals("/get_contacts"));

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        //получение списка контактов
        if (message.equals("/get_contacts")) {
            List<CustomContact> contacts = contactRepository.getAllContacts();

            if (contacts.isEmpty()) {
                return List.of(BotUtil.createMessage(employee.getId(), "Список контактов пуст!"));
            }

            StringBuilder sb = new StringBuilder();
            //формирование сообщения с номером контакта
            contacts.stream()
                    .map(CustomContact::getPhoneNumber)
                    .forEach(contact -> sb.append(contact).append("\n"));

            return List.of(BotUtil.createMessage(employee.getId(), sb.toString()));
        }

        //изменение статуса администратора для обработки текстовых команд касающихся контактов
        employee.setState(EmployeeState.ADMIN_ADD_DELETE_CONTACT);
        repository.saveAndFlush(employee);
        return List.of(BotUtil.createMessage(employee.getId(), ADMIN_ADD_DELETE_CONTACT_TEXT));
    }
}
