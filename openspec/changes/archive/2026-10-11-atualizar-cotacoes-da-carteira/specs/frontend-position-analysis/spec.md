## ADDED Requirements

### Requirement: Referência de mercado discreta no desempenho por ativo
A análise SHALL apresentar discretamente junto ao desempenho de cada ativo a cotação e referência real recebidas em posições: DATA somente data civil, INSTANTE data/hora e DESCONHECIDA mensagem explícita. MUST NOT solicitar HTTP, calcular indicador financeiro, converter DATA em timestamp ou classificar defasagem. SHALL preservar ordem, moedas, valores lossless, gráficos e hierarquia sem nova tabela; MUST NOT atribuir data única artificial a cards agregados. A distribuição MUST NOT duplicar obrigatoriamente esse texto.

#### Scenario: Precisão DATA
- **WHEN** AAPL possui DATA 2026-10-02
- **THEN** mostra Cotação de 02/10/2026 sem hora e sem deslocamento do dia

#### Scenario: Precisão INSTANTE
- **WHEN** PETR4 possui instante confiável
- **THEN** mostra data/hora formatada localmente sem alterar DTO

#### Scenario: Referência desconhecida
- **WHEN** a posição possui legado DESCONHECIDA
- **THEN** informa referência indisponível sem usar dataHoraCotacao legado

#### Scenario: Hierarquia preservada
- **WHEN** o desempenho renderiza suas posições
- **THEN** metadado é acessível e discreto, sem HTTP extra nem nova tabela ou cálculo
