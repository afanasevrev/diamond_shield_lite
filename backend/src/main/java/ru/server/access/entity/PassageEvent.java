package ru.server.access.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "passage_events")
public class PassageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "controller_id")
    private AccessController controller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "card_id", length = 100)
    private String cardId;

    @Column(name = "device_number")
    private Integer deviceNumber;

    private Integer direction;

    @Column(nullable = false)
    private boolean allowed;

    @Column(name = "remove_card")
    private Boolean removeCard;

    @Column(name = "command_source", length = 100)
    private String commandSource;

    @Column(name = "event_time", nullable = false)
    private OffsetDateTime eventTime;

    @Column(name = "raw_json", nullable = false, columnDefinition = "text")
    private String rawJson;

    public PassageEvent() {
    }

    @PrePersist
    public void prePersist() {
        if (eventTime == null) {
            eventTime = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public AccessController getController() {
        return controller;
    }

    public void setController(AccessController value) {
        this.controller = value;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person value) {
        this.person = value;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String value) {
        this.eventType = value;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String value) {
        this.cardId = value;
    }

    public Integer getDeviceNumber() {
        return deviceNumber;
    }

    public void setDeviceNumber(Integer value) {
        this.deviceNumber = value;
    }

    public Integer getDirection() {
        return direction;
    }

    public void setDirection(Integer value) {
        this.direction = value;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean value) {
        this.allowed = value;
    }

    public Boolean getRemoveCard() {
        return removeCard;
    }

    public void setRemoveCard(Boolean value) {
        this.removeCard = value;
    }

    public String getCommandSource() {
        return commandSource;
    }

    public void setCommandSource(String value) {
        this.commandSource = value;
    }

    public OffsetDateTime getEventTime() {
        return eventTime;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String value) {
        this.rawJson = value;
    }
}