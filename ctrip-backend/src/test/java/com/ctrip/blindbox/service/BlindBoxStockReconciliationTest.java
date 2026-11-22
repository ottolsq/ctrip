package com.ctrip.blindbox.service;

import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.mapper.BlindBoxTemplateMapper;
import com.ctrip.blindbox.service.impl.BlindBoxOrderServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class BlindBoxStockReconciliationTest {

    @Test
    void reconcileStock_doesNotOverwritePendingReservation() {
        BlindBoxTemplate template = new BlindBoxTemplate();
        template.setId(10L);
        template.setStock(2);
        BlindBoxTemplateMapper templateMapper = mock(BlindBoxTemplateMapper.class);
        FlashSaleService flashSaleService = mock(FlashSaleService.class);
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(flashSaleService.getRemainingStock(template.getId())).thenReturn(1);
        BlindBoxOrderServiceImpl service = new BlindBoxOrderServiceImpl(
                null, templateMapper, null, null, null, null, null, null, null, null, flashSaleService);

        service.reconcileStock();

        verify(flashSaleService).getRemainingStock(template.getId());
        verifyNoMoreInteractions(flashSaleService);
    }
}
