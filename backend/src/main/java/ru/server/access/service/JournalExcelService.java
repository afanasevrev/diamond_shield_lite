package ru.server.access.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.server.access.entity.PassageEvent;
import ru.server.access.entity.Person;
import ru.server.access.repository.PassageEventRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class JournalExcelService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private static final Map<String, String> EVENT_NAMES = Map.ofEntries(
            Map.entry("card", "Предъявлена карта"),
            Map.entry("pass_personal", "Персонифицированный проход"),
            Map.entry("pass_impersonal", "Неперсонифицированный проход"),
            Map.entry("refusal_personal", "Отказ от прохода по карте"),
            Map.entry("refusal_impersonal", "Неперсонифицированный отказ"),
            Map.entry("pass_ban_personal", "Запрет прохода по карте"),
            Map.entry("pass_ban_impersonal", "Неперсонифицированный запрет"),
            Map.entry("break", "Взлом исполнительного устройства"),
            Map.entry("exdev_long_open", "Дверь долго открыта"),
            Map.entry("exdev_unlock", "Изменение блокировки ИУ"),
            Map.entry("input", "Изменение состояния входа"),
            Map.entry("output", "Изменение состояния выхода")
    );

    private final PassageEventRepository eventRepository;

    public JournalExcelService(
            PassageEventRepository eventRepository
    ) {
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exportJournal() {
        List<PassageEvent> events =
                eventRepository.findAllByOrderByEventTimeDesc();

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet("Журнал событий");

            createHeader(workbook, sheet);
            createRows(workbook, sheet, events);
            configureColumns(sheet);

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                            0,
                            Math.max(events.size(), 1),
                            0,
                            10
                    )
            );

            workbook.write(outputStream);

            return outputStream.toByteArray();
        } catch (IOException exception) {throw new IllegalStateException("Не удалось сформировать XLSX-файл", exception);
        }
    }

    private void createHeader(
            Workbook workbook,
            Sheet sheet
    ) {
        String[] headers = {
                "№",
                "Дата и время",
                "Тип события",
                "ФИО",
                "Идентификатор карты",
                "Контроллер",
                "Номер ИУ",
                "Направление",
                "Результат",
                "Карта изъята",
                "Источник команды"
        };

        CellStyle headerStyle = createHeaderStyle(workbook);
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(28);

        for (int column = 0; column < headers.length; column++) {
            Cell cell = headerRow.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createRows(
            Workbook workbook,
            Sheet sheet,
            List<PassageEvent> events
    ) {
        CellStyle defaultStyle = createDefaultStyle(workbook);
        CellStyle centeredStyle = createCenteredStyle(workbook);
        CellStyle allowedStyle = createResultStyle(workbook, IndexedColors.LIGHT_GREEN);
        CellStyle deniedStyle = createResultStyle(workbook, IndexedColors.ROSE);

        int rowNumber = 1;

        for (PassageEvent event : events) {
            Row row = sheet.createRow(rowNumber);

            createCell(
                    row,
                    0,
                    rowNumber,
                    centeredStyle
            );

            createCell(
                    row,
                    1,
                    event.getEventTime() == null
                            ? ""
                            : event.getEventTime()
                            .format(DATE_TIME_FORMATTER),
                    defaultStyle
            );

            createCell(
                    row,
                    2,
                    eventName(event.getEventType()),
                    defaultStyle
            );

            createCell(
                    row,
                    3,
                    fullName(event.getPerson()),
                    defaultStyle
            );

            createCell(
                    row,
                    4,
                    nullable(event.getCardId()),
                    defaultStyle
            );

            createCell(
                    row,
                    5,
                    event.getController() == null
                            ? ""
                            : event.getController().getName(),
                    defaultStyle
            );

            createCell(
                    row,
                    6,
                    event.getDeviceNumber() == null
                            ? ""
                            : event.getDeviceNumber(),
                    centeredStyle
            );

            createCell(
                    row,
                    7,
                    event.getDirection() == null
                            ? ""
                            : directionName(event.getDirection()),
                    centeredStyle
            );

            boolean accessEvent = isAccessEvent(
                    event.getEventType()
            );

            String result = accessEvent
                    ? event.isAllowed()
                    ? "Разрешено"
                    : "Запрещено"
                    : "Зафиксировано";

            CellStyle resultStyle = accessEvent
                    ? event.isAllowed()
                    ? allowedStyle
                    : deniedStyle
                    : centeredStyle;

            createCell(
                    row,
                    8,
                    result,
                    resultStyle
            );

            createCell(
                    row,
                    9,
                    event.getRemoveCard() == null
                            ? ""
                            : event.getRemoveCard()
                            ? "Да"
                            : "Нет",
                    centeredStyle
            );

            createCell(
                    row,
                    10,
                    commandSourceName(event.getCommandSource()),
                    defaultStyle
            );

            rowNumber++;
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(
                IndexedColors.DARK_BLUE.getIndex()
        );
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        setBorders(style);

        return style;
    }

    private CellStyle createDefaultStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        setBorders(style);

        return style;
    }

    private CellStyle createCenteredStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setWrapText(true);

        setBorders(style);

        return style;
    }

    private CellStyle createResultStyle(
            Workbook workbook,
            IndexedColors color
    ) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);

        setBorders(style);

        return style;
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void createCell(
            Row row,
            int column,
            Object value,
            CellStyle style
    ) {
        Cell cell = row.createCell(column);

        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : value.toString());
        }
        cell.setCellStyle(style);
    }

    private void configureColumns(Sheet sheet) {
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 22 * 256);
        sheet.setColumnWidth(2, 35 * 256);
        sheet.setColumnWidth(3, 38 * 256);
        sheet.setColumnWidth(4, 24 * 256);
        sheet.setColumnWidth(5, 28 * 256);
        sheet.setColumnWidth(6, 13 * 256);
        sheet.setColumnWidth(7, 18 * 256);
        sheet.setColumnWidth(8, 18 * 256);
        sheet.setColumnWidth(9, 18 * 256);
        sheet.setColumnWidth(10, 24 * 256);
    }

    private String fullName(Person person) {
        if (person == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        if (person.getLastName() != null) {
            result.append(person.getLastName());
        }

        if (person.getFirstName() != null) {
            if (!result.isEmpty()) {
                result.append(" ");
            }

            result.append(person.getFirstName());
        }

        if (person.getMiddleName() != null
                && !person.getMiddleName().isBlank()) {
            if (!result.isEmpty()) {
                result.append(" ");
            }

            result.append(person.getMiddleName());
        }

        return result.toString();
    }

    private String eventName(String eventType) {
        if (eventType == null) {
            return "";
        }

        return EVENT_NAMES.getOrDefault(eventType, eventType);
    }

    private String directionName(Integer direction) {
        if (direction == null) {
            return "";
        }

        return switch (direction) {
            case 0 -> "0";
            case 1 -> "1";
            default -> direction.toString();
        };
    }

    private String commandSourceName(String source) {
        if (source == null || source.isBlank()) {
            return "";
        }

        return switch (source) {
            case "server" -> "Сервер";
            case "remote_control" -> "Пульт управления";
            case "sensor_fault" -> "Неисправность датчика";
            default -> source;
        };
    }

    private boolean isAccessEvent(String eventType) {
        return "card".equals(eventType)
                || "pass_personal".equals(eventType)
                || "refusal_personal".equals(eventType)
                || "pass_ban_personal".equals(eventType);
    }

    private String nullable(String value) {
        return value == null ? "" : value;
    }
}