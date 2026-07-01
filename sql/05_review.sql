-- ============================================================
-- 模块 7：评论 + AI 摘要
-- ============================================================

CREATE TABLE review (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    shop_id      BIGINT      NOT NULL                COMMENT '商家ID',
    user_id      BIGINT      NOT NULL                COMMENT '评论用户ID',
    rating       TINYINT     NOT NULL                COMMENT '评分 1-5',
    content      TEXT        DEFAULT NULL            COMMENT '评论文字',
    create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
    update_time  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT(1)  NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_shop_id (shop_id),
    KEY idx_user_shop (user_id, shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

CREATE TABLE review_summary (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '摘要ID',
    shop_id       BIGINT      NOT NULL                COMMENT '商家ID',
    pros          TEXT        DEFAULT NULL            COMMENT 'AI 总结-优点',
    cons          TEXT        DEFAULT NULL            COMMENT 'AI 总结-槽点',
    suitable_for  TEXT        DEFAULT NULL            COMMENT 'AI 总结-适合人群',
    review_count  INT         NOT NULL DEFAULT 0      COMMENT '基于多少条评论生成',
    generated_at  DATETIME    DEFAULT NULL            COMMENT '生成时刻',
    model_name    VARCHAR(50) DEFAULT NULL            COMMENT '使用的LLM模型',
    create_time   DATETIME    DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
    update_time   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT(1)  NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_shop_id (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 评论摘要表';
