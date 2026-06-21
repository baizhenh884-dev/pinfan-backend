CREATE TABLE shop (
    id           BIGINT          NOT NULL AUTO_INCREMENT COMMENT '商家ID',
    name         VARCHAR(100)    NOT NULL                COMMENT '商家名',
    category_id  BIGINT          NOT NULL                COMMENT '所属分类ID',
    icon         VARCHAR(255)    DEFAULT NULL            COMMENT '头图URL',
    address      VARCHAR(255)    DEFAULT NULL            COMMENT '校内地址',
    score        DECIMAL(2,1)    NOT NULL DEFAULT 0.0    COMMENT '平均评分 0.0-5.0',
    price        INT             DEFAULT NULL            COMMENT '人均消费（元）',
    phone        VARCHAR(20)     DEFAULT NULL            COMMENT '商家电话',
    open_hours   VARCHAR(50)     DEFAULT NULL            COMMENT '营业时间',
    status       TINYINT(1)      NOT NULL DEFAULT 1      COMMENT '1 正常 0 歇业',
    x            DECIMAL(10,7)   DEFAULT NULL            COMMENT '经度',
    y            DECIMAL(10,7)   DEFAULT NULL            COMMENT '纬度',
    create_time  DATETIME        DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
    update_time  DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_category_id (category_id),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家表';

DESC shop;