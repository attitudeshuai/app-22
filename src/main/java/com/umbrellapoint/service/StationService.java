package com.umbrellapoint.service;

import com.umbrellapoint.dto.station.StationDto;
import com.umbrellapoint.dto.station.StationRequest;
import com.umbrellapoint.entity.Station;
import com.umbrellapoint.exception.BusinessException;
import com.umbrellapoint.exception.ResourceNotFoundException;
import com.umbrellapoint.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StationService {

    private static final Logger logger = LoggerFactory.getLogger(StationService.class);

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
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
                station.getIsActive(),
                station.getCreatedAt()
        );
    }
}
