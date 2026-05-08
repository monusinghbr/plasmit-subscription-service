CREATE TABLE IF NOT EXISTS subscription_plans (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    billing_cycle VARCHAR(30) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    user_limit INT NOT NULL,
    branch_limit INT NOT NULL,
    storage_limit_gb INT NOT NULL,
    included_features JSON NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_subscription_plans_status (status),
    KEY idx_subscription_plans_deleted (is_deleted),
    KEY idx_subscription_plans_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hospital_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    hospital_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    renewal_date DATE DEFAULT NULL,
    invoice_number VARCHAR(100) DEFAULT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_hospital_subscriptions_hospital (hospital_id),
    KEY idx_hospital_subscriptions_plan (plan_id),
    KEY idx_hospital_subscriptions_status (status),
    KEY idx_hospital_subscriptions_deleted (is_deleted),
    KEY idx_hospital_subscriptions_created_at (created_at),
    CONSTRAINT fk_hospital_subscriptions_plan
        FOREIGN KEY (plan_id) REFERENCES subscription_plans (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_column_if_missing;

DELIMITER //

CREATE PROCEDURE add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `',
            REPLACE(p_table_name, '`', '``'),
            '` ADD COLUMN `',
            REPLACE(p_column_name, '`', '``'),
            '` ',
            p_column_definition
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL add_column_if_missing('hospital_subscriptions', 'created_by', 'BIGINT NULL AFTER `invoice_number`');
CALL add_column_if_missing('hospital_subscriptions', 'updated_by', 'BIGINT NULL AFTER `created_by`');

DROP PROCEDURE IF EXISTS add_column_if_missing;
