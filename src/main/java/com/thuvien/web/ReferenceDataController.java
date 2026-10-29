package com.thuvien.web;

import com.thuvien.repo.ReferenceDataRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReferenceDataController {

    private final ReferenceDataRepository data;

    public ReferenceDataController(ReferenceDataRepository data) {
        this.data = data;
    }

    @GetMapping("/the-loai")
    public String genres(Model model, @RequestParam(required = false) String message) {
        model.addAttribute("rows", data.genres());
        model.addAttribute("message", message);
        return "genres";
    }

    @PostMapping("/the-loai/them")
    public String addGenre(@RequestParam String ten) {
        try { data.addGenre(ten.trim()); return "redirect:/the-loai?message=added"; }
        catch (DataAccessException ex) { return "redirect:/the-loai?message=error"; }
    }

    @PostMapping("/the-loai/xoa")
    public String deleteGenre(@RequestParam String id) {
        try { data.deleteGenre(id); return "redirect:/the-loai?message=deleted"; }
        catch (IllegalArgumentException ex) { return "redirect:/the-loai?message=linked"; }
        catch (DataAccessException ex) { return "redirect:/the-loai?message=error"; }
    }

    @GetMapping("/tac-gia")
    public String authors(Model model, @RequestParam(required = false) String message) {
        model.addAttribute("rows", data.authors());
        model.addAttribute("message", message);
        return "authors";
    }

    @PostMapping("/tac-gia/them")
    public String addAuthor(@RequestParam String ten, @RequestParam(defaultValue = "") String chucdanh,
                            @RequestParam(defaultValue = "") String noilamviec) {
        try { data.addAuthor(ten.trim(), chucdanh.trim(), noilamviec.trim()); return "redirect:/tac-gia?message=added"; }
        catch (DataAccessException ex) { return "redirect:/tac-gia?message=error"; }
    }

    @PostMapping("/tac-gia/xoa")
    public String deleteAuthor(@RequestParam String id) {
        try { data.deleteAuthor(id); return "redirect:/tac-gia?message=deleted"; }
        catch (DataAccessException ex) { return "redirect:/tac-gia?message=linked"; }
    }

    @GetMapping("/nxb")
    public String publishers(Model model, @RequestParam(required = false) String message) {
        model.addAttribute("rows", data.publishers());
        model.addAttribute("message", message);
        return "publishers";
    }

    @PostMapping("/nxb/luu")
    public String savePublisher(@RequestParam(required = false) String id, @RequestParam String ten,
                                @RequestParam(defaultValue = "") String diachi) {
        try { data.savePublisher(id, ten.trim(), diachi.trim()); return "redirect:/nxb?message=saved"; }
        catch (DataAccessException ex) { return "redirect:/nxb?message=error"; }
    }

    @PostMapping("/nxb/xoa")
    public String deletePublisher(@RequestParam String id) {
        try { data.deletePublisher(id); return "redirect:/nxb?message=deleted"; }
        catch (IllegalArgumentException ex) { return "redirect:/nxb?message=linked"; }
        catch (DataAccessException ex) { return "redirect:/nxb?message=error"; }
    }
}
