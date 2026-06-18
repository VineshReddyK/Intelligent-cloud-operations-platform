CREATE TABLE IF NOT EXISTS orders (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    product_name  VARCHAR(255) NOT NULL,
    quantity      INTEGER      NOT NULL,
    total_amount  NUMERIC(10, 2) NOT NULL,
    status        VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP
);
