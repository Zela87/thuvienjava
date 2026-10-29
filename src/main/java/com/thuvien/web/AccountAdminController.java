package com.thuvien.web;

import com.thuvien.service.AccountAdministrationService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

@Controller
public class AccountAdminController {

    private final AccountAdministrationService accounts;

    public AccountAdminController(AccountAdministrationService accounts) { this.accounts = accounts; }

    @GetMapping("/nguoi-dung")
    public String page(@RequestParam(required = false) String message, Model model) {
        model.addAttribute("accounts", accounts.accounts());
        model.addAttribute("message", message);
        return "user-management";
    }

    @PostMapping("/nguoi-dung/tao")
    public String create(@RequestParam String username, @RequestParam String password,
                         @RequestParam(defaultValue = "") String name, @RequestParam String role,
                         RedirectAttributes redirect) {
        try {
            accounts.create(username, password, name, role);
            redirect.addFlashAttribute("success", "Đã tạo tài khoản.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("error", "Không thể tạo tài khoản. Kiểm tra cơ sở dữ liệu.");
        }
        return "redirect:/nguoi-dung";
    }

    @PostMapping("/nguoi-dung/trang-thai")
    public String toggle(@RequestParam String username, @RequestParam int currentStatus,
                         HttpSession session, RedirectAttributes redirect) {
        SessionUser actor = (SessionUser) session.getAttribute("USER");
        try {
            accounts.toggleStatus(username, currentStatus, actor.getUsername());
            redirect.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("error", "Không thể cập nhật trạng thái tài khoản.");
        }
        return "redirect:/nguoi-dung";
    }
}
