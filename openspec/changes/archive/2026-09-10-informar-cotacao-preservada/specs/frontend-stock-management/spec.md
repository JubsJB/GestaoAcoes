## ADDED Requirements

### Requirement: Comunicação explícita da cotação preservada
Após falha da atualização manual, o detalhe SHALL informar que está usando a última cotação válida disponível, mantendo o valor e a referência temporal anteriores, além da mensagem e detalhes originais do erro. O aviso SHALL permanecer durante uma nova tentativa e SHALL desaparecer somente após sucesso. O detalhe MUST NOT apresentar esse aviso antes de uma falha de atualização nem após falha de carregamento sem ação disponível. Tentativas SHALL continuar exclusivamente manuais.

#### Scenario: Atualização falha com cotação anterior
- **WHEN** uma atualização manual falha por limite, timeout, indisponibilidade ou outro erro
- **THEN** o detalhe informa que usa a última cotação válida disponível, preserva valor, data, mensagem e detalhes e libera a ação sem repetir a chamada

#### Scenario: Recuperação manual
- **WHEN** o usuário tenta novamente após falha
- **THEN** o aviso permanece enquanto a chamada está pendente e desaparece após sucesso, quando valor e data passam a refletir a resposta

#### Scenario: Ausência de falha de atualização
- **WHEN** o detalhe é carregado inicialmente ou falha sem obter uma ação
- **THEN** não afirma que uma cotação foi preservada após atualização
