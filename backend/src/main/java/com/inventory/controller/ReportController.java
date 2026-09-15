package com.inventory.controller;

import com.inventory.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 库存统计报表接口。
 *
 * <p>从物资、入库、出库数据中聚合库存总览、分类库存、
 * 出入库趋势和低库存预警，用于前端 ECharts 图表展示。</p>
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** 工作台综合数据：概览卡片、趋势、分类、库存 Top 和低库存预警。 */
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return reportService.dashboard();
    }

    /** 按天统计出入库数量趋势。 */
    @GetMapping("/movement")
    public Map<String, List<Map<String, Object>>> movement(@RequestParam(defaultValue = "14") int days) {
        return Map.of("items", reportService.movement(days));
    }

    /** 按物资分类统计库存数量。 */
    @GetMapping("/stock-by-category")
    public Map<String, List<Map<String, Object>>> stockByCategory() {
        return Map.of("items", reportService.stockByCategory());
    }

    /** 查询低于或等于安全库存的物资列表。 */
    @GetMapping("/low-stock")
    public Map<String, List<Map<String, Object>>> lowStock() {
        return Map.of("items", reportService.lowStock(100));
    }
}
