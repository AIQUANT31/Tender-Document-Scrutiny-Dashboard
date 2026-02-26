package com.example.services;

import com.example.entity.Tender;
import com.example.repository.TenderRepository;
import com.example.services.tender.TenderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenderServiceTest {

    @Mock
    private TenderRepository tenderRepository;

    @InjectMocks
    private TenderService tenderService;

    @Test
    void getAllTenders_shouldAutoCloseExpiredOpenTenders() {
        Tender t1 = new Tender();
        t1.setId(1L);
        t1.setStatus("OPEN");
        t1.setDeadline(LocalDateTime.now().minusDays(1));

        when(tenderRepository.findAllTendersSorted()).thenReturn(Arrays.asList(t1));
        when(tenderRepository.save(any(Tender.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Tender> result = tenderService.getAllTenders();

        assertEquals(1, result.size());
        assertEquals("CLOSED", result.get(0).getStatus());
        verify(tenderRepository, atLeastOnce()).save(any(Tender.class));
    }

    @Test
    void getTenderById_shouldAutoCloseExpiredOpenTender() {
        Tender t1 = new Tender();
        t1.setId(2L);
        t1.setStatus("OPEN");
        t1.setDeadline(LocalDateTime.now().minusHours(2));

        when(tenderRepository.findById(2L)).thenReturn(Optional.of(t1));
        when(tenderRepository.save(any(Tender.class))).thenAnswer(inv -> inv.getArgument(0));

        Tender result = tenderService.getTenderById(2L);

        assertEquals("CLOSED", result.getStatus());
        verify(tenderRepository, atLeastOnce()).save(any(Tender.class));
    }
}

