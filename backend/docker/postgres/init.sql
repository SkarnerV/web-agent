-- 容器启动时执行：创建 pgvector 扩展。
-- 业务表 schema 由 Flyway 在应用层管理（任务 2.x）。

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
