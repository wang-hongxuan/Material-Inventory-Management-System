package com.inventory.repository;

import com.inventory.entity.OutboundRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 出库记录数据访问接口。
 */
public interface OutboundRecordRepository extends JpaRepository<OutboundRecord, Long>, JpaSpecificationExecutor<OutboundRecord> {
    /** 判断物资是否已有出库记录，删除物资时用于关联保护。 */
    boolean existsByMaterial_Id(Long materialId);

    /** 查询最近出库记录，并预加载物资信息，避免 N+1 查询。 */
    @EntityGraph(attributePaths = {"material"})
    List<OutboundRecord> findTop8ByOrderByCreatedAtDescIdDesc();
}
