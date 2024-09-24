package ru.rozhdestveno.taxi.service.hadler;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;

import java.util.List;

public interface TextMessageHandler {
    List<PartialBotApiMethod<?>> handleText(Long chatId, String message);
}
