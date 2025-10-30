package com.campus.tracker.model;

public class Student {
    private int id;
    private String username;
    private String passwordHash;
    private String name;
    private String email;

    // Constructors
    public Student() {}

    public Student(String username, String passwordHash, String name, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.name = name;
        this.email = email;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}