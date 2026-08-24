import {useEffect, useState} from "react";
import {loadProtectedImage, subscribeToCards} from "../api";
import {LiveCard} from "../types";

interface DisplayCard extends LiveCard {
    key: string;
    imageObjectUrl: string | null;
}

export default function LivePage() {
    const [cards, setCards] = useState<DisplayCard[]>([]);
    const [connected, setConnected] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        const abortController = new AbortController();
        const objectUrls: string[] = [];

        subscribeToCards(async rawValue => {
            const card = rawValue as LiveCard;
            const imageObjectUrl = await loadProtectedImage(
                card.photoUrl
            );

            if (imageObjectUrl) {
                objectUrls.push(imageObjectUrl);
            }

            const displayCard: DisplayCard = {
                ...card,
                key: `${card.eventTime}-${card.cardId}-${Math.random()}`,
                imageObjectUrl
            };

            setCards(current => {
                const next = [displayCard, ...current].slice(0, 30);

                const removed = current.filter(
                    oldCard => !next.some(
                        nextCard => nextCard.key === oldCard.key
                    )
                );

                for (const cardToRemove of removed) {
                    if (cardToRemove.imageObjectUrl) {
                        URL.revokeObjectURL(
                            cardToRemove.imageObjectUrl
                        );
                    }
                }

                return next;
            });

            setConnected(true);
            setError("");
        }, abortController.signal).catch(exception => {
            if (!abortController.signal.aborted) {
                setConnected(false);
                setError(
                    exception instanceof Error
                        ? exception.message
                        : "Ошибка подключения"
                );
            }
        });

        return () => {
            abortController.abort();

            for (const url of objectUrls) {
                URL.revokeObjectURL(url);
            }
        };
    }, []);

    return (
        <section className="panel">
            <div className="panel-title">
                <div>
                    <h2>Фоторяд</h2>
                    <p>Предъявления карт в реальном времени</p>
                </div>

                <span className={connected ? "online" : "offline"}>
                    {connected ? "Онлайн" : "Подключение..."}
                </span>
            </div>

            {error && <div className="error">{error}</div>}

            {cards.length === 0 ? (
                <div className="empty">
                    Ожидание предъявления карты
                </div>
            ) : (
                <div className="photo-grid">
                    {cards.map(card => (
                        <article
                            className={
                                card.allowed
                                    ? "person-card allowed"
                                    : "person-card denied"
                            }
                            key={card.key}
                        >
                            {card.imageObjectUrl ? (
                                <img
                                    src={card.imageObjectUrl}
                                    alt={card.fullName}
                                />
                            ) : (
                                <div className="no-photo">
                                    Нет фотографии
                                </div>
                            )}

                            <h3>{card.fullName}</h3>

                            <dl>
                                <dt>Карта</dt>
                                <dd>{card.cardId}</dd>

                                <dt>Контроллер</dt>
                                <dd>{card.controllerName}</dd>

                                <dt>ИУ</dt>
                                <dd>{card.deviceNumber}</dd>

                                <dt>Направление</dt>
                                <dd>{card.direction}</dd>

                                <dt>Время</dt>
                                <dd>
                                    {new Date(
                                        card.eventTime
                                    ).toLocaleString()}
                                </dd>
                            </dl>

                            <strong>
                                {card.allowed
                                    ? "Доступ разрешён"
                                    : "Доступ запрещён"}
                            </strong>
                        </article>
                    ))}
                </div>
            )}
        </section>
    );
}