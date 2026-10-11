## ADDED Requirements

### Requirement: Atualização explícita das cotações da Carteira
O Dashboard SHALL oferecer Atualizar cotações separado de Atualizar dados no bloco de ações do contexto. SHALL enviar POST orientado à Carteira capturada sem IDs de Ações. A ação SHALL permanecer visível sem posições e desabilitada com explicação acessível, durante carregamento sem conjunto conhecido ou enquanto lote estiver em curso; SHALL anunciar Atualizando cotações e impedir duplo disparo inclusive após troca de seleção. Não há polling, retry ou criação de snapshot. Hierarquia financeira, layout responsivo e contexto global SHALL permanecer.

#### Scenario: Sem posições abertas
- **WHEN** a Carteira selecionada não possui posições abertas
- **THEN** a ação permanece visível, disabled e com texto associado explicando o motivo

#### Scenario: Duplo disparo
- **WHEN** o usuário tenta novamente enquanto o lote está em curso
- **THEN** nenhum segundo POST é enviado

#### Scenario: Lote concluído
- **WHEN** o lote retorna resultado inclusive misto ou com todas as falhas
- **THEN** restaura ação, mostra contadores e tickers com falha em feedback discreto acessível e recarrega os quatro GETs do Dashboard

#### Scenario: Seleção mudou durante lote
- **WHEN** a Carteira muda durante o POST
- **THEN** feedback identifica origem; dados antigos não sobrescrevem contexto novo; recarrega seleção corrente ao concluir

#### Scenario: Resultado não confirmado
- **WHEN** a conexão cai após possível persistência parcial
- **THEN** informa resultado não confirmado sem alegar rollback e sem repetir POST automaticamente

#### Scenario: Sucesso e resultado misto
- **WHEN** o resultado tem somente atualizações ou combinação de classificações
- **THEN** informa quantidade atualizada no sucesso total; no misto informa atualizadas, sem nova cotação e falhas, identificando os ativos afetados

#### Scenario: Recarga falha
- **WHEN** processamento conclui mas GET de recarga falha
- **THEN** preserva feedback do lote e distingue erro dos indicadores, oferecendo recuperação manual
