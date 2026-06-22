package com.umbrellapoint.service;

import com.umbrellapoint.entity.CreditConfig;
import com.umbrellapoint.repository.CreditConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CreditConfigService {

    private static final Logger logger = LoggerFactory.getLogger(CreditConfigService.class);

    private final CreditConfigRepository creditConfigRepository;

    private static final Map<String, String> DEFAULT_CONFIGS = new HashMap<>();

    static {
        DEFAULT_CONFIGS.put(CreditConfig.GRACE_PERIOD_HOURS, "24");
        DEFAULT_CONFIGS.put(CreditConfig.OVERDUE_PENALTY_PER_DAY, "5");
        DEFAULT_CONFIGS.put(CreditConfig.MAX_OVERDUE_PENALTY, "50");
        DEFAULT_CONFIGS.put(CreditConfig.OVERDUE_NOTIFY_ENABLED, "true");
    }

    public CreditConfigService(CreditConfigRepository creditConfigRepository) {
        this.creditConfigRepository = creditConfigRepository;
    }

    @PostConstruct
    @Transactional
    public void initDefaultConfigs() {
        for (Map.Entry<String, String> entry : DEFAULT_CONFIGS.entrySet()) {
            String key = entry.getKey();
            if (!creditConfigRepository.existsByConfigKey(key)) {
                CreditConfig config = new CreditConfig();
                config.setConfigKey(key);
                config.setConfigValue(entry.getValue());
                config.setConfigName(getConfigName(key));
                config.setDescription(getConfigDescription(key));
                config.setCategory("overdue");
                creditConfigRepository.save(config);
                logger.info("初始化信用配置: {} = {}", key, entry.getValue());
            }
        }
    }

    public int getGracePeriodHours() {
        return getIntConfig(CreditConfig.GRACE_PERIOD_HOURS, 24);
    }

    public int getOverduePenaltyPerDay() {
        return getIntConfig(CreditConfig.OVERDUE_PENALTY_PER_DAY, 5);
    }

    public int getMaxOverduePenalty() {
        return getIntConfig(CreditConfig.MAX_OVERDUE_PENALTY, 50);
    }

    public boolean isOverdueNotifyEnabled() {
        return getBooleanConfig(CreditConfig.OVERDUE_NOTIFY_ENABLED, true);
    }

    public int getIntConfig(String key, int defaultValue) {
        return creditConfigRepository.findByConfigKey(key)
                .map(config -> {
                    try {
                        return Integer.parseInt(config.getConfigValue());
                    } catch (NumberFormatException e) {
                        logger.warn("配置值格式错误: {} = {}", key, config.getConfigValue());
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    public boolean getBooleanConfig(String key, boolean defaultValue) {
        return creditConfigRepository.findByConfigKey(key)
                .map(config -> Boolean.parseBoolean(config.getConfigValue()))
                .orElse(defaultValue);
    }

    public String getStringConfig(String key, String defaultValue) {
        return creditConfigRepository.findByConfigKey(key)
                .map(CreditConfig::getConfigValue)
                .orElse(defaultValue);
    }

    public List<CreditConfig> getAllConfigs() {
        return creditConfigRepository.findAll();
    }

    public List<CreditConfig> getConfigsByCategory(String category) {
        return creditConfigRepository.findByCategory(category);
    }

    public CreditConfig getConfigByKey(String key) {
        return creditConfigRepository.findByConfigKey(key).orElse(null);
    }

    @Transactional
    public CreditConfig updateConfig(String key, String value) {
        CreditConfig config = creditConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new IllegalArgumentException("配置项不存在: " + key));
        String oldValue = config.getConfigValue();
        config.setConfigValue(value);
        config = creditConfigRepository.save(config);
        logger.info("信用配置已更新: {} = {} -> {}", key, oldValue, value);
        return config;
    }

    private String getConfigName(String key) {
        return switch (key) {
            case CreditConfig.GRACE_PERIOD_HOURS -> "逾期宽限期（小时）";
            case CreditConfig.OVERDUE_PENALTY_PER_DAY -> "每日逾期扣分";
            case CreditConfig.MAX_OVERDUE_PENALTY -> "单次最大扣分数";
            case CreditConfig.OVERDUE_NOTIFY_ENABLED -> "逾期通知开关";
            default -> key;
        };
    }

    private String getConfigDescription(String key) {
        return switch (key) {
            case CreditConfig.GRACE_PERIOD_HOURS -> "借伞后多少小时内不算逾期，宽限期内不计入逾期天数";
            case CreditConfig.OVERDUE_PENALTY_PER_DAY -> "每逾期一天扣减的信用分数";
            case CreditConfig.MAX_OVERDUE_PENALTY -> "单条逾期记录累计最多扣减的信用分数";
            case CreditConfig.OVERDUE_NOTIFY_ENABLED -> "是否开启逾期通知功能";
            default -> "";
        };
    }
}
