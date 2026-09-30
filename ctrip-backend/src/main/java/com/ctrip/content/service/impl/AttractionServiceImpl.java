package com.ctrip.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.exception.ResourceNotFoundException;
import com.ctrip.content.converter.AttractionConverter;
import com.ctrip.content.dto.request.CreateAttractionRequest;
import com.ctrip.content.dto.response.AttractionResponse;
import com.ctrip.content.entity.Attraction;
import com.ctrip.content.mapper.AttractionMapper;
import com.ctrip.content.service.AttractionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 景点服务实现类。
 */
@Service
public class AttractionServiceImpl implements AttractionService {

    private final AttractionMapper attractionMapper;

    public AttractionServiceImpl(AttractionMapper attractionMapper) {
        this.attractionMapper = attractionMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttractionResponse> listAttractions(int page, int limit, Long destinationId, String keyword) {
        Page<Attraction> pageParam = new Page<>(page, limit);

        LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<>();
        if (destinationId != null) {
            wrapper.eq(Attraction::getDestinationId, destinationId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Attraction::getName, keyword);
        }
        wrapper.orderByDesc(Attraction::getCreatedAt);

        Page<Attraction> result = attractionMapper.selectPage(pageParam, wrapper);

        List<AttractionResponse> records = result.getRecords().stream()
                .map(AttractionConverter::toResponse)
                .collect(Collectors.toList());

        Page<AttractionResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(records);
        return responsePage;
    }

    @Override
    @Transactional(readOnly = true)
    public AttractionResponse getDetail(Long id) {
        Attraction attraction = requireAttraction(id);
        return AttractionConverter.toResponse(attraction);
    }

    @Override
    @Transactional
    public AttractionResponse createAttraction(CreateAttractionRequest request) {
        Attraction attraction = AttractionConverter.toEntity(request);
        attractionMapper.insert(attraction);
        return AttractionConverter.toResponse(requireAttraction(attraction.getId()));
    }

    @Override
    @Transactional
    public void updateAttraction(Long id, com.ctrip.content.dto.request.UpdateAttractionRequest request) {
        requireAttraction(id);

        attractionMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Attraction>()
                .eq(Attraction::getId, id)
                .set(request.name() != null, Attraction::getName, request.name())
                .set(request.description() != null, Attraction::getDescription, request.description())
                .set(request.location() != null, Attraction::getLocation, request.location())
                .set(request.ticketPrice() != null, Attraction::getTicketPrice, request.ticketPrice())
                .set(request.coverUrl() != null, Attraction::getCoverUrl, request.coverUrl())
                .set(request.imageUrls() != null, Attraction::getImageUrls, request.imageUrls()));
    }

    @Override
    @Transactional
    public void deleteAttraction(Long id) {
        requireAttraction(id);
        attractionMapper.deleteById(id);
    }

    /**
     * 查询景点，不存在则抛出异常。
     */
    private Attraction requireAttraction(Long id) {
        Attraction attraction = attractionMapper.selectById(id);
        if (attraction == null) {
            throw new ResourceNotFoundException("景点不存在：id=" + id);
        }
        return attraction;
    }
}
