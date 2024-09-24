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
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.service.hadler.customer.BanCheckHandler;
import ru.rozhdestveno.taxi.service.hadler.customer.CustomerHandler;
import ru.rozhdestveno.taxi.service.hadler.customer.DefaultCustomerErrorHandler;
import ru.rozhdestveno.taxi.service.hadler.customer.callback.CustomerCancelOrderCallbackDataHandler;
import ru.rozhdestveno.taxi.service.hadler.customer.callback.CustomerFeedbackCallbackDataHandler;
import ru.rozhdestveno.taxi.service.hadler.customer.callback.CustomerLostCallbackDataHandler;
import ru.rozhdestveno.taxi.service.hadler.customer.callback.CustomerOderCallbackDataHandler;
import ru.rozhdestveno.taxi.service.hadler.customer.callback.CustomerSendMessageCallbackDataHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.admin.callback.AdminCarCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.admin.callback.AdminCarInfoCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.admin.callback.AdminCashCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.admin.callback.AdminContactCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.admin.callback.AdminEmployeeCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.admin.callback.AdminEmployeeInfoCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.common.BackCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.common.DayOffCheckHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.common.DefaultEmployeeErrorHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.common.FinishWorkCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.common.ReportCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.dispatcher.callback.DispatcherDriversCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.dispatcher.callback.DispatcherSetPriceCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.driver.callback.DriverAcceptOrderCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.driver.callback.DriverArrivedCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.driver.callback.DriverBanCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.driver.callback.DriverBoardingCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.driver.callback.DriverBreakCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.driver.callback.DriverCancelOrderCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.driver.callback.DriverCarCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.driver.callback.DriverCloseToClientCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.driver.callback.DriverFinishRideCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.driver.callback.DriverPriceCallbackHandler;
import ru.rozhdestveno.taxi.service.hadler.employee.driver.callback.DriverSendMessageCallbackHandler;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CallbackDataHandlerImpl implements CallbackDataHandler {

    private final CarRepository carRepository;
    private final CustomContactRepository contactRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;

    public List<PartialBotApiMethod<?>> handleCallbackData(Long chatId, String data) {
        Optional<Employee> employeeOpt = employeeRepository.findById(chatId);
        if (employeeOpt.isPresent()) {
            return handleEmployeeCallbackData(employeeOpt.get(), data);
        } else {
            Optional<Customer> optional = customerRepository.findById(chatId);
            return handleCustomerCallbackData(optional.get(), data);
        }
    }

    private List<PartialBotApiMethod<?>> handleCustomerCallbackData(Customer client, String data) {
        CustomerHandler banCheckHandler = new BanCheckHandler(customerRepository);
        CustomerHandler orderHandler = new CustomerOderCallbackDataHandler(customerRepository);
        CustomerHandler lostHandler = new CustomerLostCallbackDataHandler(customerRepository);
        CustomerHandler feedbackHandler = new CustomerFeedbackCallbackDataHandler(customerRepository);
        CustomerHandler cancelHandler = new CustomerCancelOrderCallbackDataHandler(
                customerRepository, orderRepository, employeeRepository);
        CustomerHandler messageHandler = new CustomerSendMessageCallbackDataHandler(
                customerRepository, orderRepository, employeeRepository);
        CustomerHandler errorHandler = new DefaultCustomerErrorHandler();

        banCheckHandler.setNextHandler(orderHandler);
        orderHandler.setNextHandler(lostHandler);
        lostHandler.setNextHandler(feedbackHandler);
        feedbackHandler.setNextHandler(cancelHandler);
        cancelHandler.setNextHandler(messageHandler);
        messageHandler.setNextHandler(errorHandler);

        return banCheckHandler.handleRequest(client, data);
    }

    private List<PartialBotApiMethod<?>> handleEmployeeCallbackData(Employee employee, String data) {
        //COMMON Handlers
        EmployeeHandler dayOffCheckHandler = new DayOffCheckHandler();
        EmployeeHandler backHandler = new BackCallbackHandler();
        EmployeeHandler errorHandler = new DefaultEmployeeErrorHandler();
        EmployeeHandler reportHandler = new ReportCallbackHandler(employeeRepository);
        EmployeeHandler finishHandler = new FinishWorkCallbackHandler(employeeRepository);

        //ADMIN
        EmployeeHandler adminCarHandler = new AdminCarCallbackHandler(employeeRepository);
        EmployeeHandler adminCarInfoHandler = new AdminCarInfoCallbackHandler(carRepository);
        EmployeeHandler adminCashHandler = new AdminCashCallbackHandler(orderRepository, employeeRepository);
        EmployeeHandler adminContactHandler = new AdminContactCallbackHandler(employeeRepository, contactRepository);
        EmployeeHandler employeeHandler = new AdminEmployeeCallbackHandler(employeeRepository);
        EmployeeHandler employeeInfoHandler = new AdminEmployeeInfoCallbackHandler(employeeRepository);

        //DISPATCHER
        EmployeeHandler dispatcherDriversHandler =
                new DispatcherDriversCallbackHandler(carRepository, employeeRepository);
        EmployeeHandler dispatcherSetPriceHandler =
                new DispatcherSetPriceCallbackHandler(employeeRepository, orderRepository);

        //DRIVER
        EmployeeHandler acceptOrderHandler = new DriverAcceptOrderCallbackHandler(employeeRepository,
                orderRepository, carRepository);
        EmployeeHandler arrivedHandler = new DriverArrivedCallbackHandler(orderRepository);
        EmployeeHandler banHandler = new DriverBanCallbackHandler(orderRepository, customerRepository);
        EmployeeHandler boardingHandler = new DriverBoardingCallbackHandler(carRepository,
                orderRepository, employeeRepository);
        EmployeeHandler breakHandler = new DriverBreakCallbackHandler(employeeRepository);
        EmployeeHandler cancelOrderHandler = new DriverCancelOrderCallbackHandler(orderRepository,
                customerRepository, employeeRepository);
        EmployeeHandler carHandler = new DriverCarCallbackHandler(carRepository);
        EmployeeHandler closeToClientHandler = new DriverCloseToClientCallbackHandler(orderRepository);
        EmployeeHandler finishRideHandler = new DriverFinishRideCallbackHandler(orderRepository,
                customerRepository, employeeRepository);
        EmployeeHandler priceHandler = new DriverPriceCallbackHandler(employeeRepository,
                orderRepository, carRepository);
        EmployeeHandler messageHandler = new DriverSendMessageCallbackHandler(orderRepository,
                customerRepository, employeeRepository);

        //common
        dayOffCheckHandler.setNextHandler(backHandler);
        backHandler.setNextHandler(reportHandler);
        reportHandler.setNextHandler(finishHandler);
        finishHandler.setNextHandler(adminCarHandler);

        //admin
        adminCarHandler.setNextHandler(adminCarInfoHandler);
        adminCarInfoHandler.setNextHandler(adminCashHandler);
        adminCashHandler.setNextHandler(adminContactHandler);
        adminContactHandler.setNextHandler(employeeHandler);
        employeeHandler.setNextHandler(employeeInfoHandler);
        employeeInfoHandler.setNextHandler(dispatcherDriversHandler);

        //dispatcher
        dispatcherDriversHandler.setNextHandler(dispatcherSetPriceHandler);
        dispatcherSetPriceHandler.setNextHandler(acceptOrderHandler);

        //driver
        acceptOrderHandler.setNextHandler(arrivedHandler);
        arrivedHandler.setNextHandler(banHandler);
        banHandler.setNextHandler(boardingHandler);
        boardingHandler.setNextHandler(breakHandler);
        breakHandler.setNextHandler(cancelOrderHandler);
        cancelOrderHandler.setNextHandler(carHandler);
        carHandler.setNextHandler(closeToClientHandler);
        closeToClientHandler.setNextHandler(finishRideHandler);
        finishRideHandler.setNextHandler(priceHandler);
        priceHandler.setNextHandler(messageHandler);

        //common
        messageHandler.setNextHandler(errorHandler);

        return dayOffCheckHandler.handleRequest(employee, data);
    }
}
