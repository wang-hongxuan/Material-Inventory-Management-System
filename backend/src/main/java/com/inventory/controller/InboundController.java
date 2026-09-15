package com.inventory.controller;

import com.inventory.common.PageResult;
import com.inventory.dto.InboundRequest;
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
 * 物资入库管理接口。
 *
 * <p>提供入库记录查询、入库登记和管理员删除入库记录功能。
 * 新增入库会自动增加对应物资库存。</p>
 */
@RestController
@RequestMapping("/api/inbound")
public class InboundController {
    private final StockService stockService;

    public InboundController(StockService stockService) {
        this.stockService = stockService;
    }

    /** 分页查询入库记录，支持物资、关键字和日期范围筛选。 */
    @GetMapping
    public PageResult<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return stockService.listInbound(keyword, materialId, from, to, page(page), pageSize(pageSize));
    }

    /** 新增入库记录，并在同一事务中增加物资库存。 */
    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody InboundRequest request,
                                      @AuthenticationPrincipal User currentUser) {
        return stockService.createInbound(request, currentUser);
    }

    /** 管理员删除入库记录，并同步回滚该入库数量。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Boolean> delete(@PathVariable Long id) {
        stockService.deleteInbound(id);
        return Map.of("success", true);
    }

    private int page(int page) {
        return Math.max(page, 1);
    }

    private int pageSize(int pageSize) {
        return Math.max(1, Math.min(pageSize, 100));
    }
}
