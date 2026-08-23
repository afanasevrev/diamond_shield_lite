package ru.server.access.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "access_controllers")
public class AccessController {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String ip;

    @Column(name = "websocket_url", length = 500)
    private String webSocketUrl;

    @Column(name = "controller_password", length = 255)
    private String controllerPassword;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean connected;

    @Column(nullable = false)
    private boolean authenticated;

    @Column(name = "last_seen")
    private OffsetDateTime lastSeen;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public AccessController() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String value) {
        this.ip = value;
    }

    public String getWebSocketUrl() {
        return webSocketUrl;
    }

    public void setWebSocketUrl(String value) {
        this.webSocketUrl = value;
    }

    public String getControllerPassword() {
        return controllerPassword;
    }

    public void setControllerPassword(String value) {
        this.controllerPassword = value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean value) {
        this.connected = value;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean value) {
        this.authenticated = value;
    }

    public OffsetDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(OffsetDateTime value) {
        this.lastSeen = value;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}