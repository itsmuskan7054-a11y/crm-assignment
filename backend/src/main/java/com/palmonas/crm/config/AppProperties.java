package com.palmonas.crm.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
@Getter
@Setter
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Admin admin = new Admin();
    private final RateLimit rateLimit = new RateLimit();
    private final Channel channel = new Channel();

    @Getter
    @Setter
    public static class Jwt {
        @NotBlank
        private String secret;
        @Positive
        private long accessTokenExpiry = 900000;
        @Positive
        private long refreshTokenExpiry = 604800000;
    }

    @Getter
    @Setter
    public static class Admin {
        @NotBlank
        private String email;
        @NotBlank
        private String password;
    }

    @Getter
    @Setter
    public static class RateLimit {
        @Positive
        private int requestsPerSecond = 100;
    }

    @Getter
    @Setter
    public static class Channel {
        private final Simulator simulator = new Simulator();
        private String syncCron = "0 */5 * * * *";

        @Getter
        @Setter
        public static class Simulator {
            private double failureRate = 0.1;
        }
    }
}
