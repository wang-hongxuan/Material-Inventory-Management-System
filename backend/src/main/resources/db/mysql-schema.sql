CREATE DATABASE IF NOT EXISTS inventory_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE inventory_db;

-- Spring JPA will automatically create and update tables on startup.
-- If your MySQL root password is not empty, start the backend with:
--   $env:DB_PASSWORD='your_password'; npm --prefix backend run dev
