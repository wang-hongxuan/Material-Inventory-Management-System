package com.inventory.repository;

import com.inventory.entity.InboundRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 入库记录数据访问接口。
 */
public interface InboundRecordRepository extends JpaRepository<InboundRecord, Long>, JpaSpecificationExecutor<InboundRecord> {
    /** 判断物资是否已有入库记录，删除物资时用于关联保护。 */
    boolean existsByMaterial_Id(Long materialId);

    /** 查询最近入库记录，并预加载物资信息，避免 N+1 查询。 */
    @EntityGraph(attributePaths = {"material"})
    List<InboundRecord> findTop8ByOrderByCreatedAtDescIdDesc();
}
