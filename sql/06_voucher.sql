-- ============================================================
-- 模块 8：秒杀优惠券
-- ============================================================

-- 1. 普通优惠券基础表
CREATE TABLE voucher (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
    shop_id       BIGINT      DEFAULT NULL            COMMENT '所属商家',
    title         VARCHAR(100) NOT NULL               COMMENT '标题',
    sub_title     VARCHAR(255) DEFAULT NULL           COMMENT '副标题',
    rules         TEXT        DEFAULT NULL            COMMENT '使用规则',
    pay_value     INT         NOT NULL                COMMENT '支付金额（分）',
    actual_value  INT         NOT NULL                COMMENT '面值（分）',
    type          TINYINT     NOT NULL DEFAULT 0      COMMENT '0普通 1秒杀',
    status        TINYINT     NOT NULL DEFAULT 1      COMMENT '1上架 0下架',
    create_time   DATETIME    DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
    update_time   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT(1)  NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_shop_id (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

-- 2. 秒杀优惠券扩展表
CREATE TABLE seckill_voucher (
    voucher_id    BIGINT      NOT NULL                COMMENT '优惠券ID（同时是主键）',
    stock         INT         NOT NULL                COMMENT '库存',
    begin_time    DATETIME    NOT NULL                COMMENT '秒杀开始时间',
    end_time      DATETIME    NOT NULL                COMMENT '秒杀结束时间',
    create_time   DATETIME    DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
    update_time   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (voucher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀券扩展表';

-- 3. 优惠券订单
CREATE TABLE voucher_order (
    id            BIGINT      NOT NULL                COMMENT '订单ID（分布式ID，不自增）',
    user_id       BIGINT      NOT NULL                COMMENT '抢券用户ID',
    voucher_id    BIGINT      NOT NULL                COMMENT '优惠券ID',
    pay_type      TINYINT     NOT NULL DEFAULT 1      COMMENT '支付方式：1余额 2微信 3支付宝',
    status        TINYINT     NOT NULL DEFAULT 1      COMMENT '1未支付 2已支付 3已核销 4已退款 5已取消',
    create_time   DATETIME    DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
    pay_time      DATETIME    DEFAULT NULL            COMMENT '支付时间',
    use_time      DATETIME    DEFAULT NULL            COMMENT '核销时间',
    refund_time   DATETIME    DEFAULT NULL            COMMENT '退款时间',
    update_time   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_voucher (user_id, voucher_id),
    KEY idx_voucher_id (voucher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券订单';
