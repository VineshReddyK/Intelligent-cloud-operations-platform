CREATE TABLE IF NOT EXISTS anomaly_events (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    service       VARCHAR(255) NOT NULL,
    metric        VARCHAR(255) NOT NULL,
    current_value FLOAT8       NOT NULL,
    z_score       FLOAT8       NOT NULL,
    detected_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_anomaly_service     ON anomaly_events (service);
CREATE INDEX IF NOT EXISTS idx_anomaly_detected_at ON anomaly_events (detected_at);
