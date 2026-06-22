package com.umbrellapoint.service;

import com.umbrellapoint.config.CrossRegionFeeConfig;
import com.umbrellapoint.dto.station.NearbyStationDto;
import com.umbrellapoint.dto.station.StationDto;
import com.umbrellapoint.dto.station.StationRequest;
import com.umbrellapoint.entity.Station;
import com.umbrellapoint.exception.BusinessException;
import com.umbrellapoint.exception.ResourceNotFoundException;
import com.umbrellapoint.repository.StationRepository;
import com.umbrellapoint.repository.UmbrellaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StationService {

    private static final Logger logger = LoggerFactory.getLogger(StationService.class);

    private final StationRepository stationRepository;
    private final UmbrellaRepository umbrellaRepository;
    private final CrossRegionFeeConfig feeConfig;

    public StationService(StationRepository stationRepository,
                          UmbrellaRepository umbrellaRepository,
                          CrossRegionFeeConfig feeConfig) {
        this.stationRepository = stationRepository;
        this.umbrellaRepository = umbrellaRepository;
        this.feeConfig = feeConfig;
    }

    public Page<StationDto> getAllStations(int page, int size, String search, String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Station> stations;
        if (StringUtils.hasText(search)) {
            stations = stationRepository.findByNameContainingOrAddressContaining(search, search, pageable);
        } else {
            stations = stationRepository.findAll(pageable);
        }
        return stations.map(this::convertToDto);
    }

    public StationDto getStationById(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("借还点", "id", id));
        return convertToDto(station);
    }

    @Transactional
    public StationDto createStation(StationRequest request) {
        Station station = new Station();
        station.setName(request.getName());
        station.setAddress(request.getAddress());
        station.setManagerId(request.getManagerId());
        station.setLatitude(request.getLatitude());
        station.setLongitude(request.getLongitude());
        station.setCapacity(request.getCapacity());
        station.setQrCode(request.getQrCode());
        station.setSafetyThreshold(request.getSafetyThreshold() != null ? request.getSafetyThreshold() : 5);
        station.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        station = stationRepository.save(station);
        logger.info("借还点创建成功: {}", station.getName());
        return convertToDto(station);
    }

    @Transactional
    public StationDto updateStation(Long id, StationRequest request) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("借还点", "id", id));

        if (request.getName() != null) station.setName(request.getName());
        if (request.getAddress() != null) station.setAddress(request.getAddress());
        if (request.getManagerId() != null) station.setManagerId(request.getManagerId());
        if (request.getLatitude() != null) station.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) station.setLongitude(request.getLongitude());
        if (request.getCapacity() != null) station.setCapacity(request.getCapacity());
        if (request.getQrCode() != null) station.setQrCode(request.getQrCode());
        if (request.getSafetyThreshold() != null) station.setSafetyThreshold(request.getSafetyThreshold());
        if (request.getIsActive() != null) station.setIsActive(request.getIsActive());

        station = stationRepository.save(station);
        logger.info("借还点更新成功: {}", station.getId());
        return convertToDto(station);
    }

    @Transactional
    public void deleteStation(Long id) {
        if (!stationRepository.existsById(id)) {
            throw new ResourceNotFoundException("借还点", "id", id);
        }
        stationRepository.deleteById(id);
        logger.info("借还点删除成功: {}", id);
    }

    private StationDto convertToDto(Station station) {
        return new StationDto(
                station.getId(),
                station.getName(),
                station.getAddress(),
                station.getManagerId(),
                station.getLatitude(),
                station.getLongitude(),
                station.getCapacity(),
                station.getQrCode(),
                station.getSafetyThreshold(),
                station.getIsActive(),
                station.getCreatedAt()
        );
    }

    public List<NearbyStationDto> findNearbyReturnStations(Station currentStation) {
        if (currentStation == null || currentStation.getLatitude() == null || currentStation.getLongitude() == null) {
            return new ArrayList<>();
        }

        BigDecimal radiusKm = feeConfig.getNearbyStationRadiusKm();
        Integer limit = feeConfig.getNearbyStationLimit();

        List<Station> allActiveStations = stationRepository.findByIsActive(true, PageRequest.of(0, 1000)).getContent();

        List<NearbyStationDto> nearbyStations = allActiveStations.stream()
                .filter(s -> !s.getId().equals(currentStation.getId()))
                .map(s -> {
                    BigDecimal distance = calculateDistance(currentStation, s);
                    long currentCount = umbrellaRepository.countByStationId(s.getId());
                    int availableSlots = s.getCapacity() - (int) currentCount;
                    NearbyStationDto dto = new NearbyStationDto();
                    dto.setId(s.getId());
                    dto.setName(s.getName());
                    dto.setAddress(s.getAddress());
                    dto.setLatitude(s.getLatitude());
                    dto.setLongitude(s.getLongitude());
                    dto.setDistanceKm(distance);
                    dto.setAvailableSlots(Math.max(0, availableSlots));
                    dto.setCapacity(s.getCapacity());
                    return dto;
                })
                .filter(dto -> dto.getDistanceKm().compareTo(radiusKm) <= 0 && dto.getAvailableSlots() > 0)
                .sorted(Comparator.comparing(NearbyStationDto::getDistanceKm))
                .limit(limit)
                .collect(Collectors.toList());

        logger.info("查找附近可归还站点: 源站点={}, 半径={}km, 找到={}个",
                currentStation.getId(), radiusKm, nearbyStations.size());
        return nearbyStations;
    }

    private BigDecimal calculateDistance(Station from, Station to) {
        if (from.getLatitude() == null || from.getLongitude() == null
                || to.getLatitude() == null || to.getLongitude() == null) {
            return BigDecimal.valueOf(99999);
        }
        final double R = 6371.0;
        double lat1 = from.getLatitude().doubleValue();
        double lon1 = from.getLongitude().doubleValue();
        double lat2 = to.getLatitude().doubleValue();
        double lon2 = to.getLongitude().doubleValue();
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(R * c).setScale(2, RoundingMode.HALF_UP);
    }
}
