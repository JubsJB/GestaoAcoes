## 1. Auditoria e regra
- [x] 1.1 Mapear chamadas, falhas, persistencia, Dashboard e historico; definir janelas e escopo.

## 2. Implementacao
- [x] 2.1 Reutilizar cotacao EUA recente e serializar/cooldown de atualizacoes com falha preservada.
- [x] 2.2 Consultar somente GLOBAL_QUOTE na atualizacao; preservar cadastro, BRAPI e timeouts configuraveis.

## 3. Testes e validacao
- [x] 3.1 Cobrir recente/antiga/primeira, repeticao/concorrencia, falhas e sucesso com mocks.
- [x] 3.2 Executar focados e suite backend, strict change/global e diff/status; registrar evidencias sem archive.
