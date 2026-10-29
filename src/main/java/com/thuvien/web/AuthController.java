package com.thuvien.web;

import com.thuvien.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Controller
public class AuthController {

    private final AccountService accounts;

    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/login")
    public String login(HttpSession session) {
        return session.getAttribute("USER") == null ? "login" : "redirect:/";
    }

    @GetMapping("/register")
    public String register(HttpSession session) {
        return session.getAttribute("USER") == null ? "register" : "redirect:/";
    }

    @PostMapping("/register")
    public String register(@RequestParam String hoten,
                           @RequestParam String username,
                           @RequestParam String cccd,
                           @RequestParam String ngaysinh,
                           @RequestParam String diachi,
                           @RequestParam String password,
                           @RequestParam String repassword,
                           org.springframework.ui.Model model) {
        String cleanUsername = username.trim();
        model.addAttribute("form", new RegisterForm(hoten, cleanUsername, cccd, ngaysinh, diachi));
        if (cleanUsername.length() < 3 || password.length() < 6
                || hoten.isBlank() || cccd.isBlank() || diachi.isBlank()) {
            model.addAttribute("error", "Tên đăng nhập cần ít nhất 3 ký tự và mật khẩu ít nhất 6 ký tự.");
            return "register";
        }
        if (!password.equals(repassword)) {
            model.addAttribute("error", "Mật khẩu nhập lại không khớp.");
            return "register";
        }
        try {
            LocalDate dateOfBirth = LocalDate.parse(ngaysinh);
            accounts.registerReader(cleanUsername, password, hoten.trim(), cccd.trim(),
                    dateOfBirth, diachi.trim());
            return "redirect:/login?registered";
        } catch (DateTimeParseException ex) {
            model.addAttribute("error", "Ngày sinh không hợp lệ.");
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "Tên đăng nhập hoặc CCCD đã được sử dụng.");
        } catch (RuntimeException ex) {
            model.addAttribute("error", "Không thể tạo tài khoản. Hãy kiểm tra cấu hình cơ sở dữ liệu.");
        }
        return "register";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        HttpServletRequest request, org.springframework.ui.Model model) {
        try {
            SessionUser user = accounts.authenticate(username.trim(), password);
            if (user == null) {
                model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng.");
                model.addAttribute("username", username);
                return "login";
            }
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) oldSession.invalidate();
            request.getSession(true).setAttribute("USER", user);
            return "redirect:/";
        } catch (RuntimeException ex) {
            model.addAttribute("error", "Không thể đăng nhập. Hãy kiểm tra kết nối cơ sở dữ liệu.");
            model.addAttribute("username", username);
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }

    public record RegisterForm(String hoten, String username, String cccd, String ngaysinh, String diachi) {}
}
