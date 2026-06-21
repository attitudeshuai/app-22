package com.umbrellapoint.controller;

import com.umbrellapoint.dto.ApiResponse;
import com.umbrellapoint.dto.PageResponse;
import com.umbrellapoint.dto.station.StationDto;
import com.umbrellapoint.dto.station.StationRequest;
import com.umbrellapoint.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stations")
@Tag(name = "借还点管理", description = "雨伞借还点CRUD接口")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    @Operation(summary = "获取借还点列表")
    public ResponseEntity<ApiResponse<PageResponse<StationDto>>> getAllStations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(stationService.getAllStations(page, size, search, sortBy, sortDir))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取借还点详情")
    public ResponseEntity<ApiResponse<StationDto>> getStationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(stationService.getStationById(id)));
    }

    @PostMapping
    @Operation(summary = "创建借还点")
    public ResponseEntity<ApiResponse<StationDto>> createStation(@Valid @RequestBody StationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("创建成功", stationService.createStation(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新借还点")
    public ResponseEntity<ApiResponse<StationDto>> updateStation(@PathVariable Long id,
                                                               @Valid @RequestBody StationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("更新成功", stationService.updateStation(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除借还点")
    public ResponseEntity<ApiResponse<Void>> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
