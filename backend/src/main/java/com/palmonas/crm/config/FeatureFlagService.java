package com.palmonas.crm.config;

import com.palmonas.crm.module.order.model.FeatureFlag;
import com.palmonas.crm.module.order.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService {

    private final FeatureFlagRepository repository;

    @Cacheable(value = "featureFlags", key = "#key")
    public boolean isEnabled(String key) {
        return repository.findByFlagKey(key)
                .map(FeatureFlag::isEnabled)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<FeatureFlag> getAll() {
        return repository.findAll();
    }

    @Transactional
    @CacheEvict(value = "featureFlags", key = "#key")
    public FeatureFlag toggle(String key, boolean enabled) {
        FeatureFlag flag = repository.findByFlagKey(key)
                .orElseThrow(() -> new RuntimeException("Feature flag not found: " + key));
        flag.setEnabled(enabled);
        flag = repository.save(flag);
        log.info("Feature flag '{}' set to {}", key, enabled);
        return flag;
    }
}
