package com.retyrment.controller;

import com.retyrment.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController extends BaseController {

    private final AnalysisService analysisService;

    @GetMapping("/networth")
    public Map<String, Object> getNetWorth() {
        String userId = getCurrentUserId();
        return analysisService.calculateNetWorth(userId);
    }

    @GetMapping("/projection")
    public Map<String, Object> getProjections(@RequestParam(defaultValue = "10") Integer years) {
        String userId = getCurrentUserId();
        return analysisService.calculateProjections(userId, years);
    }

    @GetMapping("/goals")
    public Map<String, Object> getGoalAnalysis() {
        String userId = getCurrentUserId();
        return analysisService.analyzeGoals(userId);
    }

    @GetMapping("/recommendations")
    public Map<String, Object> getRecommendations() {
        String userId = getCurrentUserId();
        return analysisService.generateRecommendations(userId);
    }

    @GetMapping("/montecarlo")
    public Map<String, Object> runMonteCarloSimulation(
            @RequestParam(defaultValue = "1000") Integer simulations,
            @RequestParam(defaultValue = "10") Integer years) {
        String userId = getCurrentUserId();
        return analysisService.runMonteCarloSimulation(userId, simulations, years);
    }

    @GetMapping("/summary")
    public Map<String, Object> getFullSummary() {
        String userId = getCurrentUserId();
        return analysisService.getFullSummary(userId);
    }
}
