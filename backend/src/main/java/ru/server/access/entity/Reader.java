package ru.server.access.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "readers")
public class Reader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "controller_id", nullable = false)
    private AccessController controller;

    @Column(name = "reader_number", nullable = false)
    private int number;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "reader_type", nullable = false, length = 100)
    private String type;

    @Column(nullable = false)
    private int port;

    @Column(name = "exdev_number", nullable = false)
    private int exdevNumber;

    @Column(name = "exdev_direction", nullable = false)
    private int exdevDirection;

    public Reader() {
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

    public int getNumber() {
        return number;
    }

    public void setNumber(int value) {
        this.number = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int value) {
        this.port = value;
    }

    public int getExdevNumber() {
        return exdevNumber;
    }

    public void setExdevNumber(int value) {
        this.exdevNumber = value;
    }

    public int getExdevDirection() {
        return exdevDirection;
    }

    public void setExdevDirection(int value) {
        this.exdevDirection = value;
    }
}