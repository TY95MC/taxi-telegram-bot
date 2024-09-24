package ru.rozhdestveno.taxi.service.hadler.employee.driver.callback;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.car.Car;
import ru.rozhdestveno.taxi.entity.car.CarRepository;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.exception.EntityNotFoundException;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DriverCarCallbackHandler extends EmployeeHandler {

    private final CarRepository carRepository;

    public DriverCarCallbackHandler(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DRIVER) &&
                (message.equals("/set_car") || message.startsWith("/set_car "));

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        if (message.equals("/set_car")) {
            List<Car> cars = carRepository.findAll();
            Map<String, String> carsToCallbackData = new HashMap<>();

            for (Car car : cars) {
                carsToCallbackData.put(car.getLicensePlate(), "/set_car " + car.getId());
            }

            return List.of(BotUtil.createMessage(employee.getId(),
                    "Выберите номер автомобиля", carsToCallbackData));
        }

        int newCarId = Integer.parseInt(message.replace("/set_car ", ""));
        Car oldCar = carRepository.findDriverCar(employee.getId());

        if (oldCar != null && oldCar.getId() == newCarId) {
            return List.of(BotUtil.createMessage(employee.getId(), "Вы уже закреплены за данным автомобилем"));
        }

        if (oldCar != null) {
            oldCar.setDriver(null);
            carRepository.saveAndFlush(oldCar);
        }

        Car newCar = carRepository.findById(newCarId).orElseThrow(
                () -> new EntityNotFoundException("Машина с id: " + newCarId + " не найдена")
        );
        newCar.setDriver(employee);
        carRepository.saveAndFlush(newCar);
        return List.of(BotUtil.createMessage(employee.getId(),
                "Ваш автомобиль: " + newCar.getCarBrandAndModel() + " " + newCar.getLicensePlate()));
    }
}
