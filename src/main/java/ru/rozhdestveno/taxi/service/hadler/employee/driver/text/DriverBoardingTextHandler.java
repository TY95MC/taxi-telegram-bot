package ru.rozhdestveno.taxi.service.hadler.employee.driver.text;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.car.Car;
import ru.rozhdestveno.taxi.entity.car.CarRepository;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.entity.order.Order;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.ArrayList;
import java.util.List;

import static ru.rozhdestveno.taxi.constants.Constants.DISPATCHER_SET_PRICE_MENU;
import static ru.rozhdestveno.taxi.constants.Constants.DRIVER_FINISH_RIDE_MENU;
import static ru.rozhdestveno.taxi.constants.Constants.WRONG_FORMAT_TEXT;


@Component
@Slf4j
public class DriverBoardingTextHandler extends EmployeeHandler {

    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;
    private final CarRepository carRepository;

    public DriverBoardingTextHandler(OrderRepository orderRepository, EmployeeRepository employeeRepository,
                                     CarRepository carRepository) {
        this.orderRepository = orderRepository;
        this.employeeRepository = employeeRepository;
        this.carRepository = carRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DRIVER) &&
                employee.getState().equals(EmployeeState.DRIVER_BOARDING);

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();

        Order order = orderRepository.findDriverLastOrder(employee.getId());
        int price;

        try {
            price = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            messages.add(BotUtil.createMessage(employee.getId(), WRONG_FORMAT_TEXT));
            return messages;
        }

        order.setPrice(price);
        orderRepository.saveAndFlush(order);
        messages.add(BotUtil.createMessage(employee.getId(),
                "Поездка  <code>" + order.getId() + "</code> начата",
                DRIVER_FINISH_RIDE_MENU, order.getId()));

        Long dispatcherId = employeeRepository.findDispatcherOnDuty();
        Car car = carRepository.findDriverCar(employee.getId());

        if (dispatcherId != null) {
            //формирование сообщения для диспетчера с указанием фамилии водителя, номера машины,
            //стоимости(по умолчанию 200, если иная стоимость не введена водителем)
            //и номером заказа для возможности корректировки диспетчером стоимости заказа
            messages.add(BotUtil.createMessage(dispatcherId, employee.getLastName() + " " + car.getLicensePlate()
                            + " начал поездку <code>" + order.getId() + "</code>",
                    DISPATCHER_SET_PRICE_MENU, order.getId()));
        }

        log.info("Поездка {}, установлена стоимость {}", order.getId(), message);
        return messages;
    }
}
