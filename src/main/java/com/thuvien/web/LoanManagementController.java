package com.thuvien.web;

import com.thuvien.service.LoanManagementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoanManagementController {

    private final LoanManagementService loans;

    public LoanManagementController(LoanManagementService loans) {
        this.loans = loans;
    }

    @GetMapping("/phieu-muon")
    public String activeLoans(@RequestParam(defaultValue = "") String search,
                              @RequestParam(defaultValue = "") String filterStatus,
                              Model model) {
        String filter = switch (filterStatus) {
            case "late", "valid" -> filterStatus;
            default -> "";
        };
        model.addAttribute("search", search);
        model.addAttribute("filterStatus", filter);
        model.addAttribute("loans", loans.activeLoans(search, filter));
        return "active-loans";
    }

    @PostMapping("/phieu-muon/tra")
    public String returnCopy(@RequestParam String barcode, @RequestParam String condition,
                             @RequestParam(defaultValue = "") String note, HttpSession session,
                             RedirectAttributes redirect) {
        SessionUser user = (SessionUser) session.getAttribute("USER");
        try {
            var result = loans.returnCopy(barcode, condition, note, user.getUsername());
            redirect.addFlashAttribute("success", "Đã nhận trả " + result.barcode()
                    + ". Phạt: " + result.fine().toPlainString() + "đ.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("error", "Không thể ghi nhận trả sách. Giao dịch đã được hủy.");
        }
        return "redirect:/phieu-muon";
    }
}
