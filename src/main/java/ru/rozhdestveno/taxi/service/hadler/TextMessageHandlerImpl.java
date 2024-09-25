package ru.rozhdestveno.taxi.service.hadler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.car.CarRepository;
import ru.rozhdestveno.taxi.entity.contact.CustomContactRepository;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.entity.customer.CustomerRepository;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.feedback.FeedbackRepository;
import ru.rozhdestveno.taxi.entity.lost.LostRepository;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.service.hadler.customer.BanCheckHandler;
import ru.rozhdestveno.taxi.service.hadler.customer.CustomerHandler;
import ru.rozhdestveno.taxi.service.hadler.customer.DefaultCustomerErrorHandler;
import ru.rozhdestveno.taxi.service.hadler.customer.text.CustomerFeedbackTextHandler;
import ru.rozhdestveno.taxi.service.hadler.customer.text.CustomerLostTextHandler;
import ru.rozhdestveno.taxi.service.hadler.customer.text.CustomerOrderTextHandler;
import ru.rozhdestveno.taxi.service.hadler.customer.text.CustomerSendMessageHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.admin.text.AdminCarHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.admin.text.AdminCashHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.admin.text.AdminContactHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.admin.text.AdminEmployeeHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.common.DayOffCheckHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.common.DefaultEmployeeErrorHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.common.ReportTextHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.dispatcher.text.DispatcherSetPriceTextHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.driver.text.DriverBoardingTextHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.driver.text.DriverSendMessageTextHandler;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TextMessageHandlerImpl implements TextMessageHandler {

    private final CarRepository carRepository;
    private final CustomContactRepository contactRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final FeedbackRepository feedbackRepository;
    private final LostRepository lostRepository;
    private final OrderRepository orderRepository;

    public List<PartialBotApiMethod<?>> handleText(Long chatId, String message) {
        Optional<Employee> employeeOpt = employeeRepository.findById(chatId);
        if (employeeOpt.isPresent()) {
            return handleEmployeeText(employeeOpt.get(), message);
        } else {
            Optional<Customer> optional = customerRepository.findById(chatId);
            Customer client = optional.isEmpty() ? registerCustomer(chatId) : optional.get();
            return handleClientText(client, message);
        }
    }

    private List<PartialBotApiMethod<?>> handleClientText(Customer client, String message) {
        CustomerHandler banCheckHandler = new BanCheckHandler(customerRepository);
        CustomerHandler orderHandler = new CustomerOrderTextHandler(orderRepository, employeeRepository);
        CustomerHandler lostHandler = new CustomerLostTextHandler(customerRepository, employeeRepository, lostRepository);
        CustomerHandler feedbackHandler = new CustomerFeedbackTextHandler(customerRepository, feedbackRepository);
        CustomerHandler messageHandler = new CustomerSendMessageHandler(orderRepository);
        CustomerHandler errorHandler = new DefaultCustomerErrorHandler();

        banCheckHandler.setNextHandler(orderHandler);
        orderHandler.setNextHandler(lostHandler);
        lostHandler.setNextHandler(feedbackHandler);
        feedbackHandler.setNextHandler(messageHandler);
        messageHandler.setNextHandler(errorHandler);
        return banCheckHandler.handleRequest(client, message);
    }

    private List<PartialBotApiMethod<?>> handleEmployeeText(Employee employee, String message) {
        //COMMON
        EmployeeHandler dayOffCheckHandler = new DayOffCheckHandler();
        EmployeeHandler reportHandler = new ReportTextHandler(employeeRepository, lostRepository, feedbackRepository);
        EmployeeHandler errorHandler = new DefaultEmployeeErrorHandler();

        //ADMIN
        EmployeeHandler adminCarHandler = new AdminCarHandler(employeeRepository, carRepository);
        EmployeeHandler adminCashHandler = new AdminCashHandler(employeeRepository, orderRepository);
        EmployeeHandler adminContactHandler = new AdminContactHandler(contactRepository, employeeRepository);
        EmployeeHandler employeeHandler = new AdminEmployeeHandler(employeeRepository);

        //DISPATCHER
        EmployeeHandler dispatcherPriceHandler = new DispatcherSetPriceTextHandler(orderRepository, employeeRepository);

        //DRIVER
        EmployeeHandler driverBoardingHandler = new DriverBoardingTextHandler(orderRepository,
                employeeRepository, carRepository);
        EmployeeHandler driverMessageHandler = new DriverSendMessageTextHandler(orderRepository);

        //common
        dayOffCheckHandler.setNextHandler(reportHandler);

        //admin
        reportHandler.setNextHandler(adminCarHandler);
        adminCarHandler.setNextHandler(adminCashHandler);
        adminCashHandler.setNextHandler(adminContactHandler);
        adminContactHandler.setNextHandler(employeeHandler);

        //dispatcher
        employeeHandler.setNextHandler(dispatcherPriceHandler);

        //driver
        dispatcherPriceHandler.setNextHandler(driverBoardingHandler);
        driverBoardingHandler.setNextHandler(driverMessageHandler);

        //common
        driverMessageHandler.setNextHandler(errorHandler);
        return dayOffCheckHandler.handleRequest(employee, message);
    }

    private Customer registerCustomer(long chatId) {
        Customer customer = new Customer();
        customer.setId(chatId);
        customer.setRegisteredOn(LocalDate.now());
        return customerRepository.save(customer);
    }
}
