CREATE TABLE IF NOT EXISTS payments (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID         UNIQUE NOT NULL,
    user_id        UUID         NOT NULL,
    amount         NUMERIC(10, 2) NOT NULL,
    status         VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(255),
    created_at     TIMESTAMP    NOT NULL,
    processed_at   TIMESTAMP
);
