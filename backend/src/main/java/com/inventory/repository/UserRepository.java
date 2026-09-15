package com.inventory.repository;

import com.inventory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * 系统用户数据访问接口。
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    /** 根据登录账号查询用户。 */
    Optional<User> findByUsername(String username);

    /** 校验用户名唯一性。 */
    boolean existsByUsername(String username);
}
