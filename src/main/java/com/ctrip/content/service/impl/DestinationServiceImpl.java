package com.ctrip.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.content.converter.AttractionConverter;
import com.ctrip.content.converter.DestinationConverter;
import com.ctrip.content.dto.request.CreateDestinationRequest;
import com.ctrip.content.dto.request.UpdateDestinationRequest;
import com.ctrip.content.dto.response.AttractionResponse;
import com.ctrip.content.dto.response.DestinationResponse;
import com.ctrip.content.entity.Attraction;
import com.ctrip.content.entity.Destination;
import com.ctrip.content.mapper.AttractionMapper;
import com.ctrip.content.mapper.DestinationMapper;
import com.ctrip.content.service.DestinationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 目的地服务实现类。
 */
@Service
public class DestinationServiceImpl implements DestinationService {

    private final DestinationMapper destinationMapper;
    private final AttractionMapper attractionMapper;

    public DestinationServiceImpl(DestinationMapper destinationMapper, AttractionMapper attractionMapper) {
        this.destinationMapper = destinationMapper;
        this.attractionMapper = attractionMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DestinationResponse> listDestinations(int page, int limit, String country, String province, String keyword) {
        Page<Destination> pageParam = new Page<>(page, limit);

        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<>();
        if (country != null && !country.isBlank()) {
            wrapper.eq(Destination::getCountry, country);
        }
        if (province != null && !province.isBlank()) {
            wrapper.eq(Destination::getProvince, province);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Destination::getName, keyword);
        }
        wrapper.orderByDesc(Destination::getCreatedAt);

        Page<Destination> result = destinationMapper.selectPage(pageParam, wrapper);

        // 转换为 DTO
        List<DestinationResponse> records = result.getRecords().stream()
                .map(DestinationConverter::toResponse)
                .collect(Collectors.toList());

        // 构建分页结果
        Page<DestinationResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(records);
        return responsePage;
    }

    @Override
    @Transactional(readOnly = true)
    public DestinationResponse getDetail(Long id) {
        Destination destination = requireDestination(id);

        // 查询关联景点
        LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attraction::getDestinationId, id);
        List<AttractionResponse> attractions = attractionMapper.selectList(wrapper).stream()
                .map(AttractionConverter::toResponse)
                .collect(Collectors.toList());

        return DestinationConverter.toResponse(destination, attractions);
    }

    @Override
    @Transactional
    public DestinationResponse createDestination(CreateDestinationRequest request) {
        Destination destination = DestinationConverter.toEntity(request);
        destinationMapper.insert(destination);
        return getDetail(destination.getId());
    }

    @Override
    @Transactional
    public DestinationResponse updateDestination(Long id, UpdateDestinationRequest request) {
        requireDestination(id);

        destinationMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Destination>()
                .eq(Destination::getId, id)
                .set(request.name() != null, Destination::getName, request.name())
                .set(request.country() != null, Destination::getCountry, request.country())
                .set(request.province() != null, Destination::getProvince, request.province())
                .set(request.description() != null, Destination::getDescription, request.description())
                .set(request.bestSeason() != null, Destination::getBestSeason, request.bestSeason())
                .set(request.coverUrl() != null, Destination::getCoverUrl, request.coverUrl())
                .set(request.imageUrls() != null, Destination::getImageUrls, request.imageUrls()));

        return getDetail(id);
    }

    @Override
    @Transactional
    public void deleteDestination(Long id) {
        requireDestination(id);
        destinationMapper.deleteById(id);
    }

    /**
     * 查询目的地，不存在则抛出异常。
     */
    private Destination requireDestination(Long id) {
        Destination destination = destinationMapper.selectById(id);
        if (destination == null) {
            throw new ResourceNotFoundException("目的地不存在：id=" + id);
        }
        return destination;
    }
}
