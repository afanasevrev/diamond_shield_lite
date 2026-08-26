import {useEffect, useState} from "react";
import {api, clearCredentials, hasCredentials} from "./api";
import {CurrentUser} from "./types";
import LoginPage from "./components/LoginPage";
import LivePage from "./components/LivePage";
import PeoplePage from "./components/PeoplePage";
import HistoryPage from "./components/HistoryPage";
import ControllersPage from "./components/ControllersPage";
import AdminsPage from "./components/AdminsPage";


type Page = "live" | "people" | "history" | "controllers" | "admins";

export default function App() {
    const [authenticated, setAuthenticated] =
        useState<boolean | null>(null);

    const [page, setPage] = useState<Page>("live");

    useEffect(() => {
        if (!hasCredentials()) {
            setAuthenticated(false);
            return;
        }

        api<CurrentUser>("/api/auth/me")
            .then(() => setAuthenticated(true))
            .catch(() => {
                clearCredentials();
                setAuthenticated(false);
            });
    }, []);

    if (authenticated === null) {
        return <div className="center-page">Загрузка...</div>;
    }

    if (!authenticated) {
        return (
            <LoginPage
                onSuccess={() => setAuthenticated(true)}
            />
        );
    }

    return (
        <div className="application">
            <header className="header">
                <div>
                    <h1>Diamond Shield - Lite</h1>
                    <small>Система контроля доступа</small>
                </div>

                <nav>
                    <button onClick={() => setPage("live")}>
                        Фоторяд
                    </button>

                    <button onClick={() => setPage("people")}>
                        Люди
                    </button>

                    <button onClick={() => setPage("history")}>
                        Журнал
                    </button>

                    <button onClick={() => setPage("controllers")}>
                        Контроллеры
                    </button>

                    <button onClick={() => setPage("admins")}>
                        Администраторы
                    </button>

                    <button
                        className="danger"
                        onClick={() => {
                            clearCredentials();
                            setAuthenticated(false);
                        }}
                    >
                        Выйти
                    </button>
                </nav>
            </header>

            <main>
                {page === "live" && <LivePage/>}
                {page === "people" && <PeoplePage/>}
                {page === "history" && <HistoryPage/>}
                {page === "controllers" && <ControllersPage/>}
                {page === "admins" && <AdminsPage/>}
            </main>
        </div>
    );
}