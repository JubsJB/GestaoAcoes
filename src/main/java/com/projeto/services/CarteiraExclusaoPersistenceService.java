package com.projeto.services;

import com.projeto.entities.Carteira;
import com.projeto.repositories.*;
import com.projeto.services.exceptions.*;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Escrita da exclusão; a fachada observa também o encerramento do proxy. */
@Service
public class CarteiraExclusaoPersistenceService {
    private final CarteiraRepository repository;
    private final OperacaoRepository operacaoRepository;
    private final SnapshotCarteiraRepository snapshotCarteiraRepository;
    private final MovimentacaoCustodiaRepository movimentacoes;
    private final EstornoMovimentacaoCustodiaRepository estornos;
    public CarteiraExclusaoPersistenceService(CarteiraRepository repository, OperacaoRepository operacaoRepository,
            SnapshotCarteiraRepository snapshotCarteiraRepository, MovimentacaoCustodiaRepository movimentacoes,
            EstornoMovimentacaoCustodiaRepository estornos) {
        this.repository=repository;
        this.operacaoRepository=operacaoRepository;
        this.snapshotCarteiraRepository=snapshotCarteiraRepository;
        this.movimentacoes=movimentacoes;
        this.estornos=estornos;
    }
    @Transactional(propagation=Propagation.REQUIRED, isolation=Isolation.READ_COMMITTED, readOnly=false, rollbackFor=Exception.class)
    public void excluir(Long id) {
        Integer isolamento = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || (isolamento != null && isolamento != java.sql.Connection.TRANSACTION_READ_COMMITTED)) {
            throw new IllegalTransactionStateException("Exclusão exige transação de escrita READ_COMMITTED compatível");
        }
        Carteira carteira = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Carteira não encontrada para o id: " + id
                ));

        if (operacaoRepository.existsByCarteiraId(id)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.CARTEIRA_POSSUI_OPERACOES,
                    "Carteira possui operações e não pode ser excluída",
                    Map.of("carteiraId", id)
            );
        }

        if (snapshotCarteiraRepository.existsByCarteiraId(id)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.CARTEIRA_POSSUI_SNAPSHOTS,
                    "Carteira possui snapshots e não pode ser excluída",
                    Map.of("carteiraId", id)
            );
        }

        if (movimentacoes.existsByCarteira_Id(id)
                || estornos.existsByMovimentacaoOriginal_Carteira_Id(id)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.CARTEIRA_POSSUI_MOVIMENTACOES_CUSTODIA,
                    "Carteira possui histórico de custódia e não pode ser excluída", Map.of("carteiraId", id));
        }

        repository.delete(carteira);
    }

}
