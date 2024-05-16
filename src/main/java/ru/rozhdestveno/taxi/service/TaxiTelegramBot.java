package ru.rozhdestveno.taxi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rozhdestveno.taxi.config.BotConfig;

import java.util.ArrayList;
import java.util.List;

import static ru.rozhdestveno.taxi.constants.Constants.HELP_COMMAND;

@Component
@Slf4j
public class TaxiTelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;

    public TaxiTelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> commandList = new ArrayList<>();
        commandList.add(new BotCommand("/start", "Запуск бота"));
        commandList.add(new BotCommand("/help", "Информация о боте и нашем такси"));
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
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    prepareAndSendMessage(chatId, "Здравствуйте " + update.getMessage().getChat().getFirstName() + "!");
                    break;
                case "/help":
                    sendMessage(chatId, HELP_COMMAND);
                    break;
                default: sendMessage(chatId, "Команда не распознана");
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "/order":
                    sendMessage(chatId, "такси заказано!");
                    break;
                case "/cancel_order":
                    sendMessage(chatId, "Заказ отменен!");
                    break;
                case "/lost_and_found":
                    sendMessage(chatId, "Ваша заявка принята, мы свяжемся с вами!");
                    break;
                case "/feedback":
                    sendMessage(chatId, "Спасибо за отзыв!");
                    break;
            }
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message:" + e.getMessage());
        }
    }
    public void prepareAndSendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        setClientMenuKeyboard(message);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message:" + e.getMessage());
        }
    }

    public void setClientMenuKeyboard(SendMessage message) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton button1 = new InlineKeyboardButton("Заказать такси");
        InlineKeyboardButton button2 = new InlineKeyboardButton("Отменить заказ");
        InlineKeyboardButton button3 = new InlineKeyboardButton("Оставлена вещь в машине");
        InlineKeyboardButton button4 = new InlineKeyboardButton("Оставить отзыв");

        button1.setCallbackData("/order");
        button2.setCallbackData("/cancel_order");
        button3.setCallbackData("/lost_and_found");
        button4.setCallbackData("/feedback");

        List<InlineKeyboardButton> row1 = List.of(button1, button2);
        List<InlineKeyboardButton> row2 = List.of(button3, button4);

        inlineKeyboardMarkup.setKeyboard(List.of(row1, row2));
        message.setReplyMarkup(inlineKeyboardMarkup);
    }
}
