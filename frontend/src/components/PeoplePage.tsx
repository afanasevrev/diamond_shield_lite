import {FormEvent, useEffect, useState} from "react";
import {api} from "../api";
import {Person} from "../types";

export default function PeoplePage() {
    const [people, setPeople] = useState<Person[]>([]);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    async function loadPeople() {
        setPeople(await api<Person[]>("/api/persons"));
    }

    useEffect(() => {
        loadPeople().catch(handleError);
    }, []);

    function handleError(exception: unknown) {
        setError(
            exception instanceof Error
                ? exception.message
                : "Произошла ошибка"
        );
    }

    async function submit(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault();
        setError("");
        setLoading(true);

        const form = event.currentTarget;
        const data = new FormData(form);
        const photo = data.get("photo");

        if (photo instanceof File && photo.size > 100 * 1024) {
            setError("Фотография не должна превышать 100 КБ");
            setLoading(false);
            return;
        }

        if (photo instanceof File && photo.size === 0) {
            data.delete("photo");
        }

        try {
            await api<Person>("/api/persons", {
                method: "POST",
                body: data
            });

            form.reset();
            await loadPeople();
        } catch (exception) {
            handleError(exception);
        } finally {
            setLoading(false);
        }
    }

    async function changeActive(person: Person) {
        try {
            await api<Person>(
                `/api/persons/${person.id}/active?active=${!person.active}`,
                {method: "PATCH"}
            );

            await loadPeople();
        } catch (exception) {
            handleError(exception);
        }
    }

    async function remove(person: Person) {
        if (!window.confirm(
            `Удалить ${person.lastName} ${person.firstName}?`
        )) {
            return;
        }

        try {
            await api<void>(`/api/persons/${person.id}`, {
                method: "DELETE"
            });

            await loadPeople();
        } catch (exception) {
            handleError(exception);
        }
    }

    return (
        <section className="panel">
            <h2>Люди</h2>

            <form className="data-form" onSubmit={submit}>
                <label>
                    Фамилия
                    <input name="lastName" required maxLength={100}/>
                </label>

                <label>
                    Имя
                    <input name="firstName" required maxLength={100}/>
                </label>

                <label>
                    Отчество
                    <input name="middleName" maxLength={100}/>
                </label>

                <label>
                    Идентификатор карты
                    <input name="cardId" required maxLength={100}/>
                </label>

                <label>
                    Фотография, до 100 КБ
                    <input
                        name="photo"
                        type="file"
                        accept="image/jpeg,image/png,image/webp"
                    />
                </label>

                <button disabled={loading}>
                    {loading ? "Добавление..." : "Добавить человека"}
                </button>
            </form>

            {error && <div className="error">{error}</div>}

            <div className="table-wrapper">
                <table>
                    <thead>
                    <tr>
                        <th>ФИО</th>
                        <th>Карта</th>
                        <th>Состояние</th>
                        <th>Действия</th>
                    </tr>
                    </thead>
                    <tbody>
                    {people.map(person => (
                        <tr key={person.id}>
                            <td>
                                {person.lastName} {person.firstName}{" "}
                                {person.middleName ?? ""}
                            </td>
                            <td>{person.cardId}</td>
                            <td>
                                {person.active
                                    ? "Активен"
                                    : "Заблокирован"}
                            </td>
                            <td className="actions">
                                <button
                                    onClick={() => changeActive(person)}
                                >
                                    {person.active
                                        ? "Заблокировать"
                                        : "Активировать"}
                                </button>

                                <button
                                    className="danger"
                                    onClick={() => remove(person)}
                                >
                                    Удалить
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </section>
    );
}