package ru.rozhdestveno.taxi.service.hadler;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;

import java.util.List;

public interface CommandHandler {
    List<PartialBotApiMethod<?>> handleCommand(Long chatId, String command);
}
