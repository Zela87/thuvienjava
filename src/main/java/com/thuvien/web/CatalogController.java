package com.thuvien.web;

import com.thuvien.repo.LibraryRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CatalogController {

    private static final int PAGE_SIZE = 12;

    private final LibraryRepository library;

    public CatalogController(LibraryRepository library) {
        this.library = library;
    }

    @GetMapping({"/", "/index", "/tra-cuu"})
    public String catalog(@RequestParam(defaultValue = "") String keyword,
                          @RequestParam(defaultValue = "TenSach") String criteria,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        String safeCriteria = switch (criteria) {
            case "TacGia", "TenTheLoai" -> criteria;
            default -> "TenSach";
        };
        model.addAttribute("keyword", keyword);
        model.addAttribute("criteria", safeCriteria);
        try {
            int totalBooks = library.countBooks(keyword, safeCriteria);
            int totalPages = (int) Math.ceil((double) totalBooks / PAGE_SIZE);
            int currentPage = Math.min(Math.max(page, 0), Math.max(totalPages - 1, 0));
            model.addAttribute("books", library.searchBooks(keyword, safeCriteria,
                    PAGE_SIZE, currentPage * PAGE_SIZE));
            model.addAttribute("totalBooks", totalBooks);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("currentPage", currentPage);
        } catch (DataAccessException ex) {
            model.addAttribute("books", java.util.List.of());
            model.addAttribute("totalBooks", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("currentPage", 0);
            model.addAttribute("catalogError", "Chưa kết nối được cơ sở dữ liệu thư viện.");
        }
        return "catalog";
    }
}
