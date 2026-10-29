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

import java.time.LocalDate;
import java.time.ZoneId;

@Controller
public class StaffLoanController {

    private final OnlineLoanService loans;

    public StaffLoanController(OnlineLoanService loans) {
        this.loans = loans;
    }

    @GetMapping("/muon-tra")
    public String queue(@RequestParam(required = false) String message, Model model) {
        model.addAttribute("requests", loans.pendingRequests());
        model.addAttribute("message", message);
        model.addAttribute("defaultDueDate", LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(14));
        return "loan-requests";
    }

    @PostMapping("/muon-tra/muon-truc-tiep")
    public String directLoan(@RequestParam String cardNumber, @RequestParam String copyId,
                             @RequestParam String dueDate, @RequestParam(defaultValue = "") String note,
                             HttpSession session, RedirectAttributes redirect) {
        SessionUser user = (SessionUser) session.getAttribute("USER");
        try {
            long loanId = loans.issueDirectLoan(cardNumber, copyId, LocalDate.parse(dueDate), note, user.getUsername());
            redirect.addFlashAttribute("success", "Đã tạo phiếu mượn #" + loanId + ".");
        } catch (LoanRuleException | java.time.format.DateTimeParseException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("error", "Không thể ghi phiếu mượn. Giao dịch đã được hủy.");
        }
        return "redirect:/muon-tra";
    }

    @PostMapping("/muon-tra/yeu-cau/duyet")
    public String approve(@RequestParam String requestId, HttpSession session, RedirectAttributes redirect) {
        SessionUser user = (SessionUser) session.getAttribute("USER");
        try {
            long loanId = loans.approveRequest(requestId, user.getUsername());
            redirect.addFlashAttribute("success", "Đã duyệt yêu cầu và tạo phiếu mượn #" + loanId + ".");
        } catch (LoanRuleException ex) {
            redirect.addFlashAttribute("error", "Không thể duyệt: " + ex.getMessage());
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("error", "Không thể ghi phiếu mượn. Giao dịch đã được hủy.");
        }
        return "redirect:/muon-tra";
    }

    @PostMapping("/muon-tra/yeu-cau/tu-choi")
    public String reject(@RequestParam String requestId, RedirectAttributes redirect) {
        try {
            if (loans.rejectRequest(requestId)) redirect.addFlashAttribute("success", "Đã từ chối yêu cầu.");
            else redirect.addFlashAttribute("error", "Yêu cầu không còn ở trạng thái chờ.");
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("error", "Không thể cập nhật yêu cầu.");
        }
        return "redirect:/muon-tra";
    }
}
