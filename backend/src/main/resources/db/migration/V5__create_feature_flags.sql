CREATE TABLE feature_flags (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    flag_key VARCHAR(100) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

INSERT INTO feature_flags (flag_key, enabled, description) VALUES
    ('channel.amazon.enabled', true, 'Enable Amazon channel integration'),
    ('channel.flipkart.enabled', true, 'Enable Flipkart channel integration'),
    ('channel.website.enabled', true, 'Enable organic website channel integration'),
    ('sync.auto.enabled', true, 'Enable automatic channel sync via cron'),
    ('notifications.enabled', false, 'Enable notification system'),
    ('cache.orders.enabled', true, 'Enable Redis caching for orders');
