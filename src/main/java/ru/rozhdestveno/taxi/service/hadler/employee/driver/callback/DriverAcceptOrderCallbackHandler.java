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

import static ru.rozhdestveno.taxi.constants.Constants.CLIENT_ORDER_MENU;
import static ru.rozhdestveno.taxi.constants.Constants.DISPATCHER_SET_PRICE_MENU;
import static ru.rozhdestveno.taxi.constants.Constants.DRIVER_ORDER_MENU;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeState.DRIVER_ACCEPT_ORDER;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.ACCEPTED;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.CANCELED;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.COMPLETED;

@Component
@Slf4j
public class DriverAcceptOrderCallbackHandler extends EmployeeHandler {
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;
    private final CarRepository carRepository;

    public DriverAcceptOrderCallbackHandler(EmployeeRepository employeeRepository,
                                            OrderRepository orderRepository, CarRepository carRepository) {
        this.employeeRepository = employeeRepository;
        this.orderRepository = orderRepository;
        this.carRepository = carRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DRIVER) && message.startsWith("/accept_order ");

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();

        message = message.replace("/accept_order ", "");
        long orderId = Long.parseLong(message);
        Car car = carRepository.findDriverCar(employee.getId());

        if (car == null) { //если водитель не закрепился за автомобилем, он не сможет принять заказ
            messages.add(BotUtil.createMessage(employee.getId(), "Чтобы принять заказ, выберите свой автомобиль"));
            return messages;
        }

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

        if (order.getStatus().equals(ACCEPTED) && !order.getDriver().getId().equals(employee.getId())) {
            messages.add(BotUtil.createMessage(employee.getId(),
                    "Заказ <code>" + order.getId() + "</code> уже принят другим водителем"));
            return messages;
        } else if (order.getStatus().equals(ACCEPTED) && order.getDriver().getId().equals(employee.getId())) {
            messages.add(BotUtil.createMessage(employee.getId(), "Вы уже приняли этот заказ"));
            return messages;
        }

        //изменения статуса водителя и заказа
        employee.setState(DRIVER_ACCEPT_ORDER);
        order.setStatus(ACCEPTED);
        order.setDriver(employee);
        orderRepository.save(order);
        //формирование сообщения для клиента с указанием имени водителя и регистрационного номера авто
        messages.add(BotUtil.createMessage(order.getClient().getId(),
                "К вам едет " + car.getDriver().getFirstName() + " "
                        + car.getCarBrandAndModel() + " " + car.getLicensePlate(),
                CLIENT_ORDER_MENU, orderId));
        //формирование сообщения для водителя
        messages.add(BotUtil.createMessage(employee.getId(), "Заказ <code>" + orderId + "</code> принят",
                DRIVER_ORDER_MENU, order.getId()));
        Long dispatcherId = employeeRepository.findDispatcherOnDuty();

        if (dispatcherId != null) {
            //формирование сообщения для диспетчера
            messages.add(BotUtil.createMessage(dispatcherId,
                    "Водитель " + employee.getLastName() + " принял заказ: " + order.getAddress(),
                    DISPATCHER_SET_PRICE_MENU, order.getId()));
        }

        log.info("Водитель {}:{} принял заказ {}", employee.getLastName(), employee.getId(), order.getId());
        return messages;
    }
}
