package ru.rozhdestveno.taxi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rozhdestveno.taxi.config.BotConfig;
import ru.rozhdestveno.taxi.service.hadler.CallbackDataHandler;
import ru.rozhdestveno.taxi.service.hadler.CommandHandler;
import ru.rozhdestveno.taxi.service.hadler.TextMessageHandler;
import ru.rozhdestveno.taxi.util.BotUtil;

import java.util.ArrayList;
import java.util.List;

import static ru.rozhdestveno.taxi.constants.Constants.WRONG_FORMAT_TEXT;

@Service
@Slf4j
public class TaxiTelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;
    private final CommandHandler commandHandler;
    private final TextMessageHandler textHandler;
    private final CallbackDataHandler callbackHandler;

    public TaxiTelegramBot(BotConfig config, CommandHandler commandHandler, TextMessageHandler messageHandler,
                           CallbackDataHandler callbackHandler) {
        this.config = config;
        this.commandHandler = commandHandler;
        this.textHandler = messageHandler;
        this.callbackHandler = callbackHandler;
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

        if (update.hasMessage() && update.getMessage().isCommand()) {
            sendMessage(commandHandler.handleCommand(update.getMessage().getChatId(), update.getMessage().getText()));
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            //удаление лишних пробелов из сообщения
            String messageText = update.getMessage().getText().replaceAll("\\s+", " ").trim();
            long chatId = update.getMessage().getChatId();

            String tmp = messageText.toUpperCase();

            if (tmp.contains("DROP") || tmp.contains("UPDATE")
                    || tmp.contains("DELETE") || tmp.contains("INSERT") || tmp.contains("SELECT")) {
                sendMessage(List.of(BotUtil.createMessage(chatId, WRONG_FORMAT_TEXT)));
                return;
            }

            sendMessage(textHandler.handleText(chatId, messageText));
        } else if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage() != null) {
            sendMessage(callbackHandler.handleCallbackData(update.getCallbackQuery().getMessage().getChatId(),
                    update.getCallbackQuery().getData()));
        } else {
            sendMessage(List.of(BotUtil.createMessage(update.getMessage().getChatId(),
                    "Допускается только текстовое сообщение")));
        }
    }

    private void sendMessage(List<PartialBotApiMethod<?>> messages) {
        try {
            for (PartialBotApiMethod<?> msg : messages) {
                if (msg instanceof SendMessage) {
                    this.execute((SendMessage) msg);
                } else {
                    this.execute((SendDocument) msg);
                }
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
