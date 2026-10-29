package com.thuvien.web;

import com.thuvien.repo.ReaderRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class ReaderController {

    private final ReaderRepository readers;

    public ReaderController(ReaderRepository readers) {
        this.readers = readers;
    }

    @GetMapping({"/doc-gia", "/the-thu-vien"})
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) String message, Model model) {
        model.addAttribute("keyword", keyword);
        model.addAttribute("message", message);
        model.addAttribute("readers", readers.readers(keyword));
        model.addAttribute("readersWithoutCard", readers.readersWithoutCard());
        return "readers";
    }

    @PostMapping("/the-thu-vien/cap")
    public String issue(@RequestParam String readerId, @RequestParam BigDecimal deposit) {
        try { readers.issueCard(readerId, deposit); return "redirect:/doc-gia?message=issued"; }
        catch (IllegalArgumentException ex) { return "redirect:/doc-gia?message=invalid"; }
        catch (DataAccessException ex) { return "redirect:/doc-gia?message=error"; }
    }

    @PostMapping("/the-thu-vien/trang-thai")
    public String setStatus(@RequestParam String cardNumber, @RequestParam int status) {
        try { readers.setCardStatus(cardNumber, status); return "redirect:/doc-gia?message=status"; }
        catch (IllegalArgumentException ex) { return "redirect:/doc-gia?message=invalid"; }
        catch (DataAccessException ex) { return "redirect:/doc-gia?message=error"; }
    }

    @PostMapping("/the-thu-vien/gia-han")
    public String renew(@RequestParam String cardNumber) {
        try { readers.renewCard(cardNumber); return "redirect:/doc-gia?message=renewed"; }
        catch (DataAccessException ex) { return "redirect:/doc-gia?message=error"; }
    }
}
