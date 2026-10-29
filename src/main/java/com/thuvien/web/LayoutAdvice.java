package com.thuvien.web;

import com.thuvien.repo.LibraryRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.dao.DataAccessException;

@ControllerAdvice
public class LayoutAdvice {

    private final LibraryRepository repo;

    public LayoutAdvice(LibraryRepository repo) {
        this.repo = repo;
    }

    @ModelAttribute
    public void layout(HttpSession session, HttpServletRequest request, org.springframework.ui.Model model) {
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("USER");
        String role = user == null ? "guest" : user.getRole();
        model.addAttribute("user", user);
        model.addAttribute("hoTen", user == null ? "Khách" : user.getHoTen());
        model.addAttribute("role", role);
        model.addAttribute("username", user == null ? "" : user.getUsername());
        model.addAttribute("currentPath", request.getRequestURI());
        int unread = 0;
        if (user != null && user.isStaff()) {
            try {
                unread = repo.countUnreadSupport(role);
            } catch (DataAccessException ignored) {
                // Keep public pages usable while the local database is unavailable.
            }
        }
        model.addAttribute("unreadSupport", unread);
    }
}
