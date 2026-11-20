## 1. Entrega isolada
- [x] 1.1 Cobrir catálogo e restrição de execução.
- [x] 1.2 Implementar e testar a carga pelas APIs.
- [x] 1.3 Documentar validação financeira e retorno.

## 2. Ativação após autorização
- [ ] 2.1 Ensaiar no Docker da apresentação mediante aprovação específica.

## Evidências
- RED no carregador ausente e na fronteira local nova. GREEN com 31 testes Node, incluindo servidor de loopback descartável, repetição segura e bloqueio de resposta ambígua.
- Maven verify isolado registrou 2.683 testes, 207 ignorados e zero falhas ou erros. JaCoCo passou com 96,25% de linhas e 84,90% de ramos.
- O teste H2 cobre catálogo, autenticação, CSRF, duas carteiras, três ativos, duas corretoras, preços médios, resultados positivos e negativos e snapshots em BRL e USD.
- Frontend com 1.171 testes aprovados, 98,29% de linhas e 92,00% de ramos.
- Compose renderizado com ambiente fictício. A extensão muda o backend e remove egress e segredos de provedores. OpenSpec validado estritamente.
- O item 2.1 permanece aberto para o ensaio do catálogo e da carga específicos desta change no ambiente limpo.

## Auditoria financeira e operacional de 20/11/2026

A auditoria de leitura do Docker limpo conferiu duas carteiras com ativos de mercado, sete operações simuladas de compra e venda e dois snapshots. A revisão confirmou ordem e ausência de duplicidades, quantidades e preços médios, custo, resultados, patrimônio e rentabilidade por moeda. Os snapshots conservaram os valores históricos anteriores às vendas. A auditoria encontrou zero movimentações de custódia e zero estornos. O PostgreSQL estava saudável, com dez changesets Liquibase e lock livre. O backend permaneceu sem integrações externas.

O [ensaio autenticado](../apresentar-em-http-loopback/tasks.md#ensaio-autenticado-de-20112026) conferiu os resultados exibidos nas duas carteiras. Essas verificações dizem respeito aos dados já persistidos no Docker limpo. O [catálogo e o procedimento de carga específicos desta change](../../../docs/DADOS-DEMONSTRACAO-OFFLINE.md) continuam rastreados separadamente em 2.1.
