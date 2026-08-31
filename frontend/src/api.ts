const API_URL = import.meta.env.VITE_API_URL || "";

export function saveCredentials(
    username: string,
    password: string
): void {
    localStorage.setItem(
        "diamondShieldCredentials",
        btoa(`${username}:${password}`)
    );
}

export function clearCredentials(): void {
    localStorage.removeItem("diamondShieldCredentials");
}

export function hasCredentials(): boolean {
    return localStorage.getItem("diamondShieldCredentials") !== null;
}

export function authorizationHeader(): string {
    const value = localStorage.getItem("diamondShieldCredentials");
    return value ? `Basic ${value}` : "";
}

export async function api<T>(
    path: string,
    options: RequestInit = {}
): Promise<T> {
    const headers = new Headers(options.headers);
    headers.set("Authorization", authorizationHeader());

    if (
        options.body !== undefined &&
        !(options.body instanceof FormData) &&
        !headers.has("Content-Type")
    ) {
        headers.set("Content-Type", "application/json");
    }

    const response = await fetch(`${API_URL}${path}`, {
        ...options,
        headers
    });

    if (!response.ok) {
        let message = `Ошибка HTTP ${response.status}`;

        try {
            const error = await response.json();
            message = error.message ?? message;
        } catch {
            const text = await response.text();
            if (text) {
                message = text;
            }
        }

        throw new Error(message);
    }

    if (response.status === 204) {
        return undefined as T;
    }

    const text = await response.text();

    return text ? JSON.parse(text) as T : undefined as T;
}

export async function loadProtectedImage(
    relativeUrl: string | null
): Promise<string | null> {
    if (!relativeUrl) {
        return null;
    }

    const response = await fetch(`${API_URL}${relativeUrl}`, {
        headers: {
            Authorization: authorizationHeader()
        }
    });

    if (!response.ok) {
        return null;
    }

    return URL.createObjectURL(await response.blob());
}

export async function subscribeToCards(
    onCard: (card: unknown) => void,
    signal: AbortSignal
): Promise<void> {
    const response = await fetch(`${API_URL}/api/live/cards`, {
        method: "GET",
        headers: {
            Authorization: authorizationHeader(),
            Accept: "text/event-stream"
        },
        signal
    });

    if (!response.ok || !response.body) {
        throw new Error("Не удалось подключиться к фоторяду");
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    let buffer = "";

    while (!signal.aborted) {
        const result = await reader.read();

        if (result.done) {
            break;
        }

        buffer += decoder.decode(result.value, {stream: true});

        const events = buffer.split("\n\n");
        buffer = events.pop() ?? "";

        for (const event of events) {
            const lines = event.split("\n");
            const eventName = lines
                .find(line => line.startsWith("event:"))
                ?.substring(6)
                .trim();

            const data = lines
                .filter(line => line.startsWith("data:"))
                .map(line => line.substring(5).trim())
                .join("");

            if (eventName === "card" && data) {
                onCard(JSON.parse(data));
            }
        }
    }
}

export async function downloadFile(
    path: string, fallbackFilename: string)
    : Promise<void> {
    const response = await fetch(`${API_URL}${path}`, {
        method: "GET",
        headers: {
            Authorization: authorizationHeader()
        }
    });

    if (!response.ok) {
        let message = `Ошибка HTTP ${response.status}`;

        try {
            const error = await response.json();
            message = error.message ?? message;
        } catch {
            // Ответ не является JSON.
        }

        throw new Error(message);
    }

    const blob = await response.blob();
    const objectUrl = URL.createObjectURL(blob);

    const link = document.createElement("a");
    link.href = objectUrl;
    link.download = getFilename(
        response.headers.get("Content-Disposition"),
        fallbackFilename
    );

    document.body.appendChild(link);
    link.click();
    link.remove();

    window.setTimeout(() => {
        URL.revokeObjectURL(objectUrl);
    }, 1000);
}

function getFilename(
    contentDisposition: string | null,
    fallbackFilename: string
): string {
    if (!contentDisposition) {
        return fallbackFilename;
    }

    const utf8Match = contentDisposition.match(
        /filename\*=UTF-8''([^;]+)/
    );

    if (utf8Match?.[1]) {
        try {
            return decodeURIComponent(utf8Match[1]);
        } catch {
            return fallbackFilename;
        }
    }

    const regularMatch = contentDisposition.match(
        /filename="?([^";]+)"?/
    );

    return regularMatch?.[1] ?? fallbackFilename;
}