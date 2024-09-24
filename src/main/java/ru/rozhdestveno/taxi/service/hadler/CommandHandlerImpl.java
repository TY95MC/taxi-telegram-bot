package ru.rozhdestveno.taxi.service.hadler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.contact.CustomContact;
import ru.rozhdestveno.taxi.entity.contact.CustomContactRepository;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.entity.customer.CustomerRepository;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ru.rozhdestveno.taxi.constants.Constants.ADMIN_HELP_COMMAND_TEXT;
import static ru.rozhdestveno.taxi.constants.Constants.ADMIN_START_MENU;
import static ru.rozhdestveno.taxi.constants.Constants.CLIENT_START_MENU;
import static ru.rozhdestveno.taxi.constants.Constants.CUSTOMER_HELP_COMMAND_TEXT;
import static ru.rozhdestveno.taxi.constants.Constants.DISPATCHER_HELP_COMMAND_TEXT;
import static ru.rozhdestveno.taxi.constants.Constants.DISPATCHER_START_MENU;
import static ru.rozhdestveno.taxi.constants.Constants.DRIVER_HELP_COMMAND_TEXT;
import static ru.rozhdestveno.taxi.constants.Constants.DRIVER_START_MENU;
import static ru.rozhdestveno.taxi.entity.customer.CustomerState.START;

@Component
@RequiredArgsConstructor
public class CommandHandlerImpl implements CommandHandler {

    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomContactRepository contactRepository;

    @Override
    public List<PartialBotApiMethod<?>> handleCommand(Long chatId, String command) {
        if (command.equals("/contacts")) {
            return List.of(BotUtil.createMessage(chatId, getContacts()));
        }

        Optional<Employee> employeeOpt = employeeRepository.findById(chatId);

        if (employeeOpt.isPresent()) {
            return handleEmployeeCommand(employeeOpt.get(), command);
        } else {
            Optional<Customer> optional = customerRepository.findById(chatId);
            Customer client = optional.isEmpty() ? registerCustomer(chatId) : optional.get();
            return handleCustomerCommand(client, command);
        }
    }

    private List<PartialBotApiMethod<?>> handleEmployeeCommand(Employee employee, String command) {
        String response = "Здравствуйте " + employee.getFirstName() + "!";
        if (command.equals("/start")) {
            Map<String, String> menu = switch (employee.getStatus()) {
                case ADMIN -> ADMIN_START_MENU;
                case DISPATCHER -> DISPATCHER_START_MENU;
                case DRIVER -> DRIVER_START_MENU;
            };
            employee.setState(EmployeeState.EMPLOYEE_ON_DUTY);
            employeeRepository.saveAndFlush(employee);
            return List.of(BotUtil.createMessage(employee.getId(), response, menu));
        }

        if (command.equals("/help")) {
            response = switch (employee.getStatus()) {
                case ADMIN -> ADMIN_HELP_COMMAND_TEXT;
                case DISPATCHER -> DISPATCHER_HELP_COMMAND_TEXT;
                case DRIVER -> DRIVER_HELP_COMMAND_TEXT;
            };
        }

        return List.of(BotUtil.createMessage(employee.getId(), response));
    }

    private List<PartialBotApiMethod<?>> handleCustomerCommand(Customer client, String command) {
        String response = "Здравствуйте!";
        if (command.equals("/start")) {
            client.setState(START);
            customerRepository.saveAndFlush(client);
            return List.of(BotUtil.createMessage(client.getId(), response, CLIENT_START_MENU));
        }

        if (command.equals("/help")) {
            response = CUSTOMER_HELP_COMMAND_TEXT;
        }

        return List.of(BotUtil.createMessage(client.getId(), response));
    }

    private String getContacts() {
        List<CustomContact> contacts = contactRepository.getAllContacts();

        if (contacts.isEmpty()) {
            return "Список контактов пуст!";
        }

        StringBuilder stringBuilder = new StringBuilder();
        contacts.stream()
                .map(CustomContact::getPhoneNumber)
                .forEach(contact -> stringBuilder.append(contact).append("\n"));

        return stringBuilder.toString();
    }

    private Customer registerCustomer(long chatId) {
        Customer customer = new Customer();
        customer.setId(chatId);
        customer.setRegisteredOn(LocalDate.now());
        return customerRepository.save(customer);
    }
}
