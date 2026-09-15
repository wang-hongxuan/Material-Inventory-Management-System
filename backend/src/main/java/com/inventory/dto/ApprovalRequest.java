package com.inventory.dto;

/**
 * 出库审批请求对象。
 *
 * <p>审批驳回时可填写 remark 作为驳回原因；审批通过接口不需要请求体。</p>
 */
public record ApprovalRequest(
        String remark
) {
}
