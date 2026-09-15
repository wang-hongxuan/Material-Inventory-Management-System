package com.inventory.service;

import com.inventory.entity.InboundRecord;
import com.inventory.entity.Material;
import com.inventory.entity.OutboundRecord;
import com.inventory.entity.User;
import com.inventory.repository.InboundRecordRepository;
import com.inventory.repository.MaterialRepository;
import com.inventory.repository.OutboundRecordRepository;
import com.inventory.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 初始化演示数据。
 *
 * <p>首次启动且数据库为空时自动创建管理员、操作员和若干物资出入库样例，
 * 便于打开系统后直接查看统计图表和基础业务流程。</p>
 */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final MaterialRepository materialRepository;
    private final InboundRecordRepository inboundRecordRepository;
    private final OutboundRecordRepository outboundRecordRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            MaterialRepository materialRepository,
            InboundRecordRepository inboundRecordRepository,
            OutboundRecordRepository outboundRecordRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.materialRepository = materialRepository;
        this.inboundRecordRepository = inboundRecordRepository;
        this.outboundRecordRepository = outboundRecordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** 应用启动后执行一次数据检查和初始化。 */
    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(user("admin", "admin123", "系统管理员", "admin"));
            userRepository.save(user("operator", "operator123", "仓库操作员", "operator"));
        }
        if (materialRepository.count() > 0) return;

        User admin = userRepository.findByUsername("admin").orElseThrow();
        List<SeedMaterial> samples = List.of(
                new SeedMaterial("M-1001", "A4复印纸", "办公耗材", "80g 500张/包", "箱",
                        "晨光文具", "A区-01货架", 10, "行政办公常用物资", List.of(80D, 30D), List.of(26D, 18D)),
                new SeedMaterial("M-2001", "一次性医用口罩", "防护用品", "50只/盒", "盒",
                        "康盾医疗", "B区-03货架", 30, "低于安全库存需及时补货", List.of(120D), List.of(68D, 24D)),
                new SeedMaterial("M-3001", "无线扫码枪", "电子设备", "2.4G USB接收器", "台",
                        "优码科技", "C区-设备柜", 5, "出库需登记领用部门", List.of(18D), List.of(6D)),
                new SeedMaterial("M-4001", "不锈钢螺丝套装", "维修备件", "M3-M8混合装", "套",
                        "华工五金", "D区-02货架", 20, "设备维修备品", List.of(45D), List.of(32D))
        );

        int index = 0;
        for (SeedMaterial sample : samples) {
            Material material = new Material();
            material.setCode(sample.code());
            material.setName(sample.name());
            material.setCategory(sample.category());
            material.setSpec(sample.spec());
            material.setUnit(sample.unit());
            material.setSupplier(sample.supplier());
            material.setLocation(sample.location());
            material.setSafetyStock(sample.safetyStock());
            material.setRemark(sample.remark());
            material.setStock(0D);
            material = materialRepository.save(material);

            for (int i = 0; i < sample.inbound().size(); i++) {
                double quantity = sample.inbound().get(i);
                material.setStock(material.getStock() + quantity);
                inboundRecordRepository.save(inbound(material, quantity, admin, daysAgo(12 - i * 3 - index)));
            }
            for (int i = 0; i < sample.outbound().size(); i++) {
                double quantity = sample.outbound().get(i);
                material.setStock(material.getStock() - quantity);
                outboundRecordRepository.save(outbound(material, quantity, admin, daysAgo(8 - i * 2 - index)));
            }
            materialRepository.save(material);
            index++;
        }
    }

    private User user(String username, String password, String name, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setName(name);
        user.setRole(role);
        user.setStatus(1);
        return user;
    }

    private InboundRecord inbound(Material material, double quantity, User admin, LocalDateTime createdAt) {
        InboundRecord record = new InboundRecord();
        record.setMaterial(material);
        record.setQuantity(quantity);
        record.setUnitPrice(0D);
        record.setSource("期初入库");
        record.setOperator(admin.getName());
        record.setRemark("示例数据");
        record.setCreatedBy(admin);
        record.setCreatedAt(createdAt);
        return record;
    }

    private OutboundRecord outbound(Material material, double quantity, User admin, LocalDateTime createdAt) {
        OutboundRecord record = new OutboundRecord();
        record.setMaterial(material);
        record.setQuantity(quantity);
        record.setRecipient("综合管理部");
        record.setPurpose("日常领用");
        record.setOperator(admin.getName());
        record.setRemark("示例数据");
        record.setCreatedBy(admin);
        record.setCreatedAt(createdAt);
        record.setStatus("approved");
        record.setApprovedBy(admin);
        record.setApprovedAt(createdAt);
        return record;
    }

    private LocalDateTime daysAgo(int days) {
        return LocalDateTime.now().minusDays(days);
    }

    private record SeedMaterial(
            String code,
            String name,
            String category,
            String spec,
            String unit,
            String supplier,
            String location,
            double safetyStock,
            String remark,
            List<Double> inbound,
            List<Double> outbound
    ) {
    }
}
