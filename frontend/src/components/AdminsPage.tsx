import {FormEvent, useEffect, useState} from "react";
import {api} from "../api";
import {Admin, CurrentUser} from "../types";

export default function AdminsPage() {
    const [admins, setAdmins] = useState<Admin[]>([]);
    const [currentUsername, setCurrentUsername] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    async function loadData() {
        const [adminsResponse, currentUser] =
            await Promise.all([
                api<Admin[]>("/api/admins"),
                api<CurrentUser>("/api/auth/me")
            ]);

        setAdmins(adminsResponse);
        setCurrentUsername(currentUser.username);
    }

    useEffect(() => {
        loadData().catch(handleError);
    }, []);

    function handleError(exception: unknown) {
        setError(
            exception instanceof Error
                ? exception.message
                : "Произошла ошибка"
        );
    }

    async function createAdmin(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault();
        setError("");
        setLoading(true);

        const form = event.currentTarget;
        const formData = new FormData(form);

        const username = String(
            formData.get("username") ?? ""
        ).trim();

        const password = String(
            formData.get("password") ?? ""
        );

        const passwordRepeat = String(
            formData.get("passwordRepeat") ?? ""
        );

        if (password !== passwordRepeat) {
            setError("Введённые пароли не совпадают");
            setLoading(false);
            return;
        }

        if (password.length < 8) {
            setError(
                "Пароль должен содержать не менее 8 символов"
            );
            setLoading(false);
            return;
        }

        try {
            await api<Admin>("/api/admins", {
                method: "POST",
                body: JSON.stringify({
                    username,
                    password
                })
            });

            form.reset();
            await loadData();
        } catch (exception) {
            handleError(exception);
        } finally {
            setLoading(false);
        }
    }

    async function deleteAdmin(admin: Admin) {
        if (admin.username === currentUsername) {
            setError(
                "Нельзя удалить администратора, "
                    + "под которым выполнен вход"
            );
            return;
        }

        const confirmed = window.confirm(
            `Удалить администратора "${admin.username}"?`
        );

        if (!confirmed) {
            return;
        }

        setError("");

        try {
            await api<void>(`/api/admins/${admin.id}`, {
                method: "DELETE"
            });

            await loadData();
        } catch (exception) {
            handleError(exception);
        }
    }

    return (
        <section className="panel">
            <h2>Администраторы</h2>

            <p>
                Текущий пользователь:{" "}
                <strong>{currentUsername}</strong>
            </p>

            <form
                className="data-form admin-form"
                onSubmit={createAdmin}
            >
                <label>
                    Логин
                    <input
                        name="username"
                        required
                        minLength={3}
                        maxLength={100}
                        autoComplete="off"
                    />
                </label>

                <label>
                    Пароль
                    <input
                        name="password"
                        type="password"
                        required
                        minLength={8}
                        maxLength={100}
                        autoComplete="new-password"
                    />
                </label>

                <label>
                    Повторите пароль
                    <input
                        name="passwordRepeat"
                        type="password"
                        required
                        minLength={8}
                        maxLength={100}
                        autoComplete="new-password"
                    />
                </label>

                <button disabled={loading}>
                    {loading
                        ? "Добавление..."
                        : "Добавить администратора"}
                </button>
            </form>

            {error && (
                <div className="error">
                    {error}
                </div>
            )}

            <div className="table-wrapper">
                <table>
                    <thead>
                    <tr>
                        <th>Логин</th>
                        <th>Состояние</th>
                        <th>Дата создания</th>
                        <th>Действия</th>
                    </tr>
                    </thead>

                    <tbody>
                    {admins.map(admin => {
                        const isCurrent =
                            admin.username === currentUsername;

                        return (
                            <tr key={admin.id}>
                                <td>
                                    {admin.username}

                                    {isCurrent && (
                                        <span className="current-admin">
                                            Вы
                                        </span>
                                    )}
                                </td>

                                <td>
                                    {admin.enabled
                                        ? "Активен"
                                        : "Отключён"}
                                </td>

                                <td>
                                    {new Date(
                                        admin.createdAt
                                    ).toLocaleString()}
                                </td>

                                <td>
                                    <button
                                        className="danger"
                                        disabled={isCurrent}
                                        onClick={() =>
                                            deleteAdmin(admin)
                                        }
                                    >
                                        Удалить
                                    </button>
                                </td>
                            </tr>
                        );
                    })}
                    </tbody>
                </table>
            </div>
        </section>
    );
}