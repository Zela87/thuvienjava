package com.thuvien.web;

import com.thuvien.repo.StatisticsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StatisticsController {

    private final StatisticsRepository statistics;

    public StatisticsController(StatisticsRepository statistics) { this.statistics = statistics; }

    @GetMapping("/thong-ke")
    public String dashboard(Model model) {
        model.addAttribute("summary", statistics.summary());
        model.addAttribute("overdue", statistics.overdueBooks());
        return "statistics";
    }
}
