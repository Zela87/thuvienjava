package com.thuvien.web;

import com.thuvien.repo.SupportReportRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

@Controller
public class SupportReportController {

    private final SupportReportRepository reports;

    public SupportReportController(SupportReportRepository reports) { this.reports = reports; }

    @GetMapping("/bao-cao")
    public String list(HttpSession session, Model model) {
        SessionUser user = (SessionUser) session.getAttribute("USER");
        model.addAttribute("reports", reports.reports(user));
        model.addAttribute("isReader", user.isDocGia());
        return "support-reports";
    }

    @PostMapping("/bao-cao/gui")
    public String submit(@RequestParam String type, @RequestParam String title, @RequestParam String body,
                         HttpSession session, RedirectAttributes redirect) {
        SessionUser user = (SessionUser) session.getAttribute("USER");
        if (!user.isDocGia()) return "redirect:/bao-cao";
        try {
            reports.submit(user.getUsername(), type, title, body);
            redirect.addFlashAttribute("success", "Đã gửi báo cáo sự cố.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("error", "Không thể gửi báo cáo. Hãy thử lại sau.");
        }
        return "redirect:/bao-cao";
    }

    @PostMapping("/bao-cao/phan-hoi")
    public String reply(@RequestParam String reportId, @RequestParam String reply,
                        HttpSession session, RedirectAttributes redirect) {
        SessionUser user = (SessionUser) session.getAttribute("USER");
        if (!user.isStaff()) return "redirect:/bao-cao";
        try {
            reports.reply(user.getUsername(), reportId, reply);
            redirect.addFlashAttribute("success", "Đã gửi phản hồi và cập nhật trạng thái.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("error", "Không thể cập nhật báo cáo. Giao dịch đã được hủy.");
        }
        return "redirect:/bao-cao";
    }
}
