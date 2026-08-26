export interface CurrentUser {
    username: string;
}

export interface Person {
    id: number;
    lastName: string;
    firstName: string;
    middleName: string | null;
    cardId: string;
    active: boolean;
    photoUrl: string | null;
}

export interface HistoryItem {
    id: number;
    eventType: string;
    cardId: string | null;
    fullName: string | null;
    controllerName: string | null;
    deviceNumber: number | null;
    direction: number | null;
    allowed: boolean;
    removeCard: boolean | null;
    commandSource: string | null;
    eventTime: string;
}

export interface LiveCard {
    eventType: string;
    cardId: string;
    allowed: boolean;
    personId: number | null;
    fullName: string;
    photoUrl: string | null;
    controllerId: number;
    controllerName: string;
    deviceNumber: number;
    direction: number;
    eventTime: string;
}

export interface AccessController {
    id: number;
    name: string;
    ip: string;
    webSocketUrl: string | null;
    enabled: boolean;
    connected: boolean;
    authenticated: boolean;
    lastSeen: string | null;
}

export interface Reader {
    id: number;
    number: number;
    name: string;
    type: string;
    port: number;
    exdevNumber: number;
    exdevDirection: number;
}

export interface Admin {
    id: number;
    username: string;
    enabled: boolean;
    createdAt: string;
}