CREATE DATABASE IF NOT EXISTS inventory_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE inventory_db;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(60) NOT NULL UNIQUE,
  password_hash VARCHAR(120) NOT NULL,
  name VARCHAR(60) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'operator',
  status INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS materials (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(80) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(80) NOT NULL,
  spec VARCHAR(160),
  unit VARCHAR(20) NOT NULL,
  supplier VARCHAR(120),
  location VARCHAR(120),
  safety_stock DOUBLE NOT NULL DEFAULT 0,
  stock DOUBLE NOT NULL DEFAULT 0,
  photo_url VARCHAR(255),
  remark VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS inbound_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  material_id BIGINT NOT NULL,
  quantity DOUBLE NOT NULL,
  unit_price DOUBLE NOT NULL DEFAULT 0,
  source VARCHAR(120),
  operator VARCHAR(60) NOT NULL,
  remark VARCHAR(500),
  created_by BIGINT,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_inbound_material FOREIGN KEY (material_id) REFERENCES materials(id),
  CONSTRAINT fk_inbound_user FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS outbound_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  material_id BIGINT NOT NULL,
  quantity DOUBLE NOT NULL,
  recipient VARCHAR(120) NOT NULL,
  purpose VARCHAR(160),
  operator VARCHAR(60) NOT NULL,
  remark VARCHAR(500),
  status VARCHAR(20) DEFAULT 'pending',
  approval_remark VARCHAR(500),
  created_by BIGINT,
  approved_by BIGINT,
  created_at DATETIME NOT NULL,
  approved_at DATETIME,
  CONSTRAINT fk_outbound_material FOREIGN KEY (material_id) REFERENCES materials(id),
  CONSTRAINT fk_outbound_user FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT fk_outbound_approver FOREIGN KEY (approved_by) REFERENCES users(id)
);

CREATE INDEX idx_materials_category ON materials(category);
CREATE INDEX idx_materials_stock ON materials(stock);
CREATE INDEX idx_inbound_material_time ON inbound_records(material_id, created_at);
CREATE INDEX idx_outbound_material_time ON outbound_records(material_id, created_at);
CREATE INDEX idx_outbound_status_time ON outbound_records(status, created_at);

-- 说明：
-- 1. 项目运行时 Spring Data JPA 会根据实体自动创建或更新表结构。
-- 2. 默认管理员、操作员和演示物资数据由 DataSeeder.java 在首次启动时自动写入。
-- 3. 实际运行前请确保 MySQL 已启动，并设置 DB_PASSWORD 环境变量。
