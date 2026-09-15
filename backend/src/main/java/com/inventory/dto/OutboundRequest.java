package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 出库登记请求对象。
 *
 * <p>Service 层会校验当前库存是否足够。管理员提交时直接出库并扣减库存；
 * 操作员提交时先生成待审批申请，管理员审批通过后再扣减库存。</p>
 */
public record OutboundRequest(
        @JsonAlias({"materialId", "material_id"})
        @NotNull(message = "请选择出库物资")
        Long materialId,
        @NotNull(message = "请输入出库数量")
        Double quantity,
        @NotBlank(message = "领用人或领用部门不能为空")
        String recipient,
        String purpose,
        String remark
) {
}
