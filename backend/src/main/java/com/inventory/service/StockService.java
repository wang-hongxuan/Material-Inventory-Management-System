package com.inventory.service;

import com.inventory.common.ApiException;
import com.inventory.common.PageResult;
import com.inventory.dto.InboundRequest;
import com.inventory.dto.OutboundRequest;
import com.inventory.entity.InboundRecord;
import com.inventory.entity.Material;
import com.inventory.entity.OutboundRecord;
import com.inventory.entity.User;
import com.inventory.repository.InboundRecordRepository;
import com.inventory.repository.MaterialRepository;
import com.inventory.repository.OutboundRecordRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 出入库流水和库存计算服务。
 *
 * <p>入库、出库审批和删除回滚都放在事务中执行，并通过数据库悲观锁读取物资，
 * 避免并发操作造成库存计算错误。操作员出库采用“提交申请 -> 管理员审批 -> 审批通过后扣减库存”的流程，
 * 管理员本人登记出库时直接扣减库存。</p>
 */
@Service
public class StockService {
    private static final String OUTBOUND_PENDING = "pending";
    private static final String OUTBOUND_APPROVED = "approved";
    private static final String OUTBOUND_REJECTED = "rejected";

    private final MaterialRepository materialRepository;
    private final InboundRecordRepository inboundRecordRepository;
    private final OutboundRecordRepository outboundRecordRepository;

    public StockService(
            MaterialRepository materialRepository,
            InboundRecordRepository inboundRecordRepository,
            OutboundRecordRepository outboundRecordRepository
    ) {
        this.materialRepository = materialRepository;
        this.inboundRecordRepository = inboundRecordRepository;
        this.outboundRecordRepository = outboundRecordRepository;
    }

    /** 分页查询入库流水，支持物资、关键字和日期范围筛选。 */
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> listInbound(String keyword, Long materialId, LocalDate from, LocalDate to,
                                                       int page, int pageSize) {
        Page<InboundRecord> result = inboundRecordRepository.findAll((root, query, cb) -> {
            Join<InboundRecord, Material> material = root.join("material");
            List<Predicate> predicates = movementPredicates(keyword, materialId, from, to, root.get("createdAt"), material, cb);
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(cb.like(root.get("source"), like), cb.like(root.get("operator"), like)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending().and(Sort.by("id").descending())));
        return new PageResult<>(result.getContent().stream().map(this::inboundView).toList(),
                result.getTotalElements(), page, pageSize);
    }

    /** 新增入库：校验数量大于 0，并同步增加物资当前库存。 */
    @Transactional
    public Map<String, Object> createInbound(InboundRequest request, User currentUser) {
        double quantity = normalizePositive(request.quantity(), "入库数量必须大于 0");
        double unitPrice = request.unitPrice() == null ? 0D : request.unitPrice();
        if (unitPrice < 0) throw ApiException.badRequest("单价不能小于 0");

        Material material = materialRepository.findByIdForUpdate(request.materialId())
                .orElseThrow(() -> ApiException.notFound("物资不存在"));
        material.setStock(material.getStock() + quantity);
        materialRepository.save(material);

        InboundRecord record = new InboundRecord();
        record.setMaterial(material);
        record.setQuantity(quantity);
        record.setUnitPrice(unitPrice);
        record.setSource(trim(request.source()));
        record.setRemark(trim(request.remark()));
        record.setOperator(currentUser.getName());
        record.setCreatedBy(currentUser);
        return inboundView(inboundRecordRepository.save(record));
    }

    /** 删除入库记录时回滚库存；如果回滚后库存为负数则拦截。 */
    @Transactional
    public void deleteInbound(Long id) {
        InboundRecord record = inboundRecordRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("入库记录不存在"));
        Material material = materialRepository.findByIdForUpdate(record.getMaterial().getId())
                .orElseThrow(() -> ApiException.notFound("物资不存在"));
        if (material.getStock() - record.getQuantity() < 0) {
            throw ApiException.conflict("删除后库存会为负数，已拦截");
        }
        material.setStock(material.getStock() - record.getQuantity());
        inboundRecordRepository.delete(record);
        materialRepository.save(material);
    }

    /** 分页查询出库流水，支持物资、关键字和日期范围筛选。 */
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> listOutbound(String keyword, Long materialId, LocalDate from, LocalDate to,
                                                        int page, int pageSize) {
        Page<OutboundRecord> result = outboundRecordRepository.findAll((root, query, cb) -> {
            Join<OutboundRecord, Material> material = root.join("material");
            List<Predicate> predicates = movementPredicates(keyword, materialId, from, to, root.get("createdAt"), material, cb);
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("recipient"), like),
                        cb.like(root.get("purpose"), like),
                        cb.like(root.get("operator"), like)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending().and(Sort.by("id").descending())));
        return new PageResult<>(result.getContent().stream().map(this::outboundView).toList(),
                result.getTotalElements(), page, pageSize);
    }

    /** 提交出库：管理员直接扣减库存；操作员保存为待审批状态，暂不扣减库存。 */
    @Transactional
    public Map<String, Object> createOutbound(OutboundRequest request, User currentUser) {
        double quantity = normalizePositive(request.quantity(), "出库数量必须大于 0");
        Material material = materialRepository.findByIdForUpdate(request.materialId())
                .orElseThrow(() -> ApiException.notFound("物资不存在"));
        if (material.getStock() < quantity) {
            throw ApiException.conflict("当前库存 " + material.getStock() + " " + material.getUnit()
                    + "，不足以出库 " + quantity + " " + material.getUnit());
        }

        OutboundRecord record = new OutboundRecord();
        record.setMaterial(material);
        record.setQuantity(quantity);
        record.setRecipient(trim(request.recipient()));
        record.setPurpose(trim(request.purpose()));
        record.setRemark(trim(request.remark()));
        record.setOperator(currentUser.getName());
        record.setCreatedBy(currentUser);

        if (isAdmin(currentUser)) {
            material.setStock(material.getStock() - quantity);
            record.setStatus(OUTBOUND_APPROVED);
            record.setApprovalRemark("");
            record.setApprovedBy(currentUser);
            record.setApprovedAt(LocalDateTime.now());
            materialRepository.save(material);
        } else {
            record.setStatus(OUTBOUND_PENDING);
        }
        return outboundView(outboundRecordRepository.save(record));
    }

    /** 管理员审批通过出库申请：审批时再次校验库存，库存足够才扣减。 */
    @Transactional
    public Map<String, Object> approveOutbound(Long id, User currentUser) {
        OutboundRecord record = outboundRecordRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("出库申请不存在"));
        ensurePending(record);

        Material material = materialRepository.findByIdForUpdate(record.getMaterial().getId())
                .orElseThrow(() -> ApiException.notFound("物资不存在"));
        if (material.getStock() < record.getQuantity()) {
            throw ApiException.conflict("审批失败：当前库存 " + material.getStock() + " " + material.getUnit()
                    + "，不足以出库 " + record.getQuantity() + " " + material.getUnit());
        }

        material.setStock(material.getStock() - record.getQuantity());
        record.setStatus(OUTBOUND_APPROVED);
        record.setApprovalRemark("");
        record.setApprovedBy(currentUser);
        record.setApprovedAt(LocalDateTime.now());
        materialRepository.save(material);
        return outboundView(outboundRecordRepository.save(record));
    }

    /** 管理员驳回出库申请：只更新审批状态和原因，不改变库存。 */
    @Transactional
    public Map<String, Object> rejectOutbound(Long id, String remark, User currentUser) {
        OutboundRecord record = outboundRecordRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("出库申请不存在"));
        ensurePending(record);

        record.setStatus(OUTBOUND_REJECTED);
        record.setApprovalRemark(trim(remark));
        record.setApprovedBy(currentUser);
        record.setApprovedAt(LocalDateTime.now());
        return outboundView(outboundRecordRepository.save(record));
    }

    /** 删除出库记录：只有已通过记录需要回滚库存，待审批和已驳回记录删除时库存不变。 */
    @Transactional
    public void deleteOutbound(Long id) {
        OutboundRecord record = outboundRecordRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("出库记录不存在"));
        if (isOutboundApproved(record)) {
            Material material = materialRepository.findByIdForUpdate(record.getMaterial().getId())
                    .orElseThrow(() -> ApiException.notFound("物资不存在"));
            material.setStock(material.getStock() + record.getQuantity());
            materialRepository.save(material);
        }
        outboundRecordRepository.delete(record);
    }

    /** 构造出入库流水通用查询条件。 */
    private <R> List<Predicate> movementPredicates(
            String keyword,
            Long materialId,
            LocalDate from,
            LocalDate to,
            jakarta.persistence.criteria.Path<LocalDateTime> createdAt,
            Join<R, Material> material,
            jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        List<Predicate> predicates = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            predicates.add(cb.or(cb.like(material.get("code"), like), cb.like(material.get("name"), like)));
        }
        if (materialId != null && materialId > 0) predicates.add(cb.equal(material.get("id"), materialId));
        if (from != null) predicates.add(cb.greaterThanOrEqualTo(createdAt, from.atStartOfDay()));
        if (to != null) predicates.add(cb.lessThanOrEqualTo(createdAt, to.atTime(LocalTime.MAX)));
        return predicates;
    }

    private Map<String, Object> inboundView(InboundRecord record) {
        Material material = record.getMaterial();
        Map<String, Object> item = baseRecord(record.getId(), material, record.getQuantity(), record.getOperator(),
                record.getRemark(), record.getCreatedAt());
        item.put("unit_price", record.getUnitPrice());
        item.put("source", record.getSource());
        return item;
    }

    private Map<String, Object> outboundView(OutboundRecord record) {
        Material material = record.getMaterial();
        Map<String, Object> item = baseRecord(record.getId(), material, record.getQuantity(), record.getOperator(),
                record.getRemark(), record.getCreatedAt());
        item.put("recipient", record.getRecipient());
        item.put("purpose", record.getPurpose());
        item.put("status", outboundStatus(record));
        item.put("approval_status", outboundStatus(record));
        item.put("approval_remark", record.getApprovalRemark());
        item.put("approved_by", record.getApprovedBy() == null ? "" : record.getApprovedBy().getName());
        item.put("approved_at", record.getApprovedAt());
        return item;
    }

    private void ensurePending(OutboundRecord record) {
        if (!OUTBOUND_PENDING.equals(outboundStatus(record))) {
            throw ApiException.conflict("只有待审批的出库申请可以执行审批操作");
        }
    }

    private boolean isOutboundApproved(OutboundRecord record) {
        return OUTBOUND_APPROVED.equals(outboundStatus(record));
    }

    private String outboundStatus(OutboundRecord record) {
        return StringUtils.hasText(record.getStatus()) ? record.getStatus() : OUTBOUND_APPROVED;
    }

    private boolean isAdmin(User user) {
        return user != null && "admin".equals(user.getRole());
    }

    private Map<String, Object> baseRecord(Long id, Material material, Double quantity, String operator,
                                           String remark, LocalDateTime createdAt) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("material_id", material.getId());
        item.put("material_code", material.getCode());
        item.put("material_name", material.getName());
        item.put("material_unit", material.getUnit());
        item.put("quantity", quantity);
        item.put("operator", operator);
        item.put("remark", remark);
        item.put("created_at", createdAt);
        return item;
    }

    private double normalizePositive(Double value, String message) {
        if (value == null || value <= 0) throw ApiException.badRequest(message);
        return value;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
