package com.toollix.analytics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {
    @GetMapping("/me/analytics")
    public String analytics() { return "analytics placeholder"; }
}
