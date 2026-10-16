CREATE TABLE IF NOT EXISTS ai_call_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'AI调用日志ID',
  feature VARCHAR(64) NOT NULL COMMENT '功能类型: guide/copywriting/sales_analysis',
  request_text LONGTEXT NULL COMMENT '请求摘要',
  response_text LONGTEXT NULL COMMENT '响应摘要',
  provider VARCHAR(64) NULL COMMENT '模型供应商',
  success TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否成功',
  error_message TEXT NULL COMMENT '异常信息',
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  INDEX idx_feature_created_at (feature, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用日志表';

CREATE TABLE IF NOT EXISTS ai_product_vector_cache (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '缓存ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  content_hash VARCHAR(64) NOT NULL COMMENT '商品向量文本哈希',
  vector_index_name VARCHAR(128) NOT NULL DEFAULT 'faiss_products' COMMENT '向量库名称',
  updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_product_index (product_id, vector_index_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品向量缓存表';

-- 原有业务表无需改动：AI服务通过 product、category、review、order、stock_in、stock_out 等表读取业务数据，避免侵入原交易流程。
