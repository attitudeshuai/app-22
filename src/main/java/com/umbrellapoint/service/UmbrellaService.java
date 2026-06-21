package com.umbrellapoint.service;

import com.umbrellapoint.dto.umbrella.UmbrellaDto;
import com.umbrellapoint.dto.umbrella.UmbrellaRequest;
import com.umbrellapoint.dto.umbrella.UmbrellaStatusRequest;
import com.umbrellapoint.entity.Station;
import com.umbrellapoint.entity.Umbrella;
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

@Service
public class UmbrellaService {

    private static final Logger logger = LoggerFactory.getLogger(UmbrellaService.class);

    private final UmbrellaRepository umbrellaRepository;
    private final StationRepository stationRepository;

    public UmbrellaService(UmbrellaRepository umbrellaRepository, StationRepository stationRepository) {
        this.umbrellaRepository = umbrellaRepository;
        this.stationRepository = stationRepository;
    }

    public Page<UmbrellaDto> getAllUmbrellas(int page, int size, String search, Long stationId,
                                             Umbrella.UmbrellaStatus status, String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Umbrella> umbrellas;

        if (status != null) {
            umbrellas = umbrellaRepository.findByStatus(status, pageable);
        } else if (stationId != null) {
            umbrellas = umbrellaRepository.findByStationId(stationId, pageable);
        } else if (StringUtils.hasText(search)) {
            umbrellas = umbrellaRepository.findByCodeContainingOrColorContaining(search, search, pageable);
        } else {
            umbrellas = umbrellaRepository.findAll(pageable);
        }
        return umbrellas.map(this::convertToDto);
    }

    public UmbrellaDto getUmbrellaById(Long id) {
        Umbrella umbrella = umbrellaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("雨伞", "id", id));
        return convertToDto(umbrella);
    }

    @Transactional
    public UmbrellaDto createUmbrella(UmbrellaRequest request) {
        if (umbrellaRepository.existsByCode(request.getCode())) {
            throw new BusinessException("雨伞编号已存在");
        }
        if (!stationRepository.existsById(request.getStationId())) {
            throw new ResourceNotFoundException("借还点", "id", request.getStationId());
        }

        Umbrella umbrella = new Umbrella();
        umbrella.setCode(request.getCode());
        umbrella.setStationId(request.getStationId());
        umbrella.setColor(request.getColor());
        umbrella.setStatus(Umbrella.UmbrellaStatus.Available);
        umbrella = umbrellaRepository.save(umbrella);
        logger.info("雨伞创建成功: {}", umbrella.getCode());
        return convertToDto(umbrella);
    }

    @Transactional
    public UmbrellaDto updateUmbrella(Long id, UmbrellaRequest request) {
        Umbrella umbrella = umbrellaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("雨伞", "id", id));

        if (request.getCode() != null && !request.getCode().equals(umbrella.getCode())) {
            if (umbrellaRepository.existsByCode(request.getCode())) {
                throw new BusinessException("雨伞编号已存在");
            }
            umbrella.setCode(request.getCode());
        }
        if (request.getStationId() != null) {
            if (!stationRepository.existsById(request.getStationId())) {
                throw new ResourceNotFoundException("借还点", "id", request.getStationId());
            }
            umbrella.setStationId(request.getStationId());
        }
        if (request.getColor() != null) umbrella.setColor(request.getColor());

        umbrella = umbrellaRepository.save(umbrella);
        logger.info("雨伞更新成功: {}", umbrella.getId());
        return convertToDto(umbrella);
    }

    @Transactional
    public UmbrellaDto updateUmbrellaStatus(Long id, UmbrellaStatusRequest request) {
        Umbrella umbrella = umbrellaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("雨伞", "id", id));
        umbrella.setStatus(request.getStatus());
        umbrella = umbrellaRepository.save(umbrella);
        logger.info("雨伞状态更新成功: {} -> {}", umbrella.getId(), request.getStatus());
        return convertToDto(umbrella);
    }

    @Transactional
    public void deleteUmbrella(Long id) {
        if (!umbrellaRepository.existsById(id)) {
            throw new ResourceNotFoundException("雨伞", "id", id);
        }
        umbrellaRepository.deleteById(id);
        logger.info("雨伞删除成功: {}", id);
    }

    private UmbrellaDto convertToDto(Umbrella umbrella) {
        return new UmbrellaDto(
                umbrella.getId(),
                umbrella.getCode(),
                umbrella.getStationId(),
                umbrella.getColor(),
                umbrella.getStatus(),
                umbrella.getCreatedAt()
        );
    }
}
