package com.umbrellapoint.controller;

import com.umbrellapoint.dto.ApiResponse;
import com.umbrellapoint.dto.credit.CreditConfigDto;
import com.umbrellapoint.entity.CreditConfig;
import com.umbrellapoint.service.CreditConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/credit-configs")
@Tag(name = "信用配置管理", description = "信用配置管理接口，支持动态调整宽限期、扣分规则等")
public class CreditConfigController {

    private final CreditConfigService creditConfigService;

    public CreditConfigController(CreditConfigService creditConfigService) {
        this.creditConfigService = creditConfigService;
    }

    @GetMapping
    @Operation(summary = "获取所有信用配置")
    public ResponseEntity<ApiResponse<List<CreditConfigDto>>> getAllConfigs(
            @RequestParam(required = false) String category) {
        List<CreditConfig> configs;
        if (category != null && !category.isEmpty()) {
            configs = creditConfigService.getConfigsByCategory(category);
        } else {
            configs = creditConfigService.getAllConfigs();
        }
        return ResponseEntity.ok(ApiResponse.success(
                configs.stream().map(this::convertToDto).collect(Collectors.toList())));
    }

    @GetMapping("/{key}")
    @Operation(summary = "根据key获取配置")
    public ResponseEntity<ApiResponse<CreditConfigDto>> getConfigByKey(@PathVariable String key) {
        CreditConfig config = creditConfigService.getConfigByKey(key);
        if (config == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "配置不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(convertToDto(config)));
    }

    @PutMapping("/{key}")
    @Operation(summary = "更新信用配置")
    public ResponseEntity<ApiResponse<CreditConfigDto>> updateConfig(
            @PathVariable String key,
            @RequestParam String value) {
        CreditConfig config = creditConfigService.updateConfig(key, value);
        return ResponseEntity.ok(ApiResponse.success("配置更新成功", convertToDto(config)));
    }

    private CreditConfigDto convertToDto(CreditConfig config) {
        return new CreditConfigDto(
                config.getId(),
                config.getConfigKey(),
                config.getConfigValue(),
                config.getConfigName(),
                config.getDescription(),
                config.getCategory(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
