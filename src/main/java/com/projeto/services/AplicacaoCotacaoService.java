package com.projeto.services;

import com.projeto.entities.Acao;
import com.projeto.entities.HistoricoCotacao;
import com.projeto.entities.ReferenciaTipoCotacao;
import com.projeto.integrations.cotacao.CotacaoCandidata;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.HistoricoCotacaoRepository;
import com.projeto.services.exceptions.ObjectNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/** Núcleo compartilhado: candidata obtida fora da transação por individual ou lote. */
@Service
public class AplicacaoCotacaoService {
    private final AcaoRepository acoes;
    private final HistoricoCotacaoRepository historico;
    private final EntityManager entityManager;

    public AplicacaoCotacaoService(AcaoRepository acoes, HistoricoCotacaoRepository historico,
                                  EntityManager entityManager) {
        this.acoes = acoes;
        this.historico = historico;
        this.entityManager = entityManager;
    }

    public record EstadoCotacao(BigDecimal preco, ReferenciaTipoCotacao tipo,
            OffsetDateTime instante, LocalDate data, OffsetDateTime obtencao,
            OffsetDateTime legado) { }

    /** O chamador sem transação envolvente recebe este resultado somente após commit do proxy. */
    public record Aplicacao(RegraDecisaoCotacao.Decisao decisao, EstadoCotacao estado) { }

    @Transactional
    public Aplicacao aplicar(Long id, CotacaoCandidata candidata) {
        Objects.requireNonNull(id, "Identidade obrigatória");
        Objects.requireNonNull(candidata, "Candidata obrigatória");
        Acao acao = acoes.findByIdForUpdate(id)
                .orElseThrow(() -> new ObjectNotFoundException("Ação não encontrada para o id: " + id));
        // A consulta com lock não basta se a entidade já estava no contexto de persistência.
        entityManager.refresh(acao, LockModeType.PESSIMISTIC_WRITE);
        var decisao = RegraDecisaoCotacao.decidir(acao, candidata);
        switch (decisao.efeito()) {
            case SUBSTITUIR_OBSERVACAO -> {
                var dados = decisao.candidataParaSubstituicao();
                acao.substituirObservacao(dados.cotacao(), dados.referenciaTipo(),
                        dados.referenciaInstante(), dados.referenciaData(), dados.cotacaoObtidaEm());
                acoes.saveAndFlush(acao);
                historico.saveAndFlush(new HistoricoCotacao(acao, acao.getCotacaoAtual(),
                        acao.getDataHoraCotacao(), acao.getReferenciaTipo(), acao.getReferenciaInstante(),
                        acao.getReferenciaData(), acao.getCotacaoObtidaEm()));
            }
            case AVANCAR_SOMENTE_OBTENCAO -> {
                acao.aplicarObtencao(decisao.cotacaoObtidaEmResultante());
                acoes.saveAndFlush(acao);
            }
            case PRESERVAR_OBSERVACAO -> { /* Sem escrita, inclusive FALHA semântica. */ }
        }
        // Exceções de escrita/commit propagam; nunca viram classificação semântica ou retry.
        return new Aplicacao(decisao, new EstadoCotacao(acao.getCotacaoAtual(), acao.getReferenciaTipo(),
                acao.getReferenciaInstante(), acao.getReferenciaData(), acao.getCotacaoObtidaEm(),
                acao.getDataHoraCotacao()));
    }
}
