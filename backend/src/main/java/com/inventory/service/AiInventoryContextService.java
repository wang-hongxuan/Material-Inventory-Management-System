package com.inventory.service;

import com.inventory.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AI 助手实时库存上下文服务。
 *
 * <p>每次用户提问时从 MySQL 查询当前库存、低库存预警、最近出入库和待审批出库，
 * 再把精简后的数据交给大模型，确保 AI 回答业务数据时以数据库实时结果为准。</p>
 */
@Service
public class AiInventoryContextService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public AiInventoryContextService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 构造给 AI 使用的实时库存数据摘要。 */
    public String buildContext(String question, User currentUser) {
        StringBuilder context = new StringBuilder();
        context.append("以下是从系统 MySQL 数据库实时查询到的业务数据，回答库存、低库存、出入库、审批问题时必须优先使用这些数据；没有出现在数据中的内容不要编造。\n");
        context.append("数据查询时间：").append(LocalDateTime.now().format(TIME_FORMATTER)).append("\n");
        context.append("当前登录用户：")
                .append(currentUser == null ? "未知用户" : currentUser.getName())
                .append("，角色：")
                .append(currentUser == null ? "未知" : currentUser.getRole())
                .append("\n\n");

        appendSummary(context);
        appendMatchedMaterials(context, question);
        appendLowStock(context);
        appendPendingOutbound(context, currentUser);
        appendRecentInbound(context);
        appendRecentOutbound(context);
        appendCategoryStock(context);
        return context.toString();
    }

    private void appendSummary(StringBuilder context) {
        Map<String, Object> summary = jdbcTemplate.queryForMap("""
                SELECT
                  COUNT(*) AS material_count,
                  COALESCE(SUM(stock), 0) AS total_stock,
                  COALESCE(SUM(CASE WHEN stock <= safety_stock THEN 1 ELSE 0 END), 0) AS low_stock_count,
                  COALESCE(SUM(CASE WHEN stock = 0 THEN 1 ELSE 0 END), 0) AS empty_stock_count
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
        Number pendingCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM outbound_records
                WHERE status = 'pending'
                """, Number.class);

        context.append("【库存总览】\n")
                .append("- 物资种类：").append(number(summary.get("material_count"))).append("\n")
                .append("- 当前总库存：").append(number(summary.get("total_stock"))).append("\n")
                .append("- 低库存物资数：").append(number(summary.get("low_stock_count"))).append("\n")
                .append("- 零库存物资数：").append(number(summary.get("empty_stock_count"))).append("\n")
                .append("- 今日入库数量：").append(number(todayInbound)).append("\n")
                .append("- 今日已通过出库数量：").append(number(todayOutbound)).append("\n")
                .append("- 待审批出库申请：").append(number(pendingCount)).append(" 条\n\n");
    }

    private void appendMatchedMaterials(StringBuilder context, String question) {
        List<Map<String, Object>> materials = jdbcTemplate.queryForList("""
                SELECT id, code, name, category, spec, unit, supplier, location, stock, safety_stock, updated_at
                FROM materials
                ORDER BY updated_at DESC, id DESC
                LIMIT 300
                """);
        List<Map<String, Object>> matched = materials.stream()
                .filter(row -> matchesQuestion(row, question))
                .limit(12)
                .toList();
        if (matched.isEmpty()) {
            matched = materials.stream().limit(8).toList();
            appendRows(context, "【最近更新物资】", matched, row -> "- "
                    + row.get("code") + " / " + row.get("name")
                    + "，分类：" + row.get("category")
                    + "，库存：" + number(row.get("stock")) + " " + nullToEmpty(row.get("unit"))
                    + "，安全库存：" + number(row.get("safety_stock"))
                    + "，库位：" + nullToEmpty(row.get("location"))
                    + "，更新时间：" + nullToEmpty(row.get("updated_at")));
            return;
        }
        appendRows(context, "【与问题匹配的物资】", matched, row -> "- "
                + row.get("code") + " / " + row.get("name")
                + "，分类：" + row.get("category")
                + "，规格：" + nullToEmpty(row.get("spec"))
                + "，库存：" + number(row.get("stock")) + " " + nullToEmpty(row.get("unit"))
                + "，安全库存：" + number(row.get("safety_stock"))
                + "，供应商：" + nullToEmpty(row.get("supplier"))
                + "，库位：" + nullToEmpty(row.get("location"))
                + "，更新时间：" + nullToEmpty(row.get("updated_at")));
    }

    private void appendLowStock(StringBuilder context) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT code, name, category, unit, stock, safety_stock, location
                FROM materials
                WHERE stock <= safety_stock
                ORDER BY (safety_stock - stock) DESC, id ASC
                LIMIT 10
                """);
        appendRows(context, "【低库存预警】", rows, row -> "- "
                + row.get("code") + " / " + row.get("name")
                + "，库存：" + number(row.get("stock")) + " " + nullToEmpty(row.get("unit"))
                + "，安全库存：" + number(row.get("safety_stock"))
                + "，库位：" + nullToEmpty(row.get("location")));
    }

    private void appendPendingOutbound(StringBuilder context, User currentUser) {
        boolean admin = currentUser != null && "admin".equals(currentUser.getRole());
        List<Map<String, Object>> rows = admin
                ? jdbcTemplate.queryForList("""
                        SELECT o.id, m.code, m.name, m.unit, o.quantity, o.recipient, o.operator, o.created_at
                        FROM outbound_records o
                        JOIN materials m ON o.material_id = m.id
                        WHERE o.status = 'pending'
                        ORDER BY o.created_at DESC, o.id DESC
                        LIMIT 8
                        """)
                : jdbcTemplate.queryForList("""
                        SELECT o.id, m.code, m.name, m.unit, o.quantity, o.recipient, o.operator, o.created_at
                        FROM outbound_records o
                        JOIN materials m ON o.material_id = m.id
                        WHERE o.status = 'pending' AND o.created_by = ?
                        ORDER BY o.created_at DESC, o.id DESC
                        LIMIT 8
                        """, currentUser == null ? -1 : currentUser.getId());
        appendRows(context, admin ? "【待审批出库申请】" : "【我的待审批出库申请】", rows, row -> "- 单号 "
                + row.get("id") + "，" + row.get("code") + " / " + row.get("name")
                + "，数量：" + number(row.get("quantity")) + " " + nullToEmpty(row.get("unit"))
                + "，领用人：" + nullToEmpty(row.get("recipient"))
                + "，提交人：" + nullToEmpty(row.get("operator"))
                + "，时间：" + nullToEmpty(row.get("created_at")));
    }

    private void appendRecentInbound(StringBuilder context) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT i.id, m.code, m.name, m.unit, i.quantity, i.source, i.operator, i.created_at
                FROM inbound_records i
                JOIN materials m ON i.material_id = m.id
                ORDER BY i.created_at DESC, i.id DESC
                LIMIT 6
                """);
        appendRows(context, "【最近入库记录】", rows, row -> "- 单号 "
                + row.get("id") + "，" + row.get("code") + " / " + row.get("name")
                + "，数量：" + number(row.get("quantity")) + " " + nullToEmpty(row.get("unit"))
                + "，来源/供应商：" + nullToEmpty(row.get("source"))
                + "，经办人：" + nullToEmpty(row.get("operator"))
                + "，时间：" + nullToEmpty(row.get("created_at")));
    }

    private void appendRecentOutbound(StringBuilder context) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT o.id, m.code, m.name, m.unit, o.quantity, o.recipient, o.operator,
                       COALESCE(o.status, 'approved') AS status, o.created_at
                FROM outbound_records o
                JOIN materials m ON o.material_id = m.id
                ORDER BY o.created_at DESC, o.id DESC
                LIMIT 6
                """);
        appendRows(context, "【最近出库记录】", rows, row -> "- 单号 "
                + row.get("id") + "，" + row.get("code") + " / " + row.get("name")
                + "，数量：" + number(row.get("quantity")) + " " + nullToEmpty(row.get("unit"))
                + "，领用人：" + nullToEmpty(row.get("recipient"))
                + "，状态：" + outboundStatus(row.get("status"))
                + "，经办人：" + nullToEmpty(row.get("operator"))
                + "，时间：" + nullToEmpty(row.get("created_at")));
    }

    private void appendCategoryStock(StringBuilder context) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT category, COUNT(*) AS count, COALESCE(SUM(stock), 0) AS stock
                FROM materials
                GROUP BY category
                ORDER BY stock DESC
                LIMIT 8
                """);
        appendRows(context, "【分类库存】", rows, row -> "- "
                + row.get("category") + "：物资 " + number(row.get("count"))
                + " 种，总库存 " + number(row.get("stock")));
    }

    private boolean matchesQuestion(Map<String, Object> row, String question) {
        if (!StringUtils.hasText(question)) return false;
        String normalizedQuestion = normalize(question);
        List<String> fields = List.of(
                nullToEmpty(row.get("code")),
                nullToEmpty(row.get("name")),
                nullToEmpty(row.get("category")),
                nullToEmpty(row.get("spec")),
                nullToEmpty(row.get("supplier")),
                nullToEmpty(row.get("location"))
        );
        for (String field : fields) {
            String value = normalize(field);
            if (value.length() >= 2 && normalizedQuestion.contains(value)) return true;
        }
        for (String keyword : keywords(normalizedQuestion)) {
            for (String field : fields) {
                String value = normalize(field);
                if (value.length() >= 2 && value.contains(keyword)) return true;
            }
        }
        return false;
    }

    private List<String> keywords(String normalizedQuestion) {
        String simplified = normalizedQuestion
                .replace("当前", "")
                .replace("现在", "")
                .replace("实时", "")
                .replace("查询", "")
                .replace("库存", "")
                .replace("数量", "")
                .replace("多少", "")
                .replace("还有", "")
                .replace("安全", "")
                .replace("低", "")
                .replace("预警", "")
                .replace("物资", "")
                .replace("记录", "")
                .replace("入库", "")
                .replace("出库", "")
                .replace("请", "")
                .replace("帮我", "")
                .replace("一下", "");
        List<String> result = new ArrayList<>();
        for (String item : simplified.split("[\\s,，。！？?、:：;；（）()\\[\\]{}<>《》\"'“”]+")) {
            if (item.length() >= 2) result.add(item);
        }
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private String number(Object value) {
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (Math.rint(doubleValue) == doubleValue) return String.valueOf((long) doubleValue);
            return String.format(Locale.ROOT, "%.2f", doubleValue);
        }
        return "0";
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private String outboundStatus(Object status) {
        String value = nullToEmpty(status);
        return switch (value) {
            case "pending" -> "待审批";
            case "rejected" -> "已驳回";
            default -> "已通过";
        };
    }

    private void appendRows(StringBuilder context, String title, List<Map<String, Object>> rows, RowFormatter formatter) {
        context.append(title).append("\n");
        if (rows == null || rows.isEmpty()) {
            context.append("- 暂无数据\n\n");
            return;
        }
        rows.forEach(row -> context.append(formatter.format(row)).append("\n"));
        context.append("\n");
    }

    @FunctionalInterface
    private interface RowFormatter {
        String format(Map<String, Object> row);
    }
}
