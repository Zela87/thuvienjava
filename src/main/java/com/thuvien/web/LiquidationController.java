package com.thuvien.web;

import com.thuvien.service.LiquidationService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

@Controller
public class LiquidationController {

    private final LiquidationService liquidations;

    public LiquidationController(LiquidationService liquidations) { this.liquidations = liquidations; }

    @GetMapping("/thanh-ly")
    public String page(@RequestParam(required = false) String message, Model model) {
        model.addAttribute("candidates", liquidations.candidates());
        model.addAttribute("message", message);
        return "liquidation";
    }

    @PostMapping("/thanh-ly")
    public String liquidate(@RequestParam String barcode, @RequestParam String reason,
                            HttpSession session, RedirectAttributes redirect) {
        SessionUser user = (SessionUser) session.getAttribute("USER");
        if (user == null || !user.isStaff() || reason.isBlank()) return "redirect:/thanh-ly?message=invalid";
        try {
            liquidations.liquidate(barcode, reason, user.getUsername());
            redirect.addFlashAttribute("success", "Đã thanh lý bản sách " + barcode + ".");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            redirect.addFlashAttribute("error", "Không thể hoàn tất thanh lý. Giao dịch đã được hủy.");
        }
        return "redirect:/thanh-ly";
    }
}
