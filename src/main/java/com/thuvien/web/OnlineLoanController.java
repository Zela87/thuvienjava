package com.thuvien.web;

import com.thuvien.service.OnlineLoanService;
import com.thuvien.service.OnlineLoanService.LoanRuleException;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class OnlineLoanController {

    private final OnlineLoanService loans;

    public OnlineLoanController(OnlineLoanService loans) {
        this.loans = loans;
    }

    @GetMapping("/dang-ky-muon")
    public String page(HttpSession session, Model model) {
        SessionUser user = (SessionUser) session.getAttribute("USER");
        String username = user.getUsername();
        model.addAttribute("summary", loans.readerSummary(username));
        model.addAttribute("copies", loans.availableCopies());
        model.addAttribute("requests", loans.requests(username));
        return "online-loan";
    }

    @PostMapping("/dang-ky-muon")
    public String request(@RequestParam(value = "selected_books", required = false) List<String> selected,
                          HttpSession session, RedirectAttributes redirect) {
        SessionUser user = (SessionUser) session.getAttribute("USER");
        try {
            loans.requestCopies(user.getUsername(), selected);
            redirect.addFlashAttribute("success", "Đã gửi yêu cầu mượn. Yêu cầu sẽ được nhân viên duyệt.");
        } catch (LoanRuleException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("error", "Không thể ghi yêu cầu. Hãy kiểm tra kết nối cơ sở dữ liệu.");
        }
        return "redirect:/dang-ky-muon";
    }

    @PostMapping("/dang-ky-muon/huy")
    public String cancel(@RequestParam String requestId, HttpSession session, RedirectAttributes redirect) {
        SessionUser user = (SessionUser) session.getAttribute("USER");
        try {
            boolean cancelled = loans.cancelRequest(user.getUsername(), requestId);
            redirect.addFlashAttribute(cancelled ? "success" : "error",
                    cancelled ? "Đã hủy yêu cầu đang chờ." : "Yêu cầu không còn ở trạng thái chờ hoặc không thuộc tài khoản của bạn.");
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("error", "Không thể hủy yêu cầu. Hãy thử lại sau.");
        }
        return "redirect:/dang-ky-muon";
    }
}
