package ru.rozhdestveno.taxi.service.hadler.employee.driver.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.car.Car;
import ru.rozhdestveno.taxi.entity.car.CarRepository;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.entity.order.Order;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.ArrayList;
import java.util.List;

import static ru.rozhdestveno.taxi.constants.Constants.DRIVER_BOARDING_PRICES_MENU;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeState.DRIVER_BOARDING;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.ACCEPTED;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.WAITING;

@Component
@Slf4j
public class DriverBoardingCallbackHandler extends EmployeeHandler {
    private final CarRepository carRepository;
    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;

    public DriverBoardingCallbackHandler(CarRepository carRepository,
                                         OrderRepository orderRepository, EmployeeRepository employeeRepository) {
        this.carRepository = carRepository;
        this.orderRepository = orderRepository;
        this.employeeRepository = employeeRepository;
    }


    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DRIVER) && message.equals("/boarding");

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();
        Car car = carRepository.findDriverCar(employee.getId());

        if (car == null) {
            messages.add(BotUtil.createMessage(employee.getId(), "Сперва нужно выбрать автомобиль в меню"));
            return messages;
        }

        Order order = orderRepository.findDriverLastOrder(employee.getId());

        if (order != null && (order.getStatus().equals(WAITING) || order.getStatus().equals(ACCEPTED))) {
            messages.add(BotUtil.createMessage(employee.getId(),
                    "Нужно завершить предыдущую поездку <code>" + order.getId() + "</code>"));
            return messages;
        }

        //изменение статуса водителя и создание нового заказа
        employee.setState(DRIVER_BOARDING);
        employeeRepository.saveAndFlush(employee);
        order = new Order();
        order.setDriver(employee);
        order.setStatus(ACCEPTED);
        orderRepository.save(order);
        messages.add(BotUtil.createMessage(employee.getId(),
                "Введите стоимость поездки <code>" + order.getId() + "</code>: ",
                DRIVER_BOARDING_PRICES_MENU, order.getId()));
        log.info("К водителю {}:{} подошел клиент, поездка {}", employee.getLastName(), employee.getId(), order.getId());
        return messages;
    }
}
