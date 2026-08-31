import {FormEvent, useState} from "react";
import {api, clearCredentials, saveCredentials} from "../api";
import {CurrentUser} from "../types";

interface Props {
    onSuccess: () => void;
}

export default function LoginPage({onSuccess}: Props) {
    const [username, setUsername] = useState("diamond");
    const [password, setPassword] = useState("DiamondAdmin2026Secure");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    async function submit(event: FormEvent) {
        event.preventDefault();
        setError("");
        setLoading(true);

        saveCredentials(username, password);

        try {
            await api<CurrentUser>("/api/auth/me");
            onSuccess();
        } catch (exception) {
            clearCredentials();
            setError(
                exception instanceof Error
                    ? exception.message
                    : "Ошибка авторизации"
            );
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="center-page">
            <form className="login-form" onSubmit={submit}>
                <h1>Diamond Shield - Lite</h1>
                <h2>Вход в систему</h2>

                <label>
                    Логин
                    <input
                        value={username}
                        onChange={event =>
                            setUsername(event.target.value)
                        }
                        required
                        autoComplete="username"
                    />
                </label>

                <label>
                    Пароль
                    <input
                        type="password"
                        value={password}
                        onChange={event =>
                            setPassword(event.target.value)
                        }
                        required
                        autoComplete="current-password"
                    />
                </label>

                <button disabled={loading}>
                    {loading ? "Вход..." : "Войти"}
                </button>

                {error && <div className="error">{error}</div>}
            </form>
        </div>
    );
}