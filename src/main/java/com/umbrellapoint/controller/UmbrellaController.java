package com.umbrellapoint.controller;

import com.umbrellapoint.dto.ApiResponse;
import com.umbrellapoint.dto.PageResponse;
import com.umbrellapoint.dto.umbrella.UmbrellaDto;
import com.umbrellapoint.dto.umbrella.UmbrellaRequest;
import com.umbrellapoint.dto.umbrella.UmbrellaStatusRequest;
import com.umbrellapoint.entity.Umbrella;
import com.umbrellapoint.service.UmbrellaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/umbrellas")
@Tag(name = "雨伞管理", description = "雨伞CRUD和状态管理接口")
public class UmbrellaController {

    private final UmbrellaService umbrellaService;

    public UmbrellaController(UmbrellaService umbrellaService) {
        this.umbrellaService = umbrellaService;
    }

    @GetMapping
    @Operation(summary = "获取雨伞列表")
    public ResponseEntity<ApiResponse<PageResponse<UmbrellaDto>>> getAllUmbrellas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) Umbrella.UmbrellaStatus status,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(umbrellaService.getAllUmbrellas(
                        page, size, search, stationId, status, sortBy, sortDir))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取雨伞详情")
    public ResponseEntity<ApiResponse<UmbrellaDto>> getUmbrellaById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(umbrellaService.getUmbrellaById(id)));
    }

    @PostMapping
    @Operation(summary = "创建雨伞")
    public ResponseEntity<ApiResponse<UmbrellaDto>> createUmbrella(@Valid @RequestBody UmbrellaRequest request) {
        return ResponseEntity.ok(ApiResponse.success("创建成功", umbrellaService.createUmbrella(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新雨伞")
    public ResponseEntity<ApiResponse<UmbrellaDto>> updateUmbrella(@PathVariable Long id,
                                                                     @Valid @RequestBody UmbrellaRequest request) {
        return ResponseEntity.ok(ApiResponse.success("更新成功", umbrellaService.updateUmbrella(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "修改雨伞状态")
    public ResponseEntity<ApiResponse<UmbrellaDto>> updateUmbrellaStatus(
            @PathVariable Long id,
            @Valid @RequestBody UmbrellaStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("状态更新成功",
                umbrellaService.updateUmbrellaStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除雨伞")
    public ResponseEntity<ApiResponse<Void>> deleteUmbrella(@PathVariable Long id) {
        umbrellaService.deleteUmbrella(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
