package com.projeto.services;

import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.OperacaoRepository;
import com.projeto.services.exceptions.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistribuicaoPosicaoCorretoraService {
    private final CarteiraRepository carteiras;
    private final OperacaoRepository operacoes;
    private final CalculadoraDistribuicaoCorretora calculadora;

    public DistribuicaoPosicaoCorretoraService(CarteiraRepository carteiras,
            OperacaoRepository operacoes, CalculadoraDistribuicaoCorretora calculadora) {
        this.carteiras = carteiras;
        this.operacoes = operacoes;
        this.calculadora = calculadora;
    }

    // Own the snapshot even when invoked from a weaker caller transaction.
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE,
            propagation = Propagation.REQUIRES_NEW)
    public CalculadoraDistribuicaoCorretora.Distribuicao consultar(Long carteiraId) {
        carteiras.findById(carteiraId)
                .orElseThrow(() -> new ObjectNotFoundException("Carteira não encontrada para o id: " + carteiraId));
        return calculadora.calcular(carteiraId, operacoes.listarParaDistribuicao(carteiraId));
    }
}
