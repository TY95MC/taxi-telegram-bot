package ru.rozhdestveno.taxi.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rozhdestveno.taxi.config.BotConfig;
import ru.rozhdestveno.taxi.entity.car.Car;
import ru.rozhdestveno.taxi.entity.car.CarRepository;
import ru.rozhdestveno.taxi.entity.contact.CustomContact;
import ru.rozhdestveno.taxi.entity.contact.CustomContactRepository;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.entity.customer.CustomerBanStatus;
import ru.rozhdestveno.taxi.entity.customer.CustomerRepository;
import ru.rozhdestveno.taxi.entity.employee.Employee;
import ru.rozhdestveno.taxi.entity.employee.EmployeeRepository;
import ru.rozhdestveno.taxi.entity.employee.EmployeeState;
import ru.rozhdestveno.taxi.entity.employee.EmployeeStatus;
import ru.rozhdestveno.taxi.entity.feedback.Feedback;
import ru.rozhdestveno.taxi.entity.feedback.FeedbackRepository;
import ru.rozhdestveno.taxi.entity.lost.Lost;
import ru.rozhdestveno.taxi.entity.lost.LostRepository;
import ru.rozhdestveno.taxi.entity.order.Order;
import ru.rozhdestveno.taxi.entity.order.OrderRepository;
import ru.rozhdestveno.taxi.entity.order.OrderStatus;
import ru.rozhdestveno.taxi.entity.util.ClientRequest;
import ru.rozhdestveno.taxi.exception.EntityNotFoundException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static ru.rozhdestveno.taxi.constants.Constants.*;
import static ru.rozhdestveno.taxi.entity.customer.CustomerState.FEEDBACK;
import static ru.rozhdestveno.taxi.entity.customer.CustomerState.LOST;
import static ru.rozhdestveno.taxi.entity.customer.CustomerState.ORDER;
import static ru.rozhdestveno.taxi.entity.customer.CustomerState.SEND_MESSAGE;
import static ru.rozhdestveno.taxi.entity.customer.CustomerState.START;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeState.*;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeStatus.ADMIN;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeStatus.DISPATCHER;
import static ru.rozhdestveno.taxi.entity.employee.EmployeeStatus.DRIVER;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.ACCEPTED;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.CANCELED;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.COMPLETED;
import static ru.rozhdestveno.taxi.entity.order.OrderStatus.WAITING;

@Component
@Slf4j
public class TaxiTelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final FeedbackRepository feedbackRepository;
    private final LostRepository lostRepository;
    private final CarRepository carRepository;
    private final CustomContactRepository customContactRepository;

    private static int lostReport = 0;
    private static int feedbackReport = 0;

    public TaxiTelegramBot(BotConfig config, EmployeeRepository employeeRepository,
                           CustomerRepository customerRepository, OrderRepository orderRepository,
                           FeedbackRepository feedbackRepository, LostRepository lostRepository,
                           CarRepository carRepository, CustomContactRepository customContactRepository) {
        this.config = config;
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.lostRepository = lostRepository;
        this.carRepository = carRepository;
        this.customContactRepository = customContactRepository;
        List<BotCommand> commandList = new ArrayList<>();
        commandList.add(new BotCommand("/start", "Запуск бота"));
        commandList.add(new BotCommand("/help", "Информация о нашем такси"));
        commandList.add(new BotCommand("/contacts", "Контактные данные"));

        try {
            this.execute(new SetMyCommands(commandList, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error constructor setting menu buttons: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText().replaceAll("\\s+", " ").trim();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/contacts")) {
                sendMessage(chatId, getContacts(), null);
                return;
            }

            String tmp = messageText.toUpperCase();

            if (tmp.contains("DROP") || tmp.contains("UPDATE")
                    || tmp.contains("DELETE") || tmp.contains("INSERT") || tmp.contains("SELECT")) {
                sendMessage(chatId, WRONG_FORMAT_TEXT, null);
                return;
            }

            Optional<Employee> employee = employeeRepository.findById(chatId);

            if (employee.isEmpty()) {
                Optional<Customer> optional = customerRepository.findById(chatId);
                Customer customer = optional.isEmpty() ? registerCustomer(chatId) : optional.get();
                customerTextMenu(customer, chatId, messageText, update);
            } else {
                employeeTextMenu(employee.get(), chatId, messageText, update);
            }

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            Optional<Employee> employee = employeeRepository.findById(chatId);

            if (employee.isEmpty()) {
                Optional<Customer> customer = customerRepository.findById(chatId);

                if (customer.isPresent()) {
                    customerCallBackQueryMenu(customer.get(), chatId, callbackData);
                } else {
                    sendMessage(chatId, DEFAULT_ERROR_MESSAGE, null);
                }

            } else {
                employeeCallBackQueryMenu(employee.get(), chatId, callbackData);
            }

        } else {
            sendMessage(update.getMessage().getChatId(), "Допускается только текстовое сообщение", null);
        }
    }

    private void employeeTextMenu(Employee employee, long employeeId, String messageText, Update update) {
        if (employee.getState().equals(EMPLOYEE_ON_WEEKEND) && !update.getMessage().isCommand()) {
            sendMessage(employeeId, WEEKEND_TEXT, null);
            return;
        }

        switch (employee.getStatus()) {
            case ADMIN:
                adminTextMenu(employee, employeeId, messageText, update);
                break;

            case DISPATCHER:
                dispatcherTextMenu(employee, employeeId, messageText, update);
                break;

            case DRIVER:
                driverTextMenu(employee, employeeId, messageText, update);
                break;

            default:
                sendMessage(employeeId, DEFAULT_ERROR_MESSAGE, null);
        }
    }

    private void employeeCallBackQueryMenu(Employee employee, long employeeId, String callbackData) {
        if (employee.getState().equals(EMPLOYEE_ON_WEEKEND)) {
            sendMessage(employeeId, WEEKEND_TEXT, null);
            return;
        }

        switch (employee.getStatus()) {
            case ADMIN:
                adminCallbackDataMenu(employee, employeeId, callbackData);
                break;

            case DISPATCHER:
                dispatcherCallbackDataMenu(employee, employeeId, callbackData);
                break;

            case DRIVER:
                driverCallbackDataMenu(employee, employeeId, callbackData);
                break;

            default:
                sendMessage(employeeId, DEFAULT_ERROR_MESSAGE, null);
        }
    }

    private void customerTextMenu(Customer client, long clientId, String messageText, Update update) {
        if (isBanned(client)) {
            return;
        }

        if (update.getMessage().isCommand()) {
            switch (messageText) {
                case "/start":
                    sendMessage(
                            clientId,
                            "Здравствуйте, " + update.getMessage().getChat().getFirstName() + "!",
                            setKeyboard(CLIENT_START_MENU)
                    );
                    client.setState(START);
                    customerRepository.saveAndFlush(client);
                    break;

                case "/help":
                    sendMessage(clientId, CUSTOMER_HELP_COMMAND_TEXT, null);
                    break;

                default:
                    sendMessage(clientId, DEFAULT_ERROR_MESSAGE, null);
            }
        } else {
            switch (client.getState()) {
                case ORDER:
                    Optional<Order> optional = orderRepository.findClientLastOrder(clientId);

                    if (optional.isPresent()) {
                        OrderStatus status = optional.get().getStatus();

                        if (status.equals(WAITING) || status.equals(ACCEPTED)) {
                            sendMessage(clientId, "Нельзя создать новый заказ, не завершив текущий", null);
                            return;
                        }
                    }

                    List<Long> drivers = employeeRepository.findWaitingDriversIds();

                    if (drivers.isEmpty()) {
                        sendMessage(client.getId(), "Все водители заняты, попробуйте позже!", null);
                        return;
                    }

                    Order order = new Order();
                    order.setClient(client);
                    order.setOrderDate(LocalDate.now());
                    order.setAddress(messageText);
                    order = orderRepository.saveAndFlush(order);

                    Order finalOrder = order;
                    drivers.forEach(
                            driverId -> sendMessage(
                                    driverId,
                                    "Заказ <code>" + finalOrder.getId() + "</code> " +
                                            " клиента <code>" + clientId + "</code>: " + finalOrder.getAddress(),
                                    setKeyboard(prepareButtons(DRIVER_ACCEPT_ORDER_MENU, finalOrder.getId()))
                            )
                    );

                    sendMessage(
                            clientId,
                            "Заказ " + order.getId() + " обрабатывается. " +
                                    "Если в течение 5 минут ваш заказ не приняли, отмените его и создайте заново.",
                            setKeyboard(prepareButtons(CLIENT_ORDER_MENU, order.getId()))
                    );
                    break;

                case LOST:
                    List<Lost> losts = lostRepository.findLastLosts(clientId, LocalDate.now());

                    if (losts != null && losts.size() == 3) {
                        sendMessage(clientId, "В день можно оставлять не более 3 заявок.", null);
                        break;
                    }

                    Lost lost = new Lost();
                    lost.setClient(client);
                    lost.setText(messageText);
                    lostRepository.saveAndFlush(lost);
                    client.setState(START);
                    customerRepository.saveAndFlush(client);
                    Long dispatcherId = employeeRepository.findDispatcherOnDuty();

                    if (dispatcherId != null) {
                        sendMessage(dispatcherId, "Заявка " + lost.getId() + " " + messageText, null);
                    }

                    sendMessage(clientId, "Принято в обработку. Номер заявки " + lost.getId(), null);
                    break;

                case FEEDBACK:
                    List<Feedback> feedbacks = feedbackRepository.findLastFeedbacks(clientId, LocalDate.now());

                    if (feedbacks != null && feedbacks.size() == 3) {
                        sendMessage(clientId, "В день можно оставлять не более 3 отзывов", null);
                        break;
                    }

                    Feedback feedback = new Feedback();
                    feedback.setText(messageText);
                    feedback.setClient(client);
                    client.setState(START);
                    customerRepository.saveAndFlush(client);
                    feedbackRepository.save(feedback);
                    sendMessage(clientId, "Принято в обработку, номер отзыва " + feedback.getId(), null);
                    break;

                case SEND_MESSAGE:
                    optional = orderRepository.findClientCurrentOrder(clientId);

                    if (optional.isPresent()) {
                        order = optional.get();

                        if (order.getStatus().equals(ACCEPTED)) {
                            sendMessage(order.getDriver().getId(), messageText, null);
                            log.info("Заказ {}, клиент {}, текст: {}", order.getId(), clientId, messageText);
                        } else {
                            sendMessage(clientId, "Дождитесь подтверждения заказа", null);
                        }

                    } else {
                        sendMessage(clientId, "Дождитесь подтверждения заказа", null);
                    }

                    break;

                default:
                    sendMessage(clientId, DEFAULT_ERROR_MESSAGE, null);
            }
        }
    }

    private void customerCallBackQueryMenu(Customer client, long clientId, String callbackData) {
        if (isBanned(client)) {
            return;
        }

        switch (callbackData) {
            case "/order":
                client.setState(ORDER);
                customerRepository.saveAndFlush(client);
                sendMessage(clientId, ADDRESS_TEXT, null);
                break;

            case "/lost":
                client.setState(LOST);
                customerRepository.saveAndFlush(client);
                sendMessage(clientId, LOST_TEXT, null);
                break;

            case "/feedback":
                client.setState(FEEDBACK);
                customerRepository.saveAndFlush(client);
                sendMessage(clientId, FEEDBACK_TEXT, null);
                break;

            default:
                if (callbackData.startsWith("/cancel_order ")) {
                    callbackData = callbackData.replace("/cancel_order ", "");
                    client.setState(START);
                    customerRepository.saveAndFlush(client);
                    Optional<Order> order = orderRepository.findById(Long.valueOf(callbackData));

                    if (order.isPresent()) {
                        if (isOrderFinished(order.get(), clientId)) {
                            sendMessage(clientId, "Заказ уже отменен", null);
                            return;
                        }

                        order.get().setStatus(CANCELED);
                        orderRepository.saveAndFlush(order.get());
                        sendMessage(clientId, "Заказ " + order.get().getId() + " отменен!", null);

                        if (order.get().getDriver() != null) {
                            sendMessage(
                                    order.get().getDriver().getId(),
                                    "Клиент отменил поездку <code>" + order.get().getId() + "</code>",
                                    null
                            );
                            order.get().getDriver().setState(EMPLOYEE_ON_DUTY);
                            employeeRepository.saveAndFlush(order.get().getDriver());
                        }

                        customerRepository.save(client);
                        log.warn("Клиент {}, отменил поездку {}", clientId, order.get().getId());
                    } else {
                        sendMessage(clientId, DEFAULT_ERROR_MESSAGE, null);
                    }

                } else if (callbackData.startsWith("/send_message ")) {
                    Optional<Order> optional = orderRepository.findClientCurrentOrder(clientId);

                    if (optional.isPresent()) {
                        Order order = optional.get();

                        if (isOrderFinished(order, clientId)) {
                            return;
                        }

                        if (client.getState().equals(SEND_MESSAGE)) {
                            sendMessage(clientId, "Вы уже можете писать сообщение водителю", null);
                            return;
                        }

                        order.getClient().setState(SEND_MESSAGE);
                        order.getDriver().setState(DRIVER_SEND_MESSAGE);
                        customerRepository.saveAndFlush(order.getClient());
                        employeeRepository.saveAndFlush(order.getDriver());
                        sendMessage(clientId, "Введите ваше сообщение", null);
                        sendMessage(order.getDriver().getId(), "Ожидайте сообщение от клиента", null);
                        log.info("Клиент {} первым пишет водителю {}:{}",
                                clientId, order.getDriver().getLastName(), order.getDriver().getId());
                    } else {
                        sendMessage(clientId, "Заказ уже числится завершенным", null);
                    }

                } else {
                    sendMessage(clientId, DEFAULT_ERROR_MESSAGE, null);
                }
        }
    }

    private void adminTextMenu(Employee admin, long adminId, String messageText, Update update) {
        if (update.getMessage().isCommand()) {
            switch (messageText) {
                case "/start":
                    sendMessage(
                            adminId,
                            "Здравствуйте, " + update.getMessage().getChat().getFirstName() + "!",
                            setKeyboard(ADMIN_START_MENU)
                    );
                    break;

                case "/help":
                    sendMessage(adminId, ADMIN_HELP_COMMAND_TEXT, null);
                    break;

                default:
                    sendMessage(adminId, DEFAULT_ERROR_MESSAGE, null);
            }
        } else {
            switch (admin.getState()) {
                case ADMIN_ADD_DELETE_ADMIN:
                    addDeleteEmployee(admin, ADMIN, messageText.toUpperCase(), adminId);
                    break;

                case ADMIN_ADD_DELETE_DISPATCHER:
                    addDeleteEmployee(admin, DISPATCHER, messageText.toUpperCase(), adminId);
                    break;

                case ADMIN_ADD_DELETE_DRIVER:
                    addDeleteEmployee(admin, DRIVER, messageText.toUpperCase(), adminId);
                    break;

                case ADMIN_ADD_DELETE_VEHICLE:
                    messageText = messageText.toUpperCase();
                    String[] tmp;

                    try {
                        tmp = messageText.split(",");
                    } catch (PatternSyntaxException | IndexOutOfBoundsException e) {
                        sendMessage(adminId, WRONG_FORMAT_TEXT, null);
                        break;
                    }

                    String licensePlate = tmp[1].trim();
                    Optional<Car> optional = carRepository.getCar(licensePlate);

                    if (optional.isEmpty()) {
                        String brandAndModel = tmp[0];
                        Car car = new Car();
                        car.setCarBrandAndModel(brandAndModel);
                        car.setLicensePlate(licensePlate);
                        carRepository.saveAndFlush(car);
                        sendMessage(
                                adminId,
                                "Автомобиль " + car.getCarBrandAndModel() + " " + car.getLicensePlate() + " добавлен",
                                null
                        );
                    } else {
                        carRepository.delete(optional.get());
                        sendMessage(
                                adminId,
                                "Автомобиль " + optional.get().getCarBrandAndModel() + " "
                                        + optional.get().getLicensePlate() + " удален",
                                null
                        );
                    }

                    admin.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(admin);
                    break;

                case ADMIN_ADD_DELETE_CONTACT:
                    String command, phoneNumber;
                    CustomContact contact;

                    try {
                        tmp = messageText.split(" ", 2);
                        command = tmp[0].toLowerCase();
                        phoneNumber = tmp[1];
                        Pattern pattern = Pattern.compile(PHONE_REGEX);
                        Matcher phoneMatcher = pattern.matcher(phoneNumber);

                        if (!phoneMatcher.matches()) {
                            sendMessage(adminId, WRONG_FORMAT_TEXT, null);
                            return;
                        }

                        contact = customContactRepository.findByPhoneNumber(phoneNumber);

                        if (command.equals("удалить")) {
                            if (contact == null) {
                                sendMessage(adminId, "Контакта нет в базе данных. Попробуйте еще раз.", null);
                                return;
                            }

                            customContactRepository.delete(contact);
                            sendMessage(adminId, "Контакт " + phoneNumber + " удален.", null);
                        } else if (command.equals("добавить")) {
                            if (contact != null) {
                                sendMessage(adminId, "Контакт уже есть в базе данных.", null);
                                return;
                            }

                            CustomContact newContact = new CustomContact();
                            newContact.setPhoneNumber(phoneNumber);
                            customContactRepository.saveAndFlush(newContact);
                            sendMessage(adminId, "Контакт " + phoneNumber + " добавлен.", null);
                        } else {
                            sendMessage(adminId, WRONG_FORMAT_TEXT, null);
                            return;
                        }

                    } catch (PatternSyntaxException | NumberFormatException | IndexOutOfBoundsException e) {
                        sendMessage(adminId, WRONG_FORMAT_TEXT, null);
                        return;
                    }

                    admin.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(admin);
                    break;

                case ADMIN_GET_CASH:
                    LocalDate date;

                    try {
                        date = LocalDate.parse(messageText.replace(" ", "-"));
                    } catch (DateTimeParseException | IndexOutOfBoundsException e) {
                        sendMessage(adminId, WRONG_FORMAT_TEXT, null);
                        break;
                    }

                    sendMessage(adminId, getCash(date), null);
                    admin.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(admin);
                    break;

                case LOST_YEAR_REPORT:
                    getReport(adminId, messageText, false, true);
                    admin.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(admin);
                    break;

                case LOST_MONTH_REPORT:
                    getReport(adminId, messageText, true, true);
                    admin.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(admin);
                    break;

                case FEEDBACK_YEAR_REPORT:
                    getReport(adminId, messageText, false, false);
                    admin.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(admin);
                    break;

                case FEEDBACK_MONTH_REPORT:
                    getReport(adminId, messageText, true, false);
                    admin.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(admin);
                    break;

                default:
                    sendMessage(adminId, DEFAULT_ERROR_MESSAGE, null);
            }
        }
    }

    private void adminCallbackDataMenu(Employee admin, long adminId, String callbackData) {
        switch (callbackData) {
            case "/add_delete_admin":
                admin.setState(ADMIN_ADD_DELETE_ADMIN);
                employeeRepository.saveAndFlush(admin);
                sendMessage(adminId, ADMIN_ADD_DELETE_EMPLOYEE_TEXT, null);
                break;

            case "/add_delete_dispatcher":
                admin.setState(ADMIN_ADD_DELETE_DISPATCHER);
                employeeRepository.saveAndFlush(admin);
                sendMessage(adminId, ADMIN_ADD_DELETE_EMPLOYEE_TEXT, null);
                break;

            case "/add_delete_driver":
                admin.setState(ADMIN_ADD_DELETE_DRIVER);
                employeeRepository.saveAndFlush(admin);
                sendMessage(adminId, ADMIN_ADD_DELETE_EMPLOYEE_TEXT, null);
                break;

            case "/add_delete_vehicle":
                admin.setState(ADMIN_ADD_DELETE_VEHICLE);
                employeeRepository.saveAndFlush(admin);
                sendMessage(adminId, ADMIN_ADD_DELETE_VEHICLE_TEXT, null);
                break;

            case "/add_delete_contact":
                admin.setState(ADMIN_ADD_DELETE_CONTACT);
                employeeRepository.saveAndFlush(admin);
                sendMessage(adminId, ADMIN_ADD_DELETE_CONTACT_TEXT, null);
                break;

            case "/get_admins":
                sendEmployees(employeeRepository.findAllByStatus(ADMIN), adminId);
                break;

            case "/get_dispatchers":
                sendEmployees(employeeRepository.findAllByStatus(DISPATCHER), adminId);
                break;

            case "/get_drivers":
                sendEmployees(employeeRepository.findAllByStatus(DRIVER), adminId);
                break;

            case "/get_vehicles":
                List<Car> cars = carRepository.findAll();

                if (cars.isEmpty()) {
                    sendMessage(adminId, "Список автомобилей пуст!", null);
                    break;
                }

                StringBuilder sb = new StringBuilder();

                for (Car c : cars) {
                    sb.append(c.getCarBrandAndModel()).append(" <code>").append(c.getLicensePlate()).append("</code>\n");
                }

                sendMessage(adminId, sb.toString(), null);
                admin.setState(EMPLOYEE_ON_DUTY);
                employeeRepository.saveAndFlush(admin);
                break;

            case "/get_contacts":
                sendMessage(adminId, getContacts(), null);
                break;

            case "/get_lost":
                sendMessage(adminId, "Выберите период отчета", setKeyboard(LOST_REPORT_MENU));
                break;

            case "/get_feedback":
                sendMessage(adminId, "Выберите период отчета", setKeyboard(FEEDBACK_REPORT_MENU));
                break;

            case "/lost_report_by_year":
                admin.setState(LOST_YEAR_REPORT);
                employeeRepository.saveAndFlush(admin);
                sendMessage(adminId, REPORT_BY_YEAR_TEXT, null);
                break;

            case "/lost_report_by_month":
                admin.setState(LOST_MONTH_REPORT);
                employeeRepository.saveAndFlush(admin);
                sendMessage(adminId, REPORT_BY_MONTH_TEXT, null);
                break;

            case "/feedback_report_by_year":
                admin.setState(FEEDBACK_YEAR_REPORT);
                employeeRepository.saveAndFlush(admin);
                sendMessage(adminId, REPORT_BY_YEAR_TEXT, null);
                break;

            case "/feedback_report_by_month":
                admin.setState(FEEDBACK_MONTH_REPORT);
                employeeRepository.saveAndFlush(admin);
                sendMessage(adminId, REPORT_BY_MONTH_TEXT, null);
                break;

            case "/back":
                sendMessage(adminId, "Главное меню", setKeyboard(ADMIN_START_MENU));
                break;

            case "/report_today":
                sendMessage(adminId, getCash(LocalDate.now()), null);
                admin.setState(EMPLOYEE_ON_DUTY);
                employeeRepository.saveAndFlush(admin);
                break;

            case "/report_by_date":
                admin.setState(ADMIN_GET_CASH);
                employeeRepository.saveAndFlush(admin);
                sendMessage(adminId, REPORT_BY_DATE_TEXT, null);
                break;

            default:
                sendMessage(adminId, DEFAULT_ERROR_MESSAGE, null);
        }
    }

    private void dispatcherTextMenu(Employee dispatcher, long dispatcherId, String messageText, Update update) {
        if (update.getMessage().isCommand()) {
            switch (messageText) {
                case "/start":
                    dispatcher.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(dispatcher);
                    sendMessage(
                            dispatcherId,
                            "Здравствуйте, " + update.getMessage().getChat().getFirstName() + "! Смена открыта!",
                            setKeyboard(DISPATCHER_START_MENU)
                    );
                    break;

                case "/help":
                    sendMessage(dispatcherId, DISPATCHER_HELP_COMMAND_TEXT, null);
                    break;

                default:
                    sendMessage(dispatcherId, DEFAULT_ERROR_MESSAGE, null);
            }
        } else {
            switch (dispatcher.getState()) {
                case DISPATCHER_SET_PRICE:
                    String[] tmp;
                    long orderId;
                    int price;

                    try {
                        tmp = messageText.split(" ");
                        orderId = Long.parseLong(tmp[0]);
                        price = Integer.parseInt(tmp[1]);
                    } catch (PatternSyntaxException | NumberFormatException | IndexOutOfBoundsException e) {
                        sendMessage(dispatcherId, WRONG_FORMAT_TEXT, null);
                        return;
                    }

                    Order order = getOrder(orderId, dispatcherId);
                    order.setPrice(price);
                    orderRepository.saveAndFlush(order);
                    dispatcher.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(dispatcher);
                    sendMessage(dispatcherId, "Цена установлена", null);
                    log.info("Изменение диспетчером {} цены заказа {}, новая цена {}",
                            dispatcherId, order.getId(), order.getPrice());
                    break;

                case LOST_YEAR_REPORT:
                    getReport(dispatcherId, messageText, false, true);
                    dispatcher.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(dispatcher);
                    break;

                case LOST_MONTH_REPORT:
                    getReport(dispatcherId, messageText, true, true);
                    dispatcher.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(dispatcher);
                    break;

                case FEEDBACK_YEAR_REPORT:
                    getReport(dispatcherId, messageText, false, false);
                    dispatcher.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(dispatcher);
                    break;

                case FEEDBACK_MONTH_REPORT:
                    getReport(dispatcherId, messageText, true, false);
                    dispatcher.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(dispatcher);
                    break;

                default:
                    sendMessage(dispatcherId, DEFAULT_ERROR_MESSAGE, null);
            }
        }
    }

    private void dispatcherCallbackDataMenu(Employee dispatcher, long dispatcherId, String callbackData) {
        switch (callbackData) {
            case "/free_drivers":
                List<Long> driverIds = employeeRepository.findWaitingDriversIds();

                if (driverIds.isEmpty()) {
                    sendMessage(dispatcherId, "Все водители заняты", null);
                    break;
                }

                List<Car> cars = carRepository.findDriversCars(driverIds);

                if (cars.isEmpty()) {
                    List<Employee> drivers = employeeRepository.findWaitingDrivers();

                    if (drivers == null) {
                        sendMessage(dispatcherId, "Нет свободных водителей", null);
                        break;
                    }

                    sendEmployees(drivers, dispatcherId);
                    break;
                }

                StringBuilder sb = new StringBuilder();

                for (Car car : cars) {
                    sb.append(car.getDriver().getLastName()).append(" ").append(car.getLicensePlate()).append("\n");
                }

                sendMessage(dispatcherId, sb.toString(), null);
                break;

            case "/finish_work":
                dispatcher.setState(EMPLOYEE_ON_WEEKEND);
                employeeRepository.saveAndFlush(dispatcher);
                sendMessage(dispatcherId, "Смена закрыта.", null);
                break;

            case "/get_lost":
                sendMessage(dispatcherId, "Выберите период отчета", setKeyboard(LOST_REPORT_MENU));
                break;

            case "/get_feedback":
                sendMessage(dispatcherId, "Выберите период отчета", setKeyboard(FEEDBACK_REPORT_MENU));
                break;

            case "/lost_report_by_year":
                dispatcher.setState(LOST_YEAR_REPORT);
                employeeRepository.saveAndFlush(dispatcher);
                sendMessage(dispatcherId, REPORT_BY_YEAR_TEXT, null);
                break;

            case "/lost_report_by_month":
                dispatcher.setState(LOST_MONTH_REPORT);
                employeeRepository.saveAndFlush(dispatcher);
                sendMessage(dispatcherId, REPORT_BY_MONTH_TEXT, null);
                break;

            case "/feedback_report_by_year":
                dispatcher.setState(FEEDBACK_YEAR_REPORT);
                employeeRepository.saveAndFlush(dispatcher);
                sendMessage(dispatcherId, REPORT_BY_YEAR_TEXT, null);
                break;

            case "/feedback_report_by_month":
                dispatcher.setState(FEEDBACK_MONTH_REPORT);
                employeeRepository.saveAndFlush(dispatcher);
                sendMessage(dispatcherId, REPORT_BY_MONTH_TEXT, null);
                break;

            case "/back":
                sendMessage(dispatcherId, "Главное меню", setKeyboard(DISPATCHER_START_MENU));
                break;

            default:
                if (callbackData.startsWith("/set_price ")) {
                    callbackData = callbackData.replace("/set_price ", "");
                    Optional<Order> optional = orderRepository.findById(Long.parseLong(callbackData));

                    if (optional.isEmpty()) {
                        sendMessage(dispatcherId, "Что-то пошло не так", null);
                    } else {
                        sendMessage(dispatcherId, String.format(DISPATCHER_SET_PRICE_TEXT, callbackData), null);
                        dispatcher.setState(DISPATCHER_SET_PRICE);
                        employeeRepository.saveAndFlush(dispatcher);
                        log.info("Изменение диспетчером {} цены заказа {}, старая цена {}",
                                dispatcherId, optional.get().getId(), optional.get().getPrice());
                    }

                } else {
                    sendMessage(dispatcherId, DEFAULT_ERROR_MESSAGE, null);
                }
        }
    }

    private void driverTextMenu(Employee driver, long driverId, String messageText, Update update) {
        if (update.getMessage().isCommand()) {
            switch (messageText) {
                case "/start":
                    driver.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(driver);
                    sendMessage(
                            driverId,
                            "Здравствуйте, " + update.getMessage().getChat().getFirstName() + "! Смена открыта!",
                            setKeyboard(DRIVER_START_MENU)
                    );
                    break;

                case "/help":
                    sendMessage(driverId, DRIVER_HELP_COMMAND_TEXT, null);
                    break;

                default:
                    sendMessage(driverId, DEFAULT_ERROR_MESSAGE, null);
            }
        } else {
            switch (driver.getState()) {
                case DRIVER_BOARDING:
                    Order order = orderRepository.findDriverLastOrder(driverId);
                    int price;

                    try {
                        price = Integer.parseInt(messageText);
                    } catch (NumberFormatException e) {
                        sendMessage(driverId, WRONG_FORMAT_TEXT, null);
                        return;
                    }

                    order.setPrice(price);
                    orderRepository.saveAndFlush(order);
                    sendMessage(
                            driverId,
                            "Поездка  <code>" + order.getId() + "</code> начата",
                            setKeyboard(prepareButtons(DRIVER_FINISH_RIDE_MENU, order.getId()))
                    );

                    Long dispatcherId = employeeRepository.findDispatcherOnDuty();
                    Car car = carRepository.findDriverCar(driverId);

                    if (dispatcherId != null) {
                        sendMessage(
                                dispatcherId,
                                driver.getLastName() + " " + car.getLicensePlate()
                                        + " начал поездку <code>" + order.getId() + "</code>",
                                setKeyboard(prepareButtons(DISPATCHER_SET_PRICE_MENU, order.getId()))
                        );
                    }

                    log.info("Поездка {}, установлена стоимость {}", order.getId(), messageText);
                    break;

                case DRIVER_SEND_MESSAGE:
                    order = orderRepository.findDriverLastOrder(driverId);

                    if (isOrderFinished(order, driverId)) {
                        sendMessage(driverId, "Невозможно послать сообщение", null);
                        break;
                    }

                    sendMessage(order.getClient().getId(), messageText, null);
                    sendMessage(
                            driverId,
                            "Сообщение отправлено клиенту! Заказ <code>" + order.getId() + "</code>",
                            null
                    );
                    log.info("Заказ {}, водитель {}:{}, текст: {}",
                            order.getId(), driver.getLastName(), driverId, messageText);
                    break;

                default:
                    sendMessage(driverId, DEFAULT_ERROR_MESSAGE, null);
            }
        }
    }

    private void driverCallbackDataMenu(Employee driver, long driverId, String callbackData) {
        switch (callbackData) {
            case "/set_car":
                List<Car> cars = carRepository.findAll();
                Map<String, String> carsToCallbackData = new HashMap<>();

                for (Car car : cars) {
                    carsToCallbackData.put(car.getLicensePlate(), "/set_car " + car.getId());
                }

                sendMessage(driverId, "Выберите номер автомобиля", setKeyboard(carsToCallbackData));
                break;

            case "/boarding":
                Car car = carRepository.findDriverCar(driverId);

                if (car == null) {
                    sendMessage(driver.getId(), "Сперва нужно выбрать автомобиль в меню", null);
                    return;
                }

                Order order = orderRepository.findDriverLastOrder(driverId);

                if (order != null && (order.getStatus().equals(WAITING) || order.getStatus().equals(ACCEPTED))) {
                    sendMessage(
                            driverId,
                            "Нужно завершить предыдущую поездку <code>" + order.getId() + "</code>",
                            null
                    );
                    break;
                }

                driver.setState(DRIVER_BOARDING);
                employeeRepository.saveAndFlush(driver);
                order = new Order();
                order.setDriver(driver);
                order.setStatus(ACCEPTED);
                orderRepository.save(order);
                sendMessage(
                        driverId,
                        "Введите стоимость поездки <code>" + order.getId() + "</code>: ",
                        setKeyboard(prepareButtons(DRIVER_BOARDING_PRICES_MENU, order.getId()))
                );
                log.info("К водителю {}:{} подошел клиент, поездка {}", driver.getLastName(), driverId, order.getId());
                break;

            case "/finish_work":
                driver.setState(EMPLOYEE_ON_WEEKEND);
                employeeRepository.saveAndFlush(driver);
                sendMessage(driverId, "Смена закрыта", null);
                break;

            case "/break":
                if (driver.getState().equals(EMPLOYEE_ON_DUTY)) {
                    driver.setState(EmployeeState.DRIVER_ON_PAUSE);
                    sendMessage(driverId, "Перерыв начат", null);
                } else {
                    driver.setState(EMPLOYEE_ON_DUTY);
                    sendMessage(driverId, "Перерыв окончен", null);
                }

                employeeRepository.saveAndFlush(driver);
                break;

            default:
                if (callbackData.startsWith("/price ")) {
                    String[] tmp = callbackData.split(" ");
                    int price = Integer.parseInt(tmp[1]);
                    long orderId = Long.parseLong(tmp[2]);
                    order = getOrder(orderId, driverId);

                    if (isOrderFinished(order, driverId)) {
                        return;
                    }

                    order.setPrice(price);
                    orderRepository.saveAndFlush(order);
                    sendMessage(
                            driverId,
                            "Поездка <code>" + orderId + "</code> начата",
                            setKeyboard(prepareButtons(DRIVER_FINISH_RIDE_MENU, orderId))
                    );
                    Long dispatcherId = employeeRepository.findDispatcherOnDuty();

                    if (dispatcherId != null) {
                        car = carRepository.findDriverCar(driverId);
                        sendMessage(
                                dispatcherId,
                                driver.getLastName() + " " + car.getLicensePlate() + " совершает поездку <code>"
                                        + order.getId() + "</code>. Стоимость поездки: " + price,
                                setKeyboard(prepareButtons(DISPATCHER_SET_PRICE_MENU, order.getId()))
                        );
                    }

                    log.info("Поездка {}, установлена стоимость {}", order.getId(), price);

                } else if (callbackData.startsWith("/set_car ")) {
                    int newCarId = Integer.parseInt(callbackData.replace("/set_car ", ""));
                    Car oldCar = carRepository.findDriverCar(driverId);

                    if (oldCar != null && oldCar.getId() == newCarId) {
                        sendMessage(driverId, "Вы уже закреплены за данным автомобилем", null);
                        return;
                    }

                    if (oldCar != null) {
                        oldCar.setDriver(null);
                        carRepository.saveAndFlush(oldCar);
                    }

                    Car newCar = getCar(newCarId, driverId);
                    newCar.setDriver(driver);
                    carRepository.saveAndFlush(newCar);
                    sendMessage(
                            driverId,
                            "Ваш автомобиль: " + newCar.getCarBrandAndModel() + " " + newCar.getLicensePlate(),
                            null
                    );
                } else if (callbackData.startsWith("/accept_order ")) {
                    callbackData = callbackData.replace("/accept_order ", "");
                    long orderId = Long.parseLong(callbackData);
                    car = carRepository.findDriverCar(driverId);

                    if (car == null) {
                        sendMessage(driverId, "Чтобы принять заказ, выберите свой автомобиль", null);
                        return;
                    }

                    order = getOrder(orderId, driverId);

                    if (order.getStatus().equals(ACCEPTED) && order.getDriver().getId() != driverId) {
                        sendMessage(
                                driverId,
                                "Заказ <code>" + order.getId() + "</code> уже принят другим водителем",
                                null
                        );
                        return;
                    } else if (order.getStatus().equals(ACCEPTED) && order.getDriver().getId() == driverId) {
                        sendMessage(driverId, "Вы уже приняли этот заказ", null);
                        return;
                    } else if (isOrderFinished(order, driverId)) {
                        return;
                    }

                    driver.setState(DRIVER_ACCEPT_ORDER);
                    order.setStatus(ACCEPTED);
                    order.setDriver(driver);
                    orderRepository.save(order);
                    sendMessage(
                            order.getClient().getId(),
                            "К вам едет " + car.getDriver().getFirstName() + " "
                                    + car.getCarBrandAndModel() + " " + car.getLicensePlate(),
                            setKeyboard(prepareButtons(CLIENT_ORDER_MENU, orderId))
                    );
                    sendMessage(
                            driverId,
                            "Заказ <code>" + orderId + "</code> принят",
                            setKeyboard(prepareButtons(DRIVER_ORDER_MENU, order.getId()))
                    );
                    Long dispatcherId = employeeRepository.findDispatcherOnDuty();

                    if (dispatcherId != null) {
                        sendMessage(
                                dispatcherId,
                                "Водитель " + driver.getLastName() + " принял заказ: " + order.getAddress(),
                                setKeyboard(prepareButtons(DISPATCHER_SET_PRICE_MENU, order.getId()))
                        );
                    }

                    log.info("Водитель {}:{} принял заказ {}", driver.getLastName(), driver.getId(), order.getId());
                } else if (callbackData.startsWith("/close_to_client ")) {
                    long orderId = Long.parseLong(callbackData.replace("/close_to_client ", ""));
                    order = getOrder(orderId, driverId);

                    if (isOrderFinished(order, driverId)) {
                        return;
                    }

                    sendMessage(order.getClient().getId(), "Водитель прибудет в течение 5 минут", null);
                } else if (callbackData.startsWith("/arrived ")) {
                    long orderId = Long.parseLong(callbackData.replace("/arrived ", ""));
                    order = getOrder(orderId, driverId);

                    if (isOrderFinished(order, driverId)) {
                        return;
                    }

                    sendMessage(
                            order.getClient().getId(),
                            "Водитель прибыл, время бесплатного ожидания 5 минут",
                            null
                    );
                } else if (callbackData.startsWith("/ban ")) {
                    long orderId = Long.parseLong(callbackData.replace("/ban ", ""));
                    order = getOrder(orderId, driverId);
                    addBan(order.getClient(), driverId);
                    customerRepository.saveAndFlush(order.getClient());
                } else if (callbackData.startsWith("/send_message ")) {
                    long orderId = Long.parseLong(callbackData.replace("/send_message ", ""));
                    order = getOrder(orderId, driverId);

                    if (isOrderFinished(order, driverId)) {
                        return;
                    }

                    driver.setState(DRIVER_SEND_MESSAGE);
                    employeeRepository.saveAndFlush(driver);
                    order.getClient().setState(SEND_MESSAGE);
                    customerRepository.saveAndFlush(order.getClient());
                    sendMessage(driverId, "Введите в свободной форме текст сообщения", null);
                    sendMessage(order.getClient().getId(), "Ожидайте сообщение от водителя", null);
                    log.info("Водитель {}:{} первым пишет клиенту {}",
                            driver.getLastName(), driverId, order.getClient().getId());
                } else if (callbackData.startsWith("/finish_ride ")) {
                    callbackData = callbackData.replace("/finish_ride ", "");
                    long orderId = Long.parseLong(callbackData);
                    order = getOrder(orderId, driverId);

                    if (isOrderFinished(order, driverId)) {
                        return;
                    }

                    order.setStatus(COMPLETED);
                    orderRepository.save(order);

                    if (order.getClient() != null) {
                        order.getClient().setState(START);
                        customerRepository.saveAndFlush(order.getClient());
                        sendMessage(order.getClient().getId(), "Заказ завершен", null);
                    }

                    driver.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(driver);
                    sendMessage(driverId, "Поездка <code>" + orderId + "</code> завершена", null);
                    log.info("Водитель {}:{} завершил поездку {}, стоимость {}",
                            driver.getLastName(), driverId, order.getId(), order.getPrice());
                } else if (callbackData.startsWith("/cancel_order ")) {
                    callbackData = callbackData.replace("/cancel_order ", "");
                    order = getOrder(Long.parseLong(callbackData), driverId);

                    if (isOrderFinished(order, driverId)) {
                        return;
                    }

                    order.setStatus(CANCELED);

                    if (order.getClient() != null) {
                        sendMessage(order.getClient().getId(), "Заказ отменен", null);
                        order.getClient().setState(START);
                        customerRepository.saveAndFlush(order.getClient());
                    }

                    orderRepository.saveAndFlush(order);
                    sendMessage(driverId, "Заказ <code>" + order.getId() + "</code> отменен!", null);
                    driver.setState(EMPLOYEE_ON_DUTY);
                    employeeRepository.saveAndFlush(driver);
                    log.warn("Водитель {}:{} отменил поездку {}, стоимость {}",
                            driver.getLastName(), driverId, order.getId(), order.getPrice());
                } else {
                    sendMessage(driverId, DEFAULT_ERROR_MESSAGE, null);
                }
        }
    }

    private Customer registerCustomer(long chatId) {
        Customer customer = new Customer();
        customer.setId(chatId);
        customer.setRegisteredOn(LocalDate.now());
        return customerRepository.save(customer);
    }

    private boolean isBanned(Customer customer) {
        if (customer.getStatus().equals(CustomerBanStatus.BANNED)) {
            if (customer.getBannedOn().isBefore(LocalDate.now().minusMonths(3))) {
                customer.setStatus(CustomerBanStatus.SECOND_WARN);
                customerRepository.saveAndFlush(customer);
                return false;
            }

            sendMessage(
                    customer.getId(),
                    "Сервис приостановлен до: " + customer.getBannedOn().plusMonths(3),
                    null);
            return true;
        }

        if (customer.getStatus().equals(CustomerBanStatus.SECOND_WARN)) {
            if (customer.getSecondWarn().isAfter(LocalDate.now())) {
                sendMessage(customer.getId(), "Сервис приостановлен до: " + customer.getSecondWarn(), null);
                return true;
            }

            if (customer.getSecondWarn().isBefore(LocalDate.now().minusMonths(2))) {
                customer.setStatus(CustomerBanStatus.FIRST_WARN);
                customer.setFirstWarn(LocalDate.now());
                customerRepository.save(customer);
                return false;
            }
        }

        if (customer.getStatus().equals(CustomerBanStatus.FIRST_WARN)) {
            if (customer.getFirstWarn().isAfter(LocalDate.now())) {
                sendMessage(customer.getId(), "Сервис приостановлен до: " + customer.getFirstWarn(), null);
                return true;
            }

            if (customer.getFirstWarn().isBefore(LocalDate.now().minusMonths(1))) {
                customer.setStatus(CustomerBanStatus.NO_WARN);
                customerRepository.save(customer);
                return false;
            }
        }
        return false;
    }

    private void addBan(Customer customer, long employeeId) {
        if (customer.getStatus().equals(CustomerBanStatus.SECOND_WARN)) {
            if (customer.getSecondWarn().isAfter(LocalDate.now())) {
                sendMessage(
                        employeeId,
                        "Невозможно выполнить действие до: " + customer.getSecondWarn(),
                        null
                );
                return;
            }

            customer.setStatus(CustomerBanStatus.BANNED);
            customer.setBannedOn(LocalDate.now());
            sendMessage(
                    employeeId,
                    "Пользователь <code>" + customer.getId() + "</code> добавлен в черный список",
                    null
            );
            log.info("Клиенту {} вынесено предупреждение {} водителем {}",
                    customer.getId(), CustomerBanStatus.BANNED, employeeId);
        }

        if (customer.getStatus().equals(CustomerBanStatus.FIRST_WARN)) {
            if (customer.getFirstWarn().isAfter(LocalDate.now())) {
                sendMessage(
                        employeeId,
                        "Невозможно выполнить действие до: " + customer.getFirstWarn(),
                        null
                );
                return;
            }

            customer.setStatus(CustomerBanStatus.SECOND_WARN);
            customer.setSecondWarn(LocalDate.now().plusMonths(1));
            sendMessage(
                    employeeId,
                    "Пользователю <code>" + customer.getId()
                            + "</code> приостановлен доступ до " + customer.getSecondWarn(),
                    null
            );
            log.info("Клиенту {} вынесено предупреждение {} водителем {}",
                    customer.getId(), CustomerBanStatus.SECOND_WARN, employeeId);
        }

        if (customer.getStatus().equals(CustomerBanStatus.NO_WARN)) {
            customer.setStatus(CustomerBanStatus.FIRST_WARN);
            customer.setFirstWarn(LocalDate.now().plusDays(7));
            sendMessage(
                    employeeId,
                    "Пользователю <code>" + customer.getId()
                            + "</code> приостановлен доступ до " + customer.getFirstWarn(),
                    null
            );
            log.info("Клиенту {} вынесено предупреждение {} водителем {}",
                    customer.getId(), CustomerBanStatus.FIRST_WARN, employeeId);
        }

        customerRepository.saveAndFlush(customer);
    }

    public void sendMessage(long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.enableHtml(true);

        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: " + e.getMessage() + "text: " + text + "message: " + message);
        }
    }

    public Map<String, String> prepareButtons(Map<String, String> buttonTextToCallbackData, Long orderId) {
        Map<String, String> tmp = new LinkedHashMap<>();

        for (String buttonText : buttonTextToCallbackData.keySet()) {
            tmp.put(buttonText, buttonTextToCallbackData.get(buttonText) + orderId);
        }

        return tmp;
    }

    public InlineKeyboardMarkup setKeyboard(Map<String, String> buttonTextToCallbackData) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> rows = new ArrayList<>();
        InlineKeyboardButton button;
        Iterator<String> it = buttonTextToCallbackData.keySet().iterator();

        while (it.hasNext()) {
            button = new InlineKeyboardButton();
            String text = String.valueOf(it.next());
            button.setText(text);
            button.setCallbackData(buttonTextToCallbackData.get(text));
            rows.add(button);

            if (rows.size() == 2) {
                keyboard.add(rows);
                rows = new ArrayList<>();
            }

            if (!it.hasNext()) {
                keyboard.add(rows);
            }

        }

        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private Order getOrder(long orderId, long chatId) {
        Optional<Order> order = orderRepository.findById(orderId);

        if (order.isPresent()) {
            return order.get();
        } else {
            log.error("Заказ с id: " + orderId + " не найден");
            sendMessage(chatId, "Что-то пошло не так, заказ не найден", null);
            throw new EntityNotFoundException("Заказ с id: " + orderId + " не найден");
        }
    }

    private boolean isOrderFinished(Order order, long chatId) {
        if (order.getStatus().equals(COMPLETED) || order.getStatus().equals(CANCELED)) {
            sendMessage(chatId, "Заказ <code>" + order.getId() + "</code> уже числится завершенным", null);
            return true;
        }

        return false;
    }

    private Car getCar(int carId, long chatId) {
        Optional<Car> car = carRepository.findById(carId);

        if (car.isPresent()) {
            return car.get();
        } else {
            log.error("Машина с id: " + carId + " не найдена");
            sendMessage(chatId, "Что-то пошло не так, машина не найдена", null);
            throw new EntityNotFoundException("Машина с id: " + carId + " не найдена");
        }
    }

    private void addDeleteEmployee(Employee admin, EmployeeStatus status, String messageText, long chatId) {
        String[] tmp;
        String firstName, lastName;
        long employeeId;

        try {
            tmp = messageText.split(",");
            employeeId = Long.parseLong(tmp[1].trim());
            tmp = tmp[0].trim().split(" ");
            firstName = tmp[0];
            lastName = tmp[1];
        } catch (PatternSyntaxException | NumberFormatException | IndexOutOfBoundsException e) {
            sendMessage(chatId, WRONG_FORMAT_TEXT, null);
            return;
        }

        Optional<Employee> optional = employeeRepository.findById(employeeId);

        if (optional.isEmpty()) {
            Employee employee = new Employee();
            employee.setId(employeeId);
            employee.setFirstName(firstName);
            employee.setLastName(lastName);
            employee.setStatus(status);
            employeeRepository.saveAndFlush(employee);
            sendMessage(chatId, "Сотрудник добавлен!", null);
            log.info("{} добавляет {} {}", admin.getId(), status, employeeId);
        } else {
            Car car = carRepository.findDriverCar(optional.get().getId());

            if (car != null) {
                car.setDriver(null);
                carRepository.saveAndFlush(car);
            }

            employeeRepository.delete(optional.get());
            sendMessage(chatId, "Сотрудник удален!", null);
            log.info("{} удаляет {} {}", admin.getId(), status, employeeId);
        }

        admin.setState(EMPLOYEE_ON_DUTY);
        employeeRepository.saveAndFlush(admin);
    }

    private void sendEmployees(List<Employee> list, long chatId) {
        if (list.size() == 0) {
            sendMessage(chatId, "Список сотрудников пуст!", null);
            return;
        }

        StringBuilder sb = new StringBuilder();

        for (Employee employee : list) {
            sb.append(employee.getFirstName()).append(" ").append(employee.getLastName())
                    .append(", id: <code>").append(employee.getId()).append("</code>\n");
        }

        sendMessage(chatId, sb.toString(), null);
    }

    private String getContacts() {
        List<CustomContact> contacts = customContactRepository.getAllContacts();

        if (contacts.isEmpty()) {
            return "Список контактов пуст!";
        }

        StringBuilder stringBuilder = new StringBuilder();
        contacts.stream()
                .map(CustomContact::getPhoneNumber)
                .forEach(contact -> stringBuilder.append(contact).append("\n"));

        return stringBuilder.toString();
    }

    private String getCash(LocalDate date) {
        Integer sumComplete = orderRepository.getSumCompleteByDate(date);
        Integer sumCancel = orderRepository.getSumCancelByDate(date);

        if (sumComplete == null && sumCancel == null) {
            log.info("Заказы по дате {} не найдены", date);
            return String.format("Заказы по дате %s не найдены", date);
        }

        if (sumComplete == null) {
            sumComplete = 0;
        }

        if (sumCancel == null) {
            sumCancel = 0;
        }

        return String.format("Выполнено заказов на сумму %d\nОтменено заказов на сумму %d", sumComplete, sumCancel);
    }

    private void getReport(long chatId, String date, boolean isMonthReport, boolean isLost) {
        int year, month;
        LocalDate start, end;

        try {
            if (isMonthReport) {
                String[] tmp = date.split(" ");
                year = Integer.parseInt(tmp[0]);
                month = Integer.parseInt(tmp[1]);
                YearMonth yearMonth = YearMonth.of(year, month);
                start = LocalDate.of(year, month, 1);
                end = LocalDate.of(year, month, yearMonth.atEndOfMonth().getDayOfMonth());
            } else {
                year = Integer.parseInt(date);
                start = LocalDate.of(year, 1, 1);
                end = LocalDate.of(year, 12, 31);
            }

            List<? extends ClientRequest> list;
            String fileName;

            if (isLost) {
                list = lostRepository.findByPeriod(start, end);
                fileName = "lostReport_" + LocalDate.now() + "_" + (lostReport++) + ".xlsx";
            } else {
                list = feedbackRepository.findByPeriod(start, end);
                fileName = "feedbackReport_" + LocalDate.now() + "_" + (feedbackReport++) + ".xlsx";
            }

            if (list.isEmpty()) {
                sendMessage(chatId, "За выбранный период нет данных", null);
                return;
            }

            File template = new File("Template.xlsx");
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(template.getName());

            XSSFWorkbook excelWorkbook = new XSSFWorkbook(Objects.requireNonNull(inputStream));
            excelWorkbook.setSheetName(0, date);
            XSSFSheet sheet = excelWorkbook.getSheet(date);

            XSSFCellStyle cellStyle = excelWorkbook.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFCell cell0;
            XSSFCell cell1;
            XSSFCell cell2;
            XSSFCell cell3;

            XSSFRow row;
            ClientRequest report;
            list.sort(Comparator.comparing(ClientRequest::getPublishedOn).reversed());

            for (int i = 0; i <= list.size(); i++) {
                row = sheet.createRow(i);

                cell0 = row.createCell(0);
                cell1 = row.createCell(1);
                cell2 = row.createCell(2);
                cell3 = row.createCell(3);

                cell0.setCellStyle(cellStyle);
                cell1.setCellStyle(cellStyle);
                cell2.setCellStyle(cellStyle);
                cell3.setCellStyle(cellStyle);

                if (i == 0) {
                    cell0.setCellValue("Дата");
                    cell1.setCellValue("id сообщения");
                    cell2.setCellValue("id клиента");
                    cell3.setCellValue("Сообщение клиента");
                    continue;
                }

                report = list.get(i - 1);
                cell0.setCellValue(report.getPublishedOn().toString());
                cell1.setCellValue(String.valueOf(report.getId()));
                cell2.setCellValue(String.valueOf(report.getClient().getId()));
                cell3.setCellValue(report.getText());
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
            sheet.autoSizeColumn(3);

            File result = new File(template.getParentFile(), fileName);
            result.deleteOnExit();
            OutputStream fos = new FileOutputStream(result);
            excelWorkbook.write(fos);
            fos.flush();
            fos.close();
            inputStream.close();

            InputFile inputFile = new InputFile(result);
            SendDocument send = new SendDocument(String.valueOf(chatId), inputFile);
            excelWorkbook.removePrintArea(0);
            excelWorkbook.close();
            this.execute(send);
        } catch (PatternSyntaxException | NumberFormatException | IndexOutOfBoundsException e) {
            sendMessage(chatId, WRONG_FORMAT_TEXT, null);
        } catch (IOException e) {
            sendMessage(chatId, FILE_ERROR_TEXT, null);
            log.error("Ошибка при записи в файл: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        } catch (TelegramApiException e) {
            sendMessage(chatId, FILE_ERROR_TEXT, null);
            log.error("Ошибка при отправке файла: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }
}
