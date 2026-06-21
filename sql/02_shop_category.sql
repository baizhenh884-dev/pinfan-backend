-- ===== 店铺分类表 =====
CREATE TABLE shop_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    name        VARCHAR(50)  NOT NULL                COMMENT '分类名称（湘菜、川菜、奶茶…）',
    sort        INT          NOT NULL DEFAULT 0      COMMENT '排序，越小越靠前',
    icon        VARCHAR(255) DEFAULT NULL            COMMENT '图标URL',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 正常 1 已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺分类表';
