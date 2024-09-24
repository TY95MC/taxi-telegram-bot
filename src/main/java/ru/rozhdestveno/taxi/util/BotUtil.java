package ru.rozhdestveno.taxi.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rozhdestveno.taxi.entity.util.ClientRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class BotUtil {

    public static SendMessage createMessage(Long chatId, String message) {
        SendMessage send = new SendMessage();
        send.setChatId(String.valueOf(chatId));
        send.setText(message);
        send.enableHtml(true);
        return send;
    }

    public static SendMessage createMessage(Long chatId, String message, Map<String, String> buttons) {
        SendMessage send = createMessage(chatId, message);
        ReplyKeyboard keyboard = createKeyboard(buttons);
        send.setReplyMarkup(keyboard);
        return send;
    }

    public static SendMessage createMessage(Long chatId, String message, Map<String, String> buttons, Long orderId) {
        SendMessage send = createMessage(chatId, message);
        buttons = prepareButtons(buttons, orderId);
        ReplyKeyboard keyboard = createKeyboard(buttons);
        send.setReplyMarkup(keyboard);
        return send;
    }

    public static SendDocument createDocument(Long chatId, InputFile file) {
        SendDocument doc = new SendDocument();
        doc.setChatId(String.valueOf(chatId));
        doc.setDocument(file);
        return doc;
    }

    public static InputFile createExcelReport(List<? extends ClientRequest> list, String date, String fileName) {
        InputFile inputFile = null;

        try {
            File template = new File("Template.xlsx");//шаблонный файл
            InputStream inputStream = BotUtil.class.getClassLoader().getResourceAsStream(template.getName());

            XSSFWorkbook excelWorkbook = new XSSFWorkbook(Objects.requireNonNull(inputStream));
            //присваивание название листа
            excelWorkbook.setSheetName(0, date);
            XSSFSheet sheet = excelWorkbook.getSheet(date);

            //создание стиля(расположения текста) ячеек
            XSSFCellStyle cellStyle = excelWorkbook.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFCell cell0;
            XSSFCell cell1;
            XSSFCell cell2;
            XSSFCell cell3;

            XSSFRow row;
            ClientRequest report;

            if (list.size() > 1) {
                list.sort(Comparator.comparing(ClientRequest::getPublishedOn).reversed());
            }

            for (int i = 0; i <= list.size(); i++) {
                //создание ряда ячеек
                row = sheet.createRow(i);

                //создание ячеек
                cell0 = row.createCell(0);
                cell1 = row.createCell(1);
                cell2 = row.createCell(2);
                cell3 = row.createCell(3);

                //установка стиля ячеек(расположение текста)
                cell0.setCellStyle(cellStyle);
                cell1.setCellStyle(cellStyle);
                cell2.setCellStyle(cellStyle);
                cell3.setCellStyle(cellStyle);

                //заполнение содержимого ячеек
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

            //подгонка размера колонок под текст
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
            sheet.autoSizeColumn(3);

            //создание результирующего файла
            File result = new File(template.getParentFile(), fileName);
            result.deleteOnExit();//удаление результирующего файла по завершению программы
            //запись в результирующий файл
            OutputStream fos = new FileOutputStream(result);
            excelWorkbook.write(fos);
            fos.flush();
            fos.close();
            inputStream.close();

            inputFile = new InputFile(result);
            excelWorkbook.removePrintArea(0);//удаление данных листа
            excelWorkbook.close();
        } catch (IOException e) {
            log.error("Ошибка при записи в файл: " + e.getMessage());
            e.printStackTrace();
        }

        return inputFile;
    }

    private static ReplyKeyboard createKeyboard(Map<String, String> buttonTextToCallbackData) {
        //клавиатура
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> rows = new ArrayList<>();
        InlineKeyboardButton button;
        Iterator<String> it = buttonTextToCallbackData.keySet().iterator();
        String text, callbackData;

        while (it.hasNext()) {
            button = new InlineKeyboardButton();
            text = String.valueOf(it.next());
            callbackData = buttonTextToCallbackData.get(text);
            button.setText(text);
            button.setCallbackData(callbackData);
            rows.add(button);

            if (rows.size() == 2) {
                keyboard.add(rows);
                rows = new ArrayList<>();
            }

            //в ряд добавляются по 2 кнопки, если кнопок нечетное количество, последняя добавляется принудительно
            if (!it.hasNext()) {
                keyboard.add(rows);
            }
        }

        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private static Map<String, String> prepareButtons(Map<String, String> buttonTextToCallbackData, Long orderId) {
        Map<String, String> tmp = new LinkedHashMap<>();

        //подготовка кнопки: текст кнопки и ее CallbackData(то, что отсылается обратно на сервер)
        for (String buttonText : buttonTextToCallbackData.keySet()) {
            tmp.put(buttonText, buttonTextToCallbackData.get(buttonText) + orderId);
        }

        return tmp;
    }
}
