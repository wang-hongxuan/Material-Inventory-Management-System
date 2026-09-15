package com.inventory.repository;

import com.inventory.entity.Material;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 物资数据访问接口。
 *
 * <p>继承 JpaSpecificationExecutor 以支持物资分页和组合条件查询。</p>
 */
public interface MaterialRepository extends JpaRepository<Material, Long>, JpaSpecificationExecutor<Material> {
    boolean existsByCode(String code);

    Optional<Material> findByCode(String code);

    /** 库存变更前使用悲观写锁读取物资，避免并发出入库导致库存错误。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Material m where m.id = :id")
    Optional<Material> findByIdForUpdate(@Param("id") Long id);

    /** 查询已有物资分类，用于前端筛选项。 */
    @Query("select distinct m.category from Material m where m.category <> '' order by m.category asc")
    List<String> findDistinctCategories();
}
