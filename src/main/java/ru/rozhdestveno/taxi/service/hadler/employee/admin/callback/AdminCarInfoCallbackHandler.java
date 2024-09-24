package ru.rozhdestveno.taxi.service.hadler.employee.admin.callback;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.car.Car;
import ru.rozhdestveno.taxi.entity.car.CarRepository;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;

import static ru.rozhdestveno.taxi.entity.employee.EmployeeStatus.ADMIN;

@Component
public class AdminCarInfoCallbackHandler extends EmployeeHandler {
    private final CarRepository repository;

    public AdminCarInfoCallbackHandler(CarRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(ADMIN) && message.equals("/get_vehicles");

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        //получение списка автомобилей таксопарка
        List<Car> cars = repository.findAll();

        if (cars.isEmpty()) {
            return List.of(BotUtil.createMessage(employee.getId(), "Список автомобилей пуст!"));
        }

        StringBuilder sb = new StringBuilder();

        //формирование текста сообщения из марки авто и его регистрационного государственного номера
        for (Car c : cars) {
            sb.append(c.getCarBrandAndModel()).append(" <code>").append(c.getLicensePlate()).append("</code>\n");
        }

        return List.of(BotUtil.createMessage(employee.getId(), sb.toString()));
    }
}
