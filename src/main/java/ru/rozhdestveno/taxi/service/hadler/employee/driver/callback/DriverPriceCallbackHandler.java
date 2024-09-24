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
import java.util.Optional;

import static ru.rozhdestveno.taxi.constants.Constants.DISPATCHER_SET_PRICE_MENU;
import static ru.rozhdestveno.taxi.constants.Constants.DRIVER_FINISH_RIDE_MENU;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.CANCELED;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.COMPLETED;

@Component
@Slf4j
public class DriverPriceCallbackHandler extends EmployeeHandler {
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;
    private final CarRepository carRepository;

    public DriverPriceCallbackHandler(EmployeeRepository employeeRepository,
                                      OrderRepository orderRepository, CarRepository carRepository) {
        this.employeeRepository = employeeRepository;
        this.orderRepository = orderRepository;
        this.carRepository = carRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DRIVER) && message.startsWith("/price ");

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();

        //преобразование строки вида "/price *стоимость поездки* *номер заказа*"
        String[] tmp = message.split(" ");
        int price = Integer.parseInt(tmp[1]);
        long orderId = Long.parseLong(tmp[2]);


        Order order;

        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isPresent()) {
            order = orderOpt.get();
        } else {
            log.error("Заказ с id: " + orderId + " не найден");
            messages.add(BotUtil.createMessage(employee.getId(), "Что-то пошло не так, заказ не найден"));
            return messages;
        }

        if (order.getStatus().equals(COMPLETED) || order.getStatus().equals(CANCELED)) {
            messages.add(BotUtil.createMessage(employee.getId(),
                    "Заказ <code>" + order.getId() + "</code> уже числится завершенным"));
            return messages;
        }

        order.setPrice(price);
        orderRepository.saveAndFlush(order);
        messages.add(BotUtil.createMessage(employee.getId(), "Поездка <code>" + orderId + "</code> начата",
                DRIVER_FINISH_RIDE_MENU, orderId));
        Long dispatcherId = employeeRepository.findDispatcherOnDuty();

        if (dispatcherId != null) {
            Car car = carRepository.findDriverCar(employee.getId());
            //формирование сообщения для диспетчера с указанием фамилии водителя, номера машины,
            //стоимости(по умолчанию 200, если иная стоимость не введена водителем)
            //и номером заказа для возможности корректировки диспетчером стоимости заказа
            messages.add(BotUtil.createMessage(dispatcherId,
                    employee.getLastName() + " " + car.getLicensePlate() + " совершает поездку <code>"
                            + order.getId() + "</code>. Стоимость поездки: " + price,
                    DISPATCHER_SET_PRICE_MENU, order.getId()));
        }

        log.info("Поездка {}, установлена стоимость {}", order.getId(), price);
        return messages;
    }
}
