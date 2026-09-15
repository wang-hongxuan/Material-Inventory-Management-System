package com.inventory.controller;

import com.inventory.common.PageResult;
import com.inventory.dto.ApprovalRequest;
import com.inventory.dto.OutboundRequest;
import com.inventory.entity.User;
import com.inventory.service.StockService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * 物资出库管理接口。
 *
 * <p>提供出库记录查询、出库申请、管理员审批和管理员删除功能。
 * 操作员提交后库存不变，只有管理员审批通过后才扣减库存；
 * 管理员本人登记出库时直接扣减库存。</p>
 */
@RestController
@RequestMapping("/api/outbound")
public class OutboundController {
    private final StockService stockService;

    public OutboundController(StockService stockService) {
        this.stockService = stockService;
    }

    /** 分页查询出库记录，支持物资、关键字和日期范围筛选。 */
    @GetMapping
    public PageResult<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return stockService.listOutbound(keyword, materialId, from, to, page(page), pageSize(pageSize));
    }

    /** 提交出库：管理员直接出库并扣减库存；操作员提交为待审批申请。 */
    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody OutboundRequest request,
                                      @AuthenticationPrincipal User currentUser) {
        return stockService.createOutbound(request, currentUser);
    }

    /** 管理员审批通过出库申请，审批通过后才真正扣减库存。 */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> approve(@PathVariable Long id,
                                       @AuthenticationPrincipal User currentUser) {
        return stockService.approveOutbound(id, currentUser);
    }

    /** 管理员驳回出库申请，库存保持不变。 */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> reject(@PathVariable Long id,
                                      @RequestBody(required = false) ApprovalRequest request,
                                      @AuthenticationPrincipal User currentUser) {
        String remark = request == null ? "" : request.remark();
        return stockService.rejectOutbound(id, remark, currentUser);
    }

    /** 管理员删除出库记录；已通过记录会回滚库存，待审批/已驳回记录库存不变。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Boolean> delete(@PathVariable Long id) {
        stockService.deleteOutbound(id);
        return Map.of("success", true);
    }

    private int page(int page) {
        return Math.max(page, 1);
    }

    private int pageSize(int pageSize) {
        return Math.max(1, Math.min(pageSize, 100));
    }
}
