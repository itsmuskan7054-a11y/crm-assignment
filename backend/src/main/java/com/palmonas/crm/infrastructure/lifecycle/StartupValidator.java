package com.palmonas.crm.infrastructure.lifecycle;

import com.palmonas.crm.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class StartupValidator implements ApplicationRunner {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final AppProperties appProperties;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Running startup validations ===");

        validateDatabase();
        validateRedis();
        validateConfig();

        log.info("=== All startup validations passed ===");
    }

    private void validateDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(5)) {
                log.info("[OK] Database connection verified");
            } else {
                throw new IllegalStateException("Database connection invalid");
            }
        } catch (Exception e) {
            log.error("[FAIL] Database connection failed: {}", e.getMessage());
            throw new IllegalStateException("Database connectivity check failed", e);
        }
    }

    private void validateRedis() {
        try {
            var conn = redisConnectionFactory.getConnection();
            conn.ping();
            conn.close();
            log.info("[OK] Redis connection verified");
        } catch (Exception e) {
            log.warn("[WARN] Redis connection failed: {} - app will continue with degraded caching", e.getMessage());
        }
    }

    private void validateConfig() {
        if (appProperties.getJwt().getSecret() == null || appProperties.getJwt().getSecret().length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters");
        }
        log.info("[OK] Configuration validated");
    }
}
