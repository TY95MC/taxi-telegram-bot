package ru.rozhdestveno.taxi.service.hadler.employee.dispatcher.callback;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.car.Car;
import ru.rozhdestveno.taxi.entity.car.CarRepository;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.service.hadler.employee.EmployeeHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.List;

@Component
public class DispatcherDriversCallbackHandler extends EmployeeHandler {
    private final CarRepository carRepository;
    private final EmployeeRepository employeeRepository;

    public DispatcherDriversCallbackHandler(CarRepository carRepository, EmployeeRepository employeeRepository) {
        this.carRepository = carRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Employee employee, String message) {
        boolean condition = employee.getStatus().equals(EmployeeStatus.DISPATCHER) && message.equals("/free_drivers");

        if (!condition && next != null) {
            return next.handleRequest(employee, message);
        }

        //получение списка водителей ожидающих заказ
        List<Long> driverIds = employeeRepository.findWaitingDriversIds();

        if (driverIds.isEmpty()) {
            return List.of(BotUtil.createMessage(employee.getId(), "Все водители заняты"));
        }

        //список автомобилей водителей
        List<Car> cars = carRepository.findDriversCars(driverIds);

        //если список по каким-то причинам пустой, получаем просто список водителей
        if (cars.isEmpty()) {
            List<Employee> drivers = employeeRepository.findWaitingDrivers();

            if (drivers == null) {
                return List.of(BotUtil.createMessage(employee.getId(), "Нет свободных водителей"));
            }

            if (drivers.size() == 0) {
                return List.of(BotUtil.createMessage(employee.getId(), "Список сотрудников пуст!"));
            }

            StringBuilder sb = new StringBuilder();

            //формирование текста сообщения о свободных водителях с указанием имени, фамилии и id
            for (Employee driver : drivers) {
                sb.append(driver.getFirstName()).append(" ").append(driver.getLastName())
                        .append(", id: <code>").append(driver.getId()).append("</code>\n");
            }

            return List.of(BotUtil.createMessage(employee.getId(), sb.toString()));
        }

        StringBuilder sb = new StringBuilder();

        //формирование текста сообщения с указанием фамилии и регистрационного номера его автомобиля
        for (Car car : cars) {
            sb.append(car.getDriver().getLastName()).append(" ").append(car.getLicensePlate()).append("\n");
        }

        return List.of(BotUtil.createMessage(employee.getId(), sb.toString()));
    }
}
