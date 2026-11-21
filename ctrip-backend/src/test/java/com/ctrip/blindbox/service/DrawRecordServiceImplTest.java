package com.ctrip.blindbox.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.dto.DrawRecordResponse;
import com.ctrip.blindbox.entity.BlindBoxDrawRecord;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.mapper.BlindBoxDrawRecordMapper;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.blindbox.prize.DrawnPrize;
import com.ctrip.blindbox.prize.PrizeTier;
import com.ctrip.blindbox.service.impl.DrawRecordServiceImpl;
import com.ctrip.user.entity.User;
import com.ctrip.user.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("抽取记录服务单元测试")
class DrawRecordServiceImplTest {

    @Mock
    private BlindBoxDrawRecordMapper recordMapper;
    @Mock
    private BlindBoxOrderMapper orderMapper;
    @Mock
    private BlindBoxTemplateMapper templateMapper;
    @Mock
    private UserMapper userMapper;

    private DrawRecordServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BlindBoxDrawRecord.class);
        TableInfoHelper.initTableInfo(assistant, BlindBoxOrder.class);
        TableInfoHelper.initTableInfo(assistant, BlindBoxTemplate.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        service = new DrawRecordServiceImpl(
                recordMapper, orderMapper, templateMapper, userMapper);
    }

    private BlindBoxOrder order() {
        return BlindBoxOrder.builder()
                .id(10L).userId(1L).templateId(2L)
                .orderNo("BB202611210001")
                .build();
    }

    private BlindBoxDrawRecord record(Long id) {
        return BlindBoxDrawRecord.builder()
                .id(id).orderId(10L).userId(1L).templateId(2L)
                .tier(PrizeTier.RARE).destinationId(3L).destinationName("杭州")
                .guaranteed(true).probabilityVersion("v1")
                .createdAt(LocalDateTime.of(2026, 11, 21, 10, 0))
                .build();
    }

    @Test
    @DisplayName("落库：保存奖品与订单快照")
    void record_shouldInsertSnapshot() {
        DrawnPrize prize = new DrawnPrize("RARE", 3L, "杭州", true, "v1");

        service.record(order(), prize);

        ArgumentCaptor<BlindBoxDrawRecord> captor =
                ArgumentCaptor.forClass(BlindBoxDrawRecord.class);
        verify(recordMapper).insert(captor.capture());
        BlindBoxDrawRecord saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(10L);
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getTemplateId()).isEqualTo(2L);
        assertThat(saved.getTier()).isEqualTo(PrizeTier.RARE);
        assertThat(saved.getDestinationId()).isEqualTo(3L);
        assertThat(saved.getDestinationName()).isEqualTo("杭州");
        assertThat(saved.getGuaranteed()).isTrue();
        assertThat(saved.getProbabilityVersion()).isEqualTo("v1");
    }

    @Test
    @DisplayName("落库：同一订单重复写入忽略唯一键冲突")
    void record_duplicateOrder_shouldBeIgnored() {
        doThrow(new DuplicateKeyException("dup"))
                .when(recordMapper).insert(any(BlindBoxDrawRecord.class));

        assertThatCode(() -> service.record(
                order(), new DrawnPrize("RARE", 3L, "杭州", false, "v1")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("我的记录：按用户筛选并填充订单/模板名")
    void listMine_shouldFilterByUserAndEnrichNames() {
        when(recordMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<BlindBoxDrawRecord> page = invocation.getArgument(0);
            page.setRecords(List.of(record(1L)));
            page.setTotal(1);
            return page;
        });
        when(orderMapper.selectBatchIds(any())).thenReturn(List.of(order()));
        when(templateMapper.selectBatchIds(any())).thenReturn(List.of(
                BlindBoxTemplate.builder().id(2L).name("周末盲盒").build()));

        Page<DrawRecordResponse> result = service.listMine(1L, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        DrawRecordResponse item = result.getRecords().getFirst();
        assertThat(item.orderNo()).isEqualTo("BB202611210001");
        assertThat(item.templateName()).isEqualTo("周末盲盒");
        assertThat(item.tier()).isEqualTo("RARE");
        assertThat(item.destination()).isEqualTo("杭州");

        ArgumentCaptor<LambdaQueryWrapper<BlindBoxDrawRecord>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(recordMapper).selectPage(any(Page.class), captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("user_id");
    }

    @Test
    @DisplayName("管理员列表：应用用户/模板/档位/日期筛选并填充用户名")
    void listAll_shouldApplyFilters() {
        when(recordMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<BlindBoxDrawRecord> page = invocation.getArgument(0);
            page.setRecords(List.of(record(1L)));
            page.setTotal(1);
            return page;
        });
        when(orderMapper.selectBatchIds(any())).thenReturn(List.of(order()));
        when(templateMapper.selectBatchIds(any())).thenReturn(List.of(
                BlindBoxTemplate.builder().id(2L).name("周末盲盒").build()));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                User.builder().id(1L).username("alice").build()));

        Page<DrawRecordResponse> result = service.listAll(
                1, 500, 1L, 2L, PrizeTier.RARE,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(result.getSize()).isEqualTo(100);
        assertThat(result.getRecords().getFirst().userName()).isEqualTo("alice");

        ArgumentCaptor<LambdaQueryWrapper<BlindBoxDrawRecord>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(recordMapper).selectPage(any(Page.class), captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("user_id", "template_id", "tier", "created_at");
    }
}
