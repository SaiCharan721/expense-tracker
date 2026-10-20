package com.saicharan.expensetracker.summary;

import com.saicharan.expensetracker.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    /** month in yyyy-MM format, defaults to the current month */
    @GetMapping
    public MonthlySummary monthly(@AuthenticationPrincipal AuthUser user,
                                  @RequestParam(required = false) String month) {
        YearMonth ym = month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
        return summaryService.monthly(user.id(), ym);
    }
}
