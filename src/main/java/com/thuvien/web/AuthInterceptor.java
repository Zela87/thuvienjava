package com.thuvien.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Set<String> STAFF = Set.of(
            "/muon-tra", "/phieu-muon", "/doc-gia", "/sach", "/tac-gia",
            "/nxb", "/the-loai", "/the-thu-vien", "/thanh-ly", "/lich-su", "/thong-ke"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String contextPath = request.getContextPath();
        String path = request.getRequestURI().substring(contextPath.length());
        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("USER");

        if (path.startsWith("/doi-mat-khau") || path.startsWith("/thong-tin") || path.startsWith("/bao-cao")
                || path.startsWith("/dang-ky-muon") || path.startsWith("/nguoi-dung")) {
            if (user == null) {
                response.sendRedirect(contextPath + "/login");
                return false;
            }
        }
        if (path.startsWith("/dang-ky-muon") && (user == null || !user.isDocGia())) {
            response.sendRedirect(contextPath + "/login");
            return false;
        }
        if (path.startsWith("/nguoi-dung") && (user == null || !user.isAdmin())) {
            response.sendRedirect(contextPath + "/");
            return false;
        }
        for (String p : STAFF) {
            if (path.equals(p) || path.startsWith(p + "/")) {
                if (user == null || !user.isStaff()) {
                    response.sendRedirect(contextPath + "/");
                    return false;
                }
            }
        }
        return true;
    }
}
