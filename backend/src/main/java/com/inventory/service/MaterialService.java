package com.inventory.service;

import com.inventory.common.ApiException;
import com.inventory.common.PageResult;
import com.inventory.dto.MaterialRequest;
import com.inventory.entity.Material;
import com.inventory.repository.InboundRecordRepository;
import com.inventory.repository.MaterialRepository;
import com.inventory.repository.OutboundRecordRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 物资档案业务服务。
 *
 * <p>负责物资基础信息维护、唯一编号校验、分类查询、照片上传和删除保护。
 * 库存数量不在物资编辑中直接修改，而是由入库/出库流水自动计算。</p>
 */
@Service
public class MaterialService {
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final MaterialRepository materialRepository;
    private final InboundRecordRepository inboundRecordRepository;
    private final OutboundRecordRepository outboundRecordRepository;
    private final Path uploadPath;

    public MaterialService(
            MaterialRepository materialRepository,
            InboundRecordRepository inboundRecordRepository,
            OutboundRecordRepository outboundRecordRepository,
            @Value("${app.upload-dir}") String uploadDir
    ) {
        this.materialRepository = materialRepository;
        this.inboundRecordRepository = inboundRecordRepository;
        this.outboundRecordRepository = outboundRecordRepository;
        this.uploadPath = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    /** 按关键字、分类和低库存条件分页查询物资档案。 */
    public PageResult<Material> list(String keyword, String category, boolean lowStock, int page, int pageSize) {
        Page<Material> result = materialRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String key = trim(keyword);
            if (StringUtils.hasText(key)) {
                String like = "%" + key + "%";
                predicates.add(cb.or(
                        cb.like(root.get("code"), like),
                        cb.like(root.get("name"), like),
                        cb.like(root.get("spec"), like),
                        cb.like(root.get("supplier"), like),
                        cb.like(root.get("location"), like)
                ));
            }
            if (StringUtils.hasText(category)) predicates.add(cb.equal(root.get("category"), category.trim()));
            if (lowStock) predicates.add(cb.lessThanOrEqualTo(root.get("stock"), root.get("safetyStock")));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(page - 1, pageSize, Sort.by("updatedAt").descending().and(Sort.by("id").descending())));
        return new PageResult<>(result.getContent(), result.getTotalElements(), page, pageSize);
    }

    /** 查询已有分类，供前端筛选和表单下拉使用。 */
    public List<String> categories() {
        return materialRepository.findDistinctCategories();
    }

    /** 查询单个物资，不存在时返回 404 业务异常。 */
    public Material get(Long id) {
        return materialRepository.findById(id).orElseThrow(() -> ApiException.notFound("物资不存在"));
    }

    /** 新增物资档案，并校验物资编号唯一性。 */
    @Transactional
    public Material create(MaterialRequest request) {
        Material material = new Material();
        fill(material, request);
        if (materialRepository.existsByCode(material.getCode())) throw ApiException.conflict("物资编号已存在");
        material.setStock(0D);
        return materialRepository.save(material);
    }

    /** 更新物资档案；如果修改编号，会再次校验编号不能重复。 */
    @Transactional
    public Material update(Long id, MaterialRequest request) {
        Material material = get(id);
        String newCode = trim(request.code());
        if (!material.getCode().equals(newCode)
                && materialRepository.findByCode(newCode).filter(item -> !item.getId().equals(id)).isPresent()) {
            throw ApiException.conflict("物资编号已存在");
        }
        fill(material, request);
        return materialRepository.save(material);
    }

    /**
     * 上传物资照片。
     *
     * <p>只允许常见图片类型，文件保存到配置的上传目录，数据库仅保存访问路径。</p>
     */
    @Transactional
    public String uploadPhoto(Long id, MultipartFile file) {
        Material material = get(id);
        if (file == null || file.isEmpty()) throw ApiException.badRequest("请选择要上传的图片");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(contentType)) throw ApiException.badRequest("只能上传 JPG、PNG、WEBP 或 GIF 图片");

        try {
            Files.createDirectories(uploadPath);
            String extension = extension(file.getOriginalFilename());
            String filename = "material-" + System.currentTimeMillis() + "-" + UUID.randomUUID() + extension;
            Path target = uploadPath.resolve(filename).normalize();
            file.transferTo(target);
            String url = "/uploads/" + filename;
            material.setPhotoUrl(url);
            materialRepository.save(material);
            return url;
        } catch (IOException error) {
            throw ApiException.badRequest("图片保存失败，请检查上传目录权限");
        }
    }

    /** 删除物资；已有入库或出库记录时禁止删除，避免历史流水失去关联。 */
    @Transactional
    public void delete(Long id) {
        if (!materialRepository.existsById(id)) throw ApiException.notFound("物资不存在");
        if (inboundRecordRepository.existsByMaterial_Id(id) || outboundRecordRepository.existsByMaterial_Id(id)) {
            throw ApiException.conflict("该物资已有出入库记录，不能删除");
        }
        materialRepository.deleteById(id);
    }

    /** 将请求对象写入实体，并统一处理必填项、空白字符和安全库存校验。 */
    private void fill(Material material, MaterialRequest request) {
        String code = trim(request.code());
        String name = trim(request.name());
        String category = trim(request.category());
        String unit = trim(request.unit());
        double safetyStock = request.safetyStock() == null ? 0D : request.safetyStock();
        if (!StringUtils.hasText(code) || !StringUtils.hasText(name)
                || !StringUtils.hasText(category) || !StringUtils.hasText(unit)) {
            throw ApiException.badRequest("编号、名称、分类和单位不能为空");
        }
        if (safetyStock < 0) throw ApiException.badRequest("安全库存不能小于 0");
        material.setCode(code);
        material.setName(name);
        material.setCategory(category);
        material.setSpec(trim(request.spec()));
        material.setUnit(unit);
        material.setSupplier(trim(request.supplier()));
        material.setLocation(trim(request.location()));
        material.setSafetyStock(safetyStock);
        material.setRemark(trim(request.remark()));
    }

    private String extension(String originalName) {
        if (!StringUtils.hasText(originalName) || !originalName.contains(".")) return ".jpg";
        String ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        return ext.length() <= 8 ? ext : ".jpg";
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
