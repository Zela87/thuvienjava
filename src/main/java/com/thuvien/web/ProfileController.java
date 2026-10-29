package com.thuvien.web;

import com.thuvien.repo.ProfileRepository;
import com.thuvien.service.AccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Controller
public class ProfileController {

    private final ProfileRepository profiles;
    private final AccountService accounts;

    public ProfileController(ProfileRepository profiles, AccountService accounts) {
        this.profiles = profiles;
        this.accounts = accounts;
    }

    @GetMapping("/thong-tin")
    public String profile(HttpSession session, Model model) {
        SessionUser user = (SessionUser) session.getAttribute("USER");
        model.addAttribute("profile", profiles.profile(user.getUsername(), user.isDocGia()));
        return "profile";
    }

    @PostMapping("/thong-tin")
    public String update(@RequestParam String name, @RequestParam(defaultValue = "") String nationalId,
                         @RequestParam(defaultValue = "") String dateOfBirth,
                         @RequestParam(defaultValue = "") String address,
                         HttpSession session, RedirectAttributes redirect) {
        SessionUser user = (SessionUser) session.getAttribute("USER");
        try {
            LocalDate birthDate = dateOfBirth.isBlank() ? null : LocalDate.parse(dateOfBirth);
            profiles.update(user.getUsername(), user.isDocGia(), name, nationalId, birthDate, address);
            user.setHoTen(name.trim());
            redirect.addFlashAttribute("success", "Đã cập nhật hồ sơ.");
        } catch (DateTimeParseException ex) {
            redirect.addFlashAttribute("error", "Ngày sinh không hợp lệ.");
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("error", "Không thể cập nhật hồ sơ trong cơ sở dữ liệu.");
        }
        return "redirect:/thong-tin";
    }

    @GetMapping("/doi-mat-khau")
    public String changePassword() { return "change-password"; }

    @PostMapping("/doi-mat-khau")
    public String changePassword(@RequestParam String oldPassword, @RequestParam String newPassword,
                                 @RequestParam String confirmPassword, HttpSession session,
                                 RedirectAttributes redirect) {
        if (newPassword.length() < 6) {
            redirect.addFlashAttribute("error", "Mật khẩu mới cần ít nhất 6 ký tự.");
            return "redirect:/doi-mat-khau";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirect.addFlashAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "redirect:/doi-mat-khau";
        }
        SessionUser user = (SessionUser) session.getAttribute("USER");
        try {
            if (!accounts.changePassword(user.getUsername(), oldPassword, newPassword)) {
                redirect.addFlashAttribute("error", "Mật khẩu hiện tại không chính xác.");
            } else {
                redirect.addFlashAttribute("success", "Đã đổi mật khẩu.");
            }
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("error", "Không thể đổi mật khẩu. Hãy kiểm tra cấu hình cơ sở dữ liệu.");
        }
        return "redirect:/doi-mat-khau";
    }
}
