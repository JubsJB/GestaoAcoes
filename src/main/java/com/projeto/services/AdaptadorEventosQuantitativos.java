package com.projeto.services;

import com.projeto.entities.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Hibernate;
import static com.projeto.services.EventoQuantitativo.*;
import static com.projeto.services.NormalizadorEventosQuantitativos.*;

/**
 * Fronteira de leitura entre entidades já carregadas e o núcleo escalar. Não consulta,
 * inicializa proxies, usa relógio ou abre transações. Aceita somente identidades persistidas.
 * O chamador garante fontes completas no mesmo snapshot, inclusive ausência de estorno
 * verificada por consulta; listas isoladas não comprovam completude nem commit.
 * Candidatas temporárias são montadas separadamente pelo chamador, fora deste adaptador.
 */
public final class AdaptadorEventosQuantitativos {
    public record Fontes(List<Financeiro> financeiros, List<Custodia> custodias) {
        public Fontes {
            financeiros = List.copyOf(financeiros);
            custodias = List.copyOf(custodias);
        }
    }

    /** Uma consulta explícita por original, sem vínculos extras, repetidos ou desconhecidos. */
    public Fontes adaptar(long carteiraId, long acaoId, List<Operacao> operacoes,
            List<MovimentacaoCustodia> movimentacoes, List<VinculoEstornoConsultado> vinculos) {
        if (carteiraId <= 0 || acaoId <= 0 || operacoes == null || movimentacoes == null || vinculos == null) {
            throw new IllegalArgumentException("Contexto e fontes completas são obrigatórios");
        }
        exigirCarregada(operacoes, "operacoes");
        exigirCarregada(movimentacoes, "movimentacoes");
        exigirCarregada(vinculos, "vinculos");
        Map<Long, VinculoEstornoConsultado> porOriginal = new HashMap<>();
        for (var vinculo : vinculos) {
            if (vinculo == null || porOriginal.putIfAbsent(vinculo.getMovimentacaoOriginalId(), vinculo) != null) {
                throw new IllegalArgumentException("Vínculo de estorno nulo ou duplicado");
            }
        }
        List<Financeiro> financeiros = new ArrayList<>();
        for (var operacao : operacoes) {
            exigirCarregada(operacao, "operacao", "id", "carteira", "acao", "corretora", "tipo",
                    "quantidade", "dataOperacao", "ordemNoDia");
            contexto(carteiraId, acaoId, operacao.getCarteira(), operacao.getAcao());
            if (operacao.getOrdemNoDia() == null) throw new IllegalArgumentException("Ordem financeira obrigatória");
            financeiros.add(new Financeiro(identidade(Dominio.FINANCEIRO, operacao.getId()), carteiraId,
                    acaoId, operacao.getAcao().getMercado(), operacao.getTipo(), operacao.getQuantidade(),
                    operacao.getDataOperacao(), operacao.getOrdemNoDia(), grupo(operacao.getCorretora())));
        }
        List<Custodia> custodias = new ArrayList<>();
        for (var original : movimentacoes) {
            prepararOriginal(original, carteiraId, acaoId);
            var consulta = porOriginal.remove(original.getId());
            if (consulta == null) throw new IllegalArgumentException("Vínculo da original não foi informado");
            var identidade = identidade(Dominio.CUSTODIA, original.getId());
            var estorno = consulta.getEstorno();
            VinculoEstorno escalar = VinculoEstorno.ausenciaVerificada(identidade);
            if (estorno != null) {
                exigirCarregada(estorno, "estorno", "id", "movimentacaoOriginal", "dataRegistro", "motivo");
                prepararOriginal(estorno.getMovimentacaoOriginal(), carteiraId, acaoId);
                estorno.validarEstruturaHistorica();
                escalar = VinculoEstorno.presente(identidade, id(estorno.getId()));
                // Mesmo ID/contexto não autorizam duas versões divergentes da original no snapshot.
                if (!custodia(original, escalar).equals(custodia(estorno.getMovimentacaoOriginal(), escalar))
                        || !original.getDataRegistro().equals(estorno.getMovimentacaoOriginal().getDataRegistro())) {
                    throw new IllegalArgumentException("Estorno referencia original incompatível");
                }
            }
            custodias.add(custodia(original, escalar));
        }
        if (!porOriginal.isEmpty()) throw new IllegalArgumentException("Vínculo sem original no contexto recebido");
        // Reutiliza a autoridade escalar para quantidade, domínio, identidades e colisões.
        // Não calcula saldos nem revalida vendas legadas como novas candidatas.
        new NormalizadorEventosQuantitativos().normalizar(carteiraId, acaoId, financeiros, custodias);
        return new Fontes(financeiros, custodias);
    }

    private static void prepararOriginal(MovimentacaoCustodia original, long carteiraId, long acaoId) {
        exigirCarregada(original, "movimentacao", "id", "carteira", "acao", "corretoraOrigem",
                "corretoraDestino", "natureza", "quantidade", "dataEfetiva", "dataRegistro",
                "ordemFinanceiraAncora", "posicaoNumerador", "posicaoDenominador");
        contexto(carteiraId, acaoId, original.getCarteira(), original.getAcao());
        grupo(original.getCorretoraOrigem());
        exigirCarregada(original.getCorretoraDestino(), "corretoraDestino", "id");
        original.validarEstruturaHistorica(); // Reconstrução persistida estrita, sem Clock ou reparo.
    }

    private static Custodia custodia(MovimentacaoCustodia original, VinculoEstorno vinculo) {
        return new Custodia(identidade(Dominio.CUSTODIA, original.getId()), original.getCarteiraId(),
                original.getAcaoId(), original.getMercado(), original.getNatureza(), original.getQuantidade(),
                original.getDataEfetiva(), original.getOrdemFinanceiraAncora(), original.getPosicaoRacional(),
                grupo(original.getCorretoraOrigem()), grupo(original.getCorretoraDestino()), vinculo);
    }

    private static void contexto(long carteiraId, long acaoId, Carteira carteira, Acao acao) {
        exigirCarregada(carteira, "carteira", "id");
        exigirCarregada(acao, "acao", "id", "mercado");
        if (id(carteira.getId()) != carteiraId || id(acao.getId()) != acaoId) {
            throw new IllegalArgumentException("Referências incompatíveis com carteira/ação");
        }
    }

    private static Grupo grupo(Corretora corretora) {
        if (corretora == null) return Grupo.semCorretora();
        exigirCarregada(corretora, "corretora", "id");
        return Grupo.corretora(id(corretora.getId()));
    }

    private static Identidade identidade(Dominio dominio, Long valor) {
        return Identidade.persistida(dominio, id(valor));
    }

    private static long id(Long valor) {
        if (valor == null || valor <= 0) throw new IllegalArgumentException("Identidade persistida positiva obrigatória");
        return valor;
    }

    private static void exigirCarregada(Object entidade, String nome, String... propriedades) {
        if (entidade == null) throw new IllegalArgumentException(nome + " obrigatória");
        if (!Hibernate.isInitialized(entidade)) {
            throw new IllegalStateException(nome + " deve estar carregada antes da adaptação");
        }
        for (String propriedade : propriedades) {
            if (!Hibernate.isPropertyInitialized(entidade, propriedade)) {
                throw new IllegalStateException(nome + "." + propriedade + " deve estar carregada antes da adaptação");
            }
        }
    }
}
