package com.thuvien.web;

import com.thuvien.repo.ActivityRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ActivityController {

    private final ActivityRepository activity;

    public ActivityController(ActivityRepository activity) { this.activity = activity; }

    @GetMapping("/lich-su")
    public String history(Model model) {
        model.addAttribute("activities", activity.activities());
        model.addAttribute("loans", activity.loanHistory());
        model.addAttribute("liquidated", activity.liquidatedBooks());
        return "activity-history";
    }
}
