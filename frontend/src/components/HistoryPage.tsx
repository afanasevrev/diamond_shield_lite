import {useEffect, useState} from "react";
import {api, downloadFile} from "../api";
import {HistoryItem} from "../types";

const EVENT_NAMES: Record<string, string> = {
    card: "Предъявлена карта",
    pass_personal: "Персонифицированный проход",
    pass_impersonal: "Неперсонифицированный проход",
    refusal_personal: "Отказ в проходе",
    refusal_impersonal: "Неперсонифицированный отказ",
    pass_ban_personal: "Запрет прохода",
    pass_ban_impersonal: "Неперсонифицированный запрет",
    break: "Взлом",
    exdev_long_open: "Дверь долго открыта",
    exdev_unlock: "Изменение блокировки",
    input: "Изменение входа",
    output: "Изменение выхода"
};


export default function HistoryPage() {
    const [history, setHistory] = useState<HistoryItem[]>([]);
    const [error, setError] = useState("");
    const [exporting, setExporting] = useState(false);

    async function exportXlsx() {
    setExporting(true);
    setError("");

    try {
        const date = new Date()
            .toISOString()
            .substring(0, 10);

        await downloadFile(
            "/api/history/export.xlsx",
            `diamond-shield-journal-${date}.xlsx`
        );
    } catch (exception) {
        setError(
            exception instanceof Error
                ? exception.message
                : "Не удалось выгрузить журнал"
        );
    } finally {
        setExporting(false);
    }
    }

    useEffect(() => {
        async function load() {
            try {
                setHistory(
                    await api<HistoryItem[]>(
                        "/api/history?limit=500"
                    )
                );
                setError("");
            } catch (exception) {
                setError(
                    exception instanceof Error
                        ? exception.message
                        : "Ошибка журнала"
                );
            }
        }

        load();

        const interval = window.setInterval(load, 5000);

        return () => window.clearInterval(interval);
    }, []);

    return (
        <section className="panel">
        
            <div className="panel-title">
            <div>
                <h2>Журнал событий</h2>
                <p>История событий контроллеров</p>
            </div>

            <button
            className="excel-button"
            disabled={exporting}
            onClick={exportXlsx}
            >
            {exporting
            ? "Формирование XLSX..."
            : "Выгрузить в XLSX"}
            </button>
            </div>

            {error && <div className="error">{error}</div>}

            <div className="table-wrapper">
                <table>
                    <thead>
                    <tr>
                        <th>Время</th>
                        <th>Событие</th>
                        <th>Человек</th>
                        <th>Карта</th>
                        <th>Контроллер</th>
                        <th>ИУ / направление</th>
                        <th>Результат</th>
                    </tr>
                    </thead>
                    <tbody>
                    {history.map(item => (
                        <tr key={item.id}>
                            <td>
                                {new Date(
                                    item.eventTime
                                ).toLocaleString()}
                            </td>

                            <td>
                                {EVENT_NAMES[item.eventType]
                                    ?? item.eventType}
                            </td>

                            <td>{item.fullName ?? "Неизвестно"}</td>
                            <td>{item.cardId ?? "—"}</td>
                            <td>{item.controllerName ?? "—"}</td>

                            <td>
                                {item.deviceNumber ?? "—"} /{" "}
                                {item.direction ?? "—"}
                            </td>

                            <td>
                                {item.eventType === "card"
                                    ? item.allowed
                                        ? "Разрешено"
                                        : "Запрещено"
                                    : "Зафиксировано"}
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </section>
    );
}