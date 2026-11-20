package com.projeto.services;

import com.projeto.repositories.AcaoRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/** Lote interno síncrono; captura termina antes da rede e cada aplicação usa seu próprio proxy. */
@Service
public class AtualizacaoCotacoesCarteiraService {
    private final PosicaoService posicoes;
    private final AcaoService acoes;
    private final AcaoRepository repository;

    public AtualizacaoCotacoesCarteiraService(PosicaoService posicoes, AcaoService acoes,
                                             AcaoRepository repository) {
        this.posicoes = posicoes;
        this.acoes = acoes;
        this.repository = repository;
    }

    public record Resumo(long atualizadas, long semNovaCotacao, long falhas) {
        public Resumo {
            if (atualizadas < 0 || semNovaCotacao < 0 || falhas < 0) {
                throw new IllegalArgumentException("Contadores não podem ser negativos");
            }
        }
    }

    public record Lote(Long carteiraId, Resumo resumo, List<ResultadoAtualizacaoCotacao> resultados) {
        public Lote {
            if (carteiraId == null || resumo == null || resultados == null) {
                throw new IllegalArgumentException("Carteira, resumo e resultados obrigatórios");
            }
            resultados = List.copyOf(resultados);
            if (resultados.stream().map(ResultadoAtualizacaoCotacao::acaoId).distinct().count() != resultados.size()
                    || resumo.atualizadas() != resultados.stream().filter(r -> r.resultado() == RegraDecisaoCotacao.Resultado.ATUALIZADA).count()
                    || resumo.semNovaCotacao() != resultados.stream().filter(r -> r.resultado() == RegraDecisaoCotacao.Resultado.SEM_NOVA_COTACAO).count()
                    || resumo.falhas() != resultados.stream().filter(r -> r.resultado() == RegraDecisaoCotacao.Resultado.FALHA).count()) {
                throw new IllegalArgumentException("Resultados duplicados ou resumo incoerente");
            }
        }
    }

    public Lote atualizar(Long carteiraId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Lote não pode executar em transação envolvente");
        }
        // A chamada atravessa o proxy de leitura consistente; nenhuma entidade escapa da captura.
        List<Long> ids = posicoes.capturarAcoesComPosicaoAberta(carteiraId);
        List<ResultadoAtualizacaoCotacao> resultados = new ArrayList<>(ids.size());
        for (Long id : ids) {
            ResultadoAtualizacaoCotacao resultado;
            try {
                resultado = acoes.atualizarCotacaoCompartilhada(id);
            } catch (DataIntegrityViolationException | ConcurrencyFailureException
                     | UnexpectedRollbackException failure) {
                // Somente falhas isoláveis, após o proxy concluir rollback. Sem retry.
                var acao = repository.findById(id).orElseThrow(() ->
                        new ObjectNotFoundException("Ação não encontrada para o id: " + id));
                String codigo = failure instanceof DataIntegrityViolationException
                        ? ErrorCodes.INTEGRIDADE_DADOS_VIOLADA : ErrorCodes.FALHA_APLICACAO_COTACAO;
                resultado = ResultadoAtualizacaoCotacao.de(acao, RegraDecisaoCotacao.Resultado.FALHA,
                        codigo, ResultadoAtualizacaoCotacao.Origem.CONSULTA,
                        ResultadoAtualizacaoCotacao.estado(acao),
                        new ApiException(HttpStatus.CONFLICT, codigo, "Não foi possível aplicar a cotação"));
            }
            if (resultado.resultado() == RegraDecisaoCotacao.Resultado.FALHA
                    && resultado.origem() == ResultadoAtualizacaoCotacao.Origem.CONSULTA) {
                // Leitura de apresentação após falha, fora do catch de aplicação e sem alterar o PATCH.
                // Se a leitura falhar, propaga globalmente; não fabrica snapshot nem faz retry.
                var snapshot = repository.lerSnapshotCotacao(id).orElseThrow(() ->
                        new ObjectNotFoundException("Ação não encontrada para o id: " + id));
                resultado = resultado.comSnapshot(new AplicacaoCotacaoService.EstadoCotacao(
                        snapshot.getPreco(), snapshot.getTipo(), snapshot.getInstante(), snapshot.getData(),
                        snapshot.getObtencao(), snapshot.getLegado()));
            }
            resultados.add(resultado);
        }
        long atualizadas = resultados.stream().filter(r ->
                r.resultado() == RegraDecisaoCotacao.Resultado.ATUALIZADA).count();
        long semNova = resultados.stream().filter(r ->
                r.resultado() == RegraDecisaoCotacao.Resultado.SEM_NOVA_COTACAO).count();
        return new Lote(carteiraId, new Resumo(atualizadas, semNova,
                resultados.size() - atualizadas - semNova), resultados);
    }
}
