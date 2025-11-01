package com.campus.tracker.util;

import com.campus.tracker.model.Student;

public class SessionManager {
    private static SessionManager instance;
    private Student currentStudent;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentStudent(Student student) {
        this.currentStudent = student;
    }

    public Student getCurrentStudent() {
        return currentStudent;
    }

    public int getCurrentStudentId() {
        return currentStudent != null ? currentStudent.getId() : -1;
    }

    public boolean isLoggedIn() {
        return currentStudent != null;
    }

    public void logout() {
        currentStudent = null;
    }
    public void clearSession() {
        this.currentStudent = null;
    }
}