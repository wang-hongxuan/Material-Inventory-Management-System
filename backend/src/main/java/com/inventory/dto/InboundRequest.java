package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

/**
 * 入库登记请求对象。
 *
 * <p>materialId 指定入库物资，quantity 为入库数量，Service 层会校验数量必须大于 0。</p>
 */
public record InboundRequest(
        @JsonAlias({"materialId", "material_id"})
        @NotNull(message = "请选择入库物资")
        Long materialId,
        @NotNull(message = "请输入入库数量")
        Double quantity,
        @JsonAlias({"unitPrice", "unit_price"})
        Double unitPrice,
        String source,
        String remark
) {
}
