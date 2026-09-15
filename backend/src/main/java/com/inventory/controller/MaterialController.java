package com.inventory.controller;

import com.inventory.common.PageResult;
import com.inventory.dto.MaterialRequest;
import com.inventory.entity.Material;
import com.inventory.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 物资基本信息管理接口。
 *
 * <p>提供物资档案的增删改查、分页条件查询、分类选项和照片上传。
 * 物资编号在数据库层和业务层均保持唯一。</p>
 */
@RestController
@RequestMapping("/api/materials")
public class MaterialController {
    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    /** 分页查询物资，支持关键字、分类和低库存筛选。 */
    @GetMapping
    public PageResult<Material> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "false") boolean lowStock,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return materialService.list(keyword, category, lowStock, page(page), pageSize(pageSize));
    }

    /** 获取已存在的物资分类，用于前端下拉筛选和表单录入。 */
    @GetMapping("/categories/options")
    public Map<String, List<String>> categories() {
        return Map.of("items", materialService.categories());
    }

    /** 根据主键查询物资详情。 */
    @GetMapping("/{id}")
    public Material get(@PathVariable Long id) {
        return materialService.get(id);
    }

    /** 新增物资档案，新建时库存默认为 0，库存由出入库单据自动维护。 */
    @PostMapping
    public Material create(@Valid @RequestBody MaterialRequest request) {
        return materialService.create(request);
    }

    /** 编辑物资基础信息，不直接修改库存流水。 */
    @PutMapping("/{id}")
    public Material update(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        return materialService.update(id, request);
    }

    /** 上传物资照片，成功后返回可访问的图片地址。 */
    @PostMapping("/{id}/photo")
    public Map<String, String> uploadPhoto(@PathVariable Long id, @RequestParam("photo") MultipartFile file) {
        return Map.of("photo_url", materialService.uploadPhoto(id, file));
    }

    /** 删除物资；已有出入库记录的物资会被 Service 层拦截。 */
    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@PathVariable Long id) {
        materialService.delete(id);
        return Map.of("success", true);
    }

    private int page(int page) {
        return Math.max(page, 1);
    }

    private int pageSize(int pageSize) {
        return Math.max(1, Math.min(pageSize, 100));
    }
}
