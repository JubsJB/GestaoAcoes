package com.projeto.services;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import java.sql.SQLException;
import java.util.*;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;

/** Tradução no limite que observa também o encerramento do proxy; nunca repete a escrita. */
final class FalhasEscritaOperacao {
    private FalhasEscritaOperacao() { }
    static RuntimeException traduzir(RuntimeException falha) {
        // O domínio já decidiu código e detalhes; causas técnicas não reclassificam essa decisão.
        if (falha instanceof ApiException || falha instanceof com.projeto.services.exceptions.ObjectNotFoundException) {
            return falha;
        }
        Set<Throwable> vistos = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean integridade = false;
        for (Throwable causa = falha; causa != null && vistos.add(causa); causa = causa.getCause()) {
            if (causa instanceof PessimisticLockingFailureException
                    || causa instanceof jakarta.persistence.PessimisticLockException
                    || causa instanceof jakarta.persistence.LockTimeoutException
                    || causa instanceof org.hibernate.exception.LockAcquisitionException
                    || (causa instanceof SQLException sql && sql.getSQLState() != null
                            && Set.of("40001", "40P01", "55P03").contains(sql.getSQLState()))) {
                return new ApiException(HttpStatus.CONFLICT, ErrorCodes.CONFLITO_TRANSACIONAL_TRANSITORIO,
                        "Conflito transacional; a operação não foi confirmada");
            }
            integridade |= causa instanceof org.springframework.dao.DataIntegrityViolationException
                    || causa instanceof org.hibernate.exception.ConstraintViolationException;
        }
        if (integridade) return new ApiException(HttpStatus.CONFLICT, ErrorCodes.INTEGRIDADE_DADOS_VIOLADA,
                "A operação viola uma regra de integridade dos dados.");
        return falha;
    }
}
