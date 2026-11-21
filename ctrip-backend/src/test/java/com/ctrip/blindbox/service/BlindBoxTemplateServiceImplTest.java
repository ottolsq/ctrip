package com.ctrip.blindbox.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.BlindBoxType;
import com.ctrip.blindbox.entity.enums.TemplateStatus;
import com.ctrip.blindbox.mapper.BlindBoxOrderMapper;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.blindbox.service.impl.BlindBoxTemplateServiceImpl;
import com.ctrip.common.exception.BusinessException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("盲盒模板服务单元测试")
class BlindBoxTemplateServiceImplTest {

    @Mock
    private BlindBoxTemplateMapper templateMapper;
    @Mock
    private BlindBoxOrderMapper orderMapper;

    private BlindBoxTemplateServiceImpl templateService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BlindBoxTemplate.class);
        TableInfoHelper.initTableInfo(assistant, BlindBoxOrder.class);
    }

    @BeforeEach
    void setUp() {
        templateService = new BlindBoxTemplateServiceImpl(templateMapper, orderMapper);
    }

    private String validRule() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return "{\"activityStartTime\":\"" + LocalDateTime.now().minusDays(1).format(fmt)
                + "\",\"activityEndTime\":\"" + LocalDateTime.now().plusDays(1).format(fmt) + "\"}";
    }

    private BlindBoxTemplate template(BlindBoxType type, Integer stock, String ruleConfig) {
        return BlindBoxTemplate.builder()
                .name("测试盲盒")
                .type(type)
                .price(new BigDecimal("99.00"))
                .stock(stock)
                .ruleConfig(ruleConfig)
                .status(TemplateStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("创建日常盲盒：库存强制 -1")
    void createDaily_shouldForceUnlimitedStock() {
        BlindBoxTemplate request = template(BlindBoxType.DAILY, 99, null);

        templateService.createTemplate(request);

        ArgumentCaptor<BlindBoxTemplate> captor = ArgumentCaptor.forClass(BlindBoxTemplate.class);
        verify(templateMapper).insert(captor.capture());
        assertThat(captor.getValue().getStock()).isEqualTo(-1);
    }

    @Test
    @DisplayName("创建限定盲盒：缺少活动时间拒绝")
    void createLimitedWithoutWindow_shouldThrow() {
        BlindBoxTemplate request = template(BlindBoxType.LIMITED, 5, null);

        assertThatThrownBy(() -> templateService.createTemplate(request))
                .isInstanceOf(BusinessException.class);
        verify(templateMapper, never()).insert(any(BlindBoxTemplate.class));
    }

    @Test
    @DisplayName("创建限定盲盒：合法窗口成功")
    void createLimitedValid_shouldSucceed() {
        BlindBoxTemplate request = template(BlindBoxType.LIMITED, 5, validRule());

        templateService.createTemplate(request);

        verify(templateMapper).insert(request);
    }

    @Test
    @DisplayName("售罄模板仍可改名/下架")
    void updateSoldOutTemplateName_shouldSucceed() {
        BlindBoxTemplate existing = template(BlindBoxType.LIMITED, 0, validRule());
        existing.setId(1L);
        when(templateMapper.selectById(1L)).thenReturn(existing);

        BlindBoxTemplate updates = BlindBoxTemplate.builder()
                .name("新名称")
                .status(TemplateStatus.INACTIVE)
                .build();
        templateService.updateTemplate(1L, updates);

        ArgumentCaptor<BlindBoxTemplate> captor = ArgumentCaptor.forClass(BlindBoxTemplate.class);
        verify(templateMapper).updateById(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("新名称");
        assertThat(captor.getValue().getStock()).isZero();
    }

    @Test
    @DisplayName("删除有订单的模板：拒绝")
    void deleteTemplateWithOrders_shouldThrow() {
        when(templateMapper.selectById(1L)).thenReturn(template(BlindBoxType.DAILY, -1, null));
        when(orderMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> templateService.deleteTemplate(1L))
                .isInstanceOf(BusinessException.class);
        verify(templateMapper, never()).deleteById(1L);
    }

    @Test
    @DisplayName("删除无订单的模板：成功")
    void deleteTemplateWithoutOrders_shouldDelete() {
        when(templateMapper.selectById(1L)).thenReturn(template(BlindBoxType.DAILY, -1, null));
        when(orderMapper.selectCount(any())).thenReturn(0L);

        templateService.deleteTemplate(1L);

        verify(templateMapper).deleteById(1L);
    }
}
