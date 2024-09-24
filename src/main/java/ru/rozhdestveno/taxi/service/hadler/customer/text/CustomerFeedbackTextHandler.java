package ru.rozhdestveno.taxi.service.hadler.customer.text;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.entity.customer.CustomerRepository;
import ru.rozhdestveno.taxi.entity.customer.CustomerState;
import ru.rozhdestveno.taxi.entity.feedback.Feedback;
import ru.rozhdestveno.taxi.entity.feedback.FeedbackRepository;
import ru.rozhdestveno.taxi.service.hadler.customer.CustomerHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.time.LocalDate;
import java.util.List;

@Component
public class CustomerFeedbackTextHandler extends CustomerHandler {
    private final CustomerRepository customerRepository;
    private final FeedbackRepository feedbackRepository;

    public CustomerFeedbackTextHandler(CustomerRepository customerRepository, FeedbackRepository feedbackRepository) {
        this.customerRepository = customerRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleRequest(Customer client, String message) {
        if (!client.getState().equals(CustomerState.FEEDBACK) && next != null) {
            return next.handleRequest(client, message);
        }

        //проверка на ограничение количества отзывов в день во избежание спама
        List<Feedback> feedbacks = feedbackRepository.findLastFeedbacks(client.getId(), LocalDate.now());

        if (feedbacks != null && feedbacks.size() == 3) {
            return List.of(BotUtil.createMessage(client.getId(), "В день можно оставлять не более 3 отзывов."));
        }

        //создание нового отзыва
        Feedback feedback = new Feedback();
        feedback.setText(message);
        feedback.setClient(client);
        client.setState(CustomerState.START);
        customerRepository.saveAndFlush(client);
        feedbackRepository.save(feedback);
        return List.of(BotUtil.createMessage(client.getId(), "Принято в обработку, номер отзыва " + feedback.getId()));
    }
}
