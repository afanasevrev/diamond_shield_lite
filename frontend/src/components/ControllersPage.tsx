import {FormEvent, useEffect, useState} from "react";
import {api} from "../api";
import {AccessController, Reader} from "../types";

const READER_TYPES = [
    "Wiegand",
    "Barcode_terminator",
    "Barcode-USB_terminator",
    "Barcode",
    "Barcode-USB"
];

export default function ControllersPage() {
    const [controllers, setControllers] =
        useState<AccessController[]>([]);

    const [readers, setReaders] =
        useState<Record<number, Reader[]>>({});

    const [error, setError] = useState("");

    async function loadControllers() {
        const result = await api<AccessController[]>(
            "/api/controllers"
        );

        setControllers(result);

        for (const controller of result) {
            const controllerReaders = await api<Reader[]>(
                `/api/controllers/${controller.id}/readers`
            );

            setReaders(current => ({
                ...current,
                [controller.id]: controllerReaders
            }));
        }
    }

    useEffect(() => {
        loadControllers().catch(handleError);

        const interval = window.setInterval(() => {
            loadControllers().catch(handleError);
        }, 5000);

        return () => window.clearInterval(interval);
    }, []);

    function handleError(exception: unknown) {
        setError(
            exception instanceof Error
                ? exception.message
                : "Произошла ошибка"
        );
    }

    async function createController(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault();
        setError("");

        const form = event.currentTarget;
        const data = new FormData(form);

        try {
            await api<AccessController>("/api/controllers", {
                method: "POST",
                body: JSON.stringify({
                    name: data.get("name"),
                    ip: data.get("ip"),
                    webSocketUrl: data.get("webSocketUrl"),
                    password: data.get("password")
                })
            });

            form.reset();
            await loadControllers();
        } catch (exception) {
            handleError(exception);
        }
    }

    async function connect(controllerId: number) {
        try {
            await api<void>(
                `/api/controllers/${controllerId}/connect`,
                {method: "POST"}
            );

            window.setTimeout(loadControllers, 1000);
        } catch (exception) {
            handleError(exception);
        }
    }

    async function createReader(
        event: FormEvent<HTMLFormElement>,
        controllerId: number
    ) {
        event.preventDefault();

        const form = event.currentTarget;
        const data = new FormData(form);

        try {
            await api<Reader>(
                `/api/controllers/${controllerId}/readers`,
                {
                    method: "POST",
                    body: JSON.stringify({
                        number: Number(data.get("number")),
                        name: data.get("name"),
                        type: data.get("type"),
                        port: Number(data.get("port")),
                        exdevNumber: Number(data.get("exdevNumber")),
                        exdevDirection: Number(
                            data.get("exdevDirection")
                        )
                    })
                }
            );

            form.reset();
            await loadControllers();
        } catch (exception) {
            handleError(exception);
        }
    }

    async function sendStateRequest(controllerId: number) {
        try {
            await api(
                `/api/controllers/${controllerId}/command`,
                {
                    method: "POST",
                    body: JSON.stringify({
                        get: "state"
                    })
                }
            );

            alert("Запрос состояния отправлен");
        } catch (exception) {
            handleError(exception);
        }
    }

    async function open(
        controllerId: number,
        number: number,
        direction: number
    ) {
        try {
            await api(
                `/api/controllers/${controllerId}/command`,
                {
                    method: "POST",
                    body: JSON.stringify({
                        control: "exdev",
                        exdev: {
                            number,
                            direction,
                            action: "open",
                            open_type: "open once",
                            open_time: 3000
                        }
                    })
                }
            );
        } catch (exception) {
            handleError(exception);
        }
    }

    return (
        <section className="panel">
            <h2>Контроллеры</h2>

            <form
                className="data-form"
                onSubmit={createController}
            >
                <label>
                    Название
                    <input name="name" required/>
                </label>

                <label>
                    IP-адрес
                    <input
                        name="ip"
                        required
                        placeholder="192.168.1.20"
                    />
                </label>

                <label>
                    WebSocket URL
                    <input
                        name="webSocketUrl"
                        placeholder="ws://192.168.1.20:8080/ws"
                    />
                </label>

                <label>
                    Пароль контроллера
                    <input name="password" type="password"/>
                </label>

                <button>Добавить контроллер</button>
            </form>

            {error && <div className="error">{error}</div>}

            <div className="controller-list">
                {controllers.map(controller => (
                    <article
                        className="controller-card"
                        key={controller.id}
                    >
                        <div className="controller-header">
                            <div>
                                <h3>{controller.name}</h3>
                                <div>IP: {controller.ip}</div>
                                <div>
                                    WS:{" "}
                                    {controller.webSocketUrl
                                        ?? "Входящее подключение"}
                                </div>
                            </div>

                            <div>
                                <span
                                    className={
                                        controller.connected
                                            ? "online"
                                            : "offline"
                                    }
                                >
                                    {controller.connected
                                        ? "Подключён"
                                        : "Отключён"}
                                </span>

                                {controller.connected && (
                                    <span
                                        className={
                                            controller.authenticated
                                                ? "online"
                                                : "offline"
                                        }
                                    >
                                        {controller.authenticated
                                            ? "Авторизован"
                                            : "Без авторизации"}
                                    </span>
                                )}
                            </div>
                        </div>

                        <div className="actions">
                            {controller.webSocketUrl && (
                                <button
                                    onClick={() =>
                                        connect(controller.id)
                                    }
                                >
                                    Подключить
                                </button>
                            )}

                            <button
                                onClick={() =>
                                    sendStateRequest(controller.id)
                                }
                                disabled={!controller.connected}
                            >
                                Запросить состояние
                            </button>
                        </div>

                        <h4>Считыватели</h4>

                        <div className="reader-list">
                            {(readers[controller.id] ?? []).map(
                                reader => (
                                    <div
                                        className="reader"
                                        key={reader.id}
                                    >
                                        <div>
                                            <strong>
                                                {reader.name}
                                            </strong>
                                            <div>
                                                № {reader.number},{" "}
                                                {reader.type}, порт{" "}
                                                {reader.port}
                                            </div>
                                            <div>
                                                ИУ {reader.exdevNumber},
                                                направление{" "}
                                                {reader.exdevDirection}
                                            </div>
                                        </div>

                                        <button
                                            disabled={
                                                !controller.connected
                                            }
                                            onClick={() =>
                                                open(
                                                    controller.id,
                                                    reader.exdevNumber,
                                                    reader.exdevDirection
                                                )
                                            }
                                        >
                                            Открыть
                                        </button>
                                    </div>
                                )
                            )}
                        </div>

                        <form
                            className="reader-form"
                            onSubmit={event =>
                                createReader(event, controller.id)
                            }
                        >
                            <input
                                name="number"
                                type="number"
                                min="0"
                                required
                                placeholder="Номер"
                            />

                            <input
                                name="name"
                                required
                                placeholder="Название"
                            />

                            <select name="type">
                                {READER_TYPES.map(type => (
                                    <option key={type} value={type}>
                                        {type}
                                    </option>
                                ))}
                            </select>

                            <input
                                name="port"
                                type="number"
                                min="0"
                                required
                                placeholder="Порт"
                            />

                            <select name="exdevNumber">
                                <option value="0">ИУ 0</option>
                                <option value="1">ИУ 1</option>
                            </select>

                            <select name="exdevDirection">
                                <option value="0">
                                    Направление 0
                                </option>
                                <option value="1">
                                    Направление 1
                                </option>
                            </select>

                            <button>Добавить считыватель</button>
                        </form>
                    </article>
                ))}
            </div>
        </section>
    );
}