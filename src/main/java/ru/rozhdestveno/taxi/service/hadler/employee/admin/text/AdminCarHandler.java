package ru.rozhdestveno.taxi.service.hadler.employee.admin.text;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.car.Car;
import ru.rozhdestveno.taxi.entity.car.CarRepository;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

import static ru.rozhdestveno.taxi.constants.Constants.WRONG_FORMAT_TEXT;

@Component
public class AdminCarHandler extends EmployeeHandler {

    private final EmployeeRepository employeeRepository;
    private final ru.rozhdestveno.taxi.entity.car.CarRepository carRepository;

    public AdminCarHandler(EmployeeRepository employeeRepository, CarRepository carRepository) {
        this.employeeRepository = employeeRepository;
        this.carRepository = carRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.ADMIN) &&
                employee.getState().equals(EmployeeState.ADMIN_ADD_DELETE_VEHICLE);

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        List<PartialBotApiMethod<?>> messages = new ArrayList<>();
        message = message.toUpperCase();
        String[] tmp;

        try {
            tmp = message.split(","); //разделение сообщения вида "Рено Логан, А111АА790" или "удалить, a111aa790"
        } catch (PatternSyntaxException | IndexOutOfBoundsException e) {
            return List.of(BotUtil.createMessage(employee.getId(), WRONG_FORMAT_TEXT));
        }

        String licensePlate = tmp[1].trim(); //получение регистрационного номера автомобиля
        Optional<Car> optional = carRepository.getCar(licensePlate);

        if (optional.isEmpty()) { //создание нового, если автомобиль еще не добавлен и оповещение об этом
            String brandAndModel = tmp[0].trim();
            Car car = new Car();
            car.setCarBrandAndModel(brandAndModel);
            car.setLicensePlate(licensePlate);
            carRepository.saveAndFlush(car);
            messages.add(BotUtil.createMessage(employee.getId(),
                    "Автомобиль " + car.getCarBrandAndModel() + " " + car.getLicensePlate() + " добавлен"));
        } else { //удаление автомобиля, если он присутствует в репозитории, и оповещение об этом
            carRepository.delete(optional.get());
            messages.add(BotUtil.createMessage(employee.getId(),
                    "Автомобиль " + optional.get().getCarBrandAndModel() + " "
                            + optional.get().getLicensePlate() + " удален"));
        }

        employee.setState(EmployeeState.EMPLOYEE_ON_DUTY);
        employeeRepository.saveAndFlush(employee);
        return messages;
    }
}
