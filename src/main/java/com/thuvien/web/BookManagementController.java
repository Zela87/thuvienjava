package com.thuvien.web;

import com.thuvien.repo.ReferenceDataRepository;
import com.thuvien.service.BookManagementService;
import com.thuvien.service.BookManagementService.CopyRemovalResult;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

@Controller
public class BookManagementController {

    private final BookManagementService books;
    private final ReferenceDataRepository references;

    public BookManagementController(BookManagementService books, ReferenceDataRepository references) {
        this.books = books;
        this.references = references;
    }

    @GetMapping("/sach")
    public String manage(Model model, @RequestParam(required = false) String message,
                         @RequestParam(required = false) Integer deleted,
                         @RequestParam(required = false) Integer liquidated,
                         @RequestParam(required = false) Integer borrowing) {
        model.addAttribute("books", books.books());
        model.addAttribute("copies", books.copies());
        model.addAttribute("genres", references.genres());
        model.addAttribute("authors", references.authors());
        model.addAttribute("publishers", references.publishers());
        model.addAttribute("message", message);
        model.addAttribute("currentYear", Year.now().getValue());
        model.addAttribute("deleted", deleted);
        model.addAttribute("liquidated", liquidated);
        model.addAttribute("borrowing", borrowing);
        return "books-manage";
    }

    @PostMapping("/sach/them")
    public String create(@RequestParam String ten, @RequestParam int nam, @RequestParam BigDecimal gia,
                         @RequestParam String theloai, @RequestParam String ma_tacgia,
                         @RequestParam String ma_nxb_select) {
        try {
            if (ten.isBlank() || nam < 1000 || gia.signum() < 0) throw new IllegalArgumentException();
            books.createBook(ten.trim(), nam, gia, theloai, ma_nxb_select, ma_tacgia);
            return "redirect:/sach?message=book-added";
        } catch (IllegalArgumentException ex) {
            return "redirect:/sach?message=invalid";
        } catch (DataAccessException ex) {
            return "redirect:/sach?message=error";
        }
    }

    @PostMapping("/sach/ban-sao/them")
    public String addCopies(@RequestParam String bookId, @RequestParam int quantity) {
        try { books.addCopies(bookId, quantity); return "redirect:/sach?message=copies-added"; }
        catch (IllegalArgumentException ex) { return "redirect:/sach?message=invalid"; }
        catch (DataAccessException ex) { return "redirect:/sach?message=error"; }
    }

    @PostMapping("/sach/ban-sao/xoa")
    public String removeCopies(@RequestParam(value = "selectedCopies", required = false) List<String> selected,
                               jakarta.servlet.http.HttpSession session) {
        if (selected == null || selected.isEmpty()) return "redirect:/sach?message=no-selection";
        SessionUser user = (SessionUser) session.getAttribute("USER");
        try {
            CopyRemovalResult result = books.removeCopies(selected, user.getUsername());
            return "redirect:/sach?message=copies-removed&deleted=" + result.deleted()
                    + "&liquidated=" + result.liquidated() + "&borrowing=" + result.borrowing();
        } catch (DataAccessException ex) {
            return "redirect:/sach?message=error";
        }
    }

    @PostMapping("/sach/xoa")
    public String deleteBook(@RequestParam String id) {
        try { books.deleteBook(id); return "redirect:/sach?message=book-deleted"; }
        catch (IllegalArgumentException ex) { return "redirect:/sach?message=has-history"; }
        catch (DataAccessException ex) { return "redirect:/sach?message=error"; }
    }
}
