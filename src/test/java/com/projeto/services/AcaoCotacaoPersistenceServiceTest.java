package com.projeto.services;

import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.HistoricoCotacaoRepository;
import com.projeto.services.exceptions.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class AcaoCotacaoPersistenceServiceTest {

    @Mock
    private AcaoRepository repository;

    @Mock
    private HistoricoCotacaoRepository historicoRepository;

    private AcaoCotacaoPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new AcaoCotacaoPersistenceService(repository, historicoRepository);
    }

    @Test
    void persisteCandidatoPosterior() {
        Acao acao = acao();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(acao));
        when(repository.saveAndFlush(acao)).thenReturn(acao);

        Acao resultado = service.atualizarSePosterior(
                1L, new BigDecimal("35.000000"), OffsetDateTime.parse("2026-08-20T15:30:00Z"));

        assertSame(acao, resultado);
        assertEquals(new BigDecimal("35.000000"), acao.getCotacaoAtual());
        assertEquals(OffsetDateTime.parse("2026-08-20T15:30:00Z"), acao.getDataHoraCotacao());
        verify(repository).saveAndFlush(acao);
        ArgumentCaptor<com.projeto.entities.HistoricoCotacao> captor =
                ArgumentCaptor.forClass(com.projeto.entities.HistoricoCotacao.class);
        verify(historicoRepository).saveAndFlush(captor.capture());
        assertEquals(acao.getCotacaoAtual(), captor.getValue().getCotacao());
        assertEquals(acao.getDataHoraCotacao(), captor.getValue().getDataHoraCotacao());
    }

    @Test
    void ignoraTimestampIgualOuAnteriorSemEscrita() {
        for (OffsetDateTime candidato : new OffsetDateTime[]{
                OffsetDateTime.parse("2026-08-19T15:30:00Z"),
                OffsetDateTime.parse("2026-08-18T15:30:00Z")
        }) {
            Acao acao = acao();
            when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(acao));

            assertSame(acao, service.atualizarSePosterior(1L, BigDecimal.TEN, candidato));
            assertEquals(new BigDecimal("30.000000"), acao.getCotacaoAtual());
        }
        verify(repository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
        verify(historicoRepository, never()).saveAndFlush(any());
    }

    @Test
    void falhaSemEstadoParcialQuandoAcaoDesaparece() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundException.class, () -> service.atualizarSePosterior(
                1L, BigDecimal.TEN, OffsetDateTime.parse("2026-08-20T15:30:00Z")));
        verify(repository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
        verify(historicoRepository, never()).saveAndFlush(any());
    }

    @Test
    void mesmoPrecoEmTimestampPosteriorCriaHistorico() {
        Acao acao = acao();
        OffsetDateTime posterior = OffsetDateTime.parse("2026-08-20T15:30:00Z");
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(acao));
        when(repository.saveAndFlush(acao)).thenReturn(acao);

        service.atualizarSePosterior(1L, new BigDecimal("30.000000"), posterior);

        assertEquals(posterior, acao.getDataHoraCotacao());
        verify(historicoRepository).saveAndFlush(any());
    }

    @Test
    void propagaFalhaDoHistoricoParaRollbackDaTransacao() {
        Acao acao = acao();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(acao));
        when(repository.saveAndFlush(acao)).thenReturn(acao);
        when(historicoRepository.saveAndFlush(any())).thenThrow(new RuntimeException("history"));

        assertThrows(RuntimeException.class, () -> service.atualizarSePosterior(
                1L, new BigDecimal("35.000000"), OffsetDateTime.parse("2026-08-20T15:30:00Z")));
    }

    private Acao acao() {
        return new Acao(
                "PETR4", "Empresa", Mercado.BRASIL, Moeda.BRL,
                new BigDecimal("30.000000"), OffsetDateTime.parse("2026-08-19T15:30:00Z")
        );
    }
}
