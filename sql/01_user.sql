-- ===== 用户表 =====
CREATE TABLE user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    phone       VARCHAR(11)  NOT NULL                COMMENT '手机号（登录账号）',
    nickname    VARCHAR(50)  DEFAULT '校园用户'        COMMENT '昵称',
    avatar      VARCHAR(255) DEFAULT NULL            COMMENT '头像URL',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 正常 1 已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ===== 用户偏好表 =====
CREATE TABLE user_preference (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id             BIGINT       NOT NULL                COMMENT '用户ID',
    diet_restrictions   VARCHAR(500) DEFAULT NULL            COMMENT '饮食限制 JSON 数组',
    taste_preferences   VARCHAR(500) DEFAULT NULL            COMMENT '口味偏好 JSON 数组',
    budget_min          INT          DEFAULT NULL            COMMENT '单餐预算下限',
    budget_max          INT          DEFAULT NULL            COMMENT '单餐预算上限',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好表';