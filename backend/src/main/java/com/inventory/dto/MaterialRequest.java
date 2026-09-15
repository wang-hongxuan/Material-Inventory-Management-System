package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * 物资新增和编辑请求对象。
 *
 * <p>库存不允许通过该对象直接修改，必须由入库和出库业务自动计算。</p>
 */
public record MaterialRequest(
        @NotBlank(message = "物资编号不能为空") String code,
        @NotBlank(message = "物资名称不能为空") String name,
        @NotBlank(message = "分类不能为空") String category,
        String spec,
        @NotBlank(message = "单位不能为空") String unit,
        String supplier,
        String location,
        @JsonAlias({"safetyStock", "safety_stock"}) Double safetyStock,
        String remark
) {
}
