CREATE TABLE dish (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
    shop_id      BIGINT         NOT NULL                COMMENT '所属商家ID',
    name         VARCHAR(100)   NOT NULL                COMMENT '菜品名',
    description  VARCHAR(255)   DEFAULT NULL            COMMENT '描述',
    price        INT            NOT NULL                COMMENT '价格（单位：元）',
    image        VARCHAR(255)   DEFAULT NULL            COMMENT '菜品图片URL',
    category     VARCHAR(50)    DEFAULT NULL            COMMENT '店内分类（如：主食/凉菜/招牌）',
    status       TINYINT(1)     NOT NULL DEFAULT 1      COMMENT '1 在售 0 停售',
    create_time  DATETIME       DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
    update_time  DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_shop_id (shop_id),
    KEY idx_shop_status (shop_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品表';