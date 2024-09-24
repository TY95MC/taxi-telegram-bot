package ru.rozhdestveno.taxi.service.hadler;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;

import java.util.List;

public interface CallbackDataHandler {
    List<PartialBotApiMethod<?>> handleCallbackData(Long chatId, String data);
}
