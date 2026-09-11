## 1. Entregáveis

- [x] 1.1 Criar coleção Postman v2.1 cobrindo todos os endpoints públicos de negócio com variáveis, exemplos e verificações.
- [x] 1.2 Completar README raiz e corrigir README frontend conforme código e configuração atuais.

## 2. Validação consolidada

- [x] 2.1 Conferir cobertura, parâmetros, JSON resolvido, scripts e links locais; validar OpenSpec e diff; registrar resultados e limitações.

Validação consolidada: 26/26 métodos/paths de negócio comparados aos quatro Resources; 34 requests com parâmetros conferidos, variáveis declaradas e corpos JSON resolvidos. Os 34 scripts compilam e executam com respostas controladas, incluindo proteção da captura de IDs após falhas. Links locais dos READMEs e UTF-8 conferidos; coleção sem variáveis de credenciais. OpenSpec strict: 35 itens aprovados (34 specs e esta change). git diff --check aprovado após remover linha vazia no fim da spec sincronizada.

Não foram executadas chamadas reais da coleção nem importação em interface Postman. Não houve alteração de código nesta rodada documental, portanto não foram repetidas suítes backend/frontend nem atualização AST do Graphify. Revisão humana final pendente; sem commit, push ou archive desta change.

Aceite final: documentação e coleção aprovadas explicitamente pelo usuário em 2026-09-10. Archive autorizado para fechamento do MVP.
