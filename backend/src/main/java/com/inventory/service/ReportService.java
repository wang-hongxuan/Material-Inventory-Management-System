package com.inventory.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存统计报表服务。
 *
 * <p>通过 SQL 聚合物资库存、出入库流水和低库存数据，为前端工作台和报表页
 * 提供 ECharts 所需的数据结构。</p>
 */
@Service
public class ReportService {
    private final JdbcTemplate jdbcTemplate;

    public ReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 汇总工作台所需的概览、趋势、分类、库存排行和低库存预警数据。 */
    public Map<String, Object> dashboard() {
        Map<String, Object> summaryRow = jdbcTemplate.queryForMap("""
                SELECT
                  COUNT(*) AS material_count,
                  COALESCE(SUM(stock), 0) AS total_stock,
                  COALESCE(SUM(CASE WHEN stock <= safety_stock THEN 1 ELSE 0 END), 0) AS low_stock_count
                FROM materials
                """);
        Number todayInbound = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM inbound_records
                WHERE DATE(created_at) = CURDATE()
                """, Number.class);
        Number todayOutbound = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM outbound_records
                WHERE (status IS NULL OR status = 'approved')
                  AND DATE(created_at) = CURDATE()
                """, Number.class);

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("materialCount", value(summaryRow.get("material_count")));
        summary.put("totalStock", value(summaryRow.get("total_stock")));
        summary.put("lowStockCount", value(summaryRow.get("low_stock_count")));
        summary.put("todayInbound", value(todayInbound));
        summary.put("todayOutbound", value(todayOutbound));

        payload.put("summary", summary);
        payload.put("stockByCategory", stockByCategory());
        payload.put("movement", movement(14));
        payload.put("topStock", jdbcTemplate.queryForList("""
                SELECT id, code, name, unit, stock
                FROM materials
                ORDER BY stock DESC
                LIMIT 8
                """));
        payload.put("lowStock", lowStock(8));
        return payload;
    }

    /** 按日期补齐最近 N 天的入库、出库趋势数据。 */
    public List<Map<String, Object>> movement(int days) {
        int normalizedDays = Math.max(7, Math.min(days, 60));
        LocalDate start = LocalDate.now().minusDays(normalizedDays - 1L);
        List<Map<String, Object>> inboundRows = jdbcTemplate.queryForList("""
                SELECT DATE(created_at) AS day, COALESCE(SUM(quantity), 0) AS quantity
                FROM inbound_records
                WHERE DATE(created_at) >= ?
                GROUP BY DATE(created_at)
                """, start);
        List<Map<String, Object>> outboundRows = jdbcTemplate.queryForList("""
                SELECT DATE(created_at) AS day, COALESCE(SUM(quantity), 0) AS quantity
                FROM outbound_records
                WHERE (status IS NULL OR status = 'approved')
                  AND DATE(created_at) >= ?
                GROUP BY DATE(created_at)
                """, start);

        Map<String, Number> inbound = rowsToMap(inboundRows);
        Map<String, Number> outbound = rowsToMap(outboundRows);
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < normalizedDays; i++) {
            String day = start.plusDays(i).toString();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", day);
            item.put("inbound", value(inbound.get(day)));
            item.put("outbound", value(outbound.get(day)));
            items.add(item);
        }
        return items;
    }

    /** 按物资分类统计物资种类数和库存总量。 */
    public List<Map<String, Object>> stockByCategory() {
        return jdbcTemplate.queryForList("""
                SELECT category, COUNT(*) AS count, COALESCE(SUM(stock), 0) AS stock
                FROM materials
                GROUP BY category
                ORDER BY stock DESC
                """);
    }

    /** 查询低于或等于安全库存的物资，按缺口大小排序。 */
    public List<Map<String, Object>> lowStock(int limit) {
        return jdbcTemplate.queryForList("""
                SELECT id, code, name, unit, stock, safety_stock, location
                FROM materials
                WHERE stock <= safety_stock
                ORDER BY (safety_stock - stock) DESC, id ASC
                LIMIT ?
                """, limit);
    }

    private Map<String, Number> rowsToMap(List<Map<String, Object>> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> row.get("day").toString(),
                row -> (Number) row.get("quantity")
        ));
    }

    private double value(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0D;
    }
}
