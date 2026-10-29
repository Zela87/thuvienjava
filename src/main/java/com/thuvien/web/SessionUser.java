package com.thuvien.web;

import java.io.Serializable;

public class SessionUser implements Serializable {
    private String username;
    private String hoTen;
    private String role;

    public SessionUser() {
    }

    public SessionUser(String username, String hoTen, String role) {
        this.username = username;
        this.hoTen = hoTen;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isStaff() {
        return "QuanTriVien".equals(role) || "ThuThu".equals(role);
    }

    public boolean isAdmin() {
        return "QuanTriVien".equals(role);
    }

    public boolean isDocGia() {
        return "DocGia".equals(role);
    }
}
