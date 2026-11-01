# Plano de limpeza de autenticação — política documental aprovada

Escopo da tarefa 6.5: planejamento, sem implementar job, executar DELETE, alterar schema ou reduzir prazos de segurança. Em 31/10/2026 o solicitante respondeu explicitamente “Aprovar 7 dias e 24 horas” à proposta abaixo, incluindo preservação de auditoria/usuários/singleton, frequência diária e lotes pequenos. A aprovação é documental: não constitui autorização para executar exclusão, instalar job ou acessar banco real.

## Elegibilidade e retenção aprovadas para o plano

| Registro | Elegibilidade aprovada | Preservação |
| --- | --- | --- |
| sessao_autenticacao | Prazo absoluto vencido e sete dias completos após o maior entre expira_em_absoluto e revogada_em, quando existente | Não usar somente inatividade local/ultima_atividade_em para decidir exclusão; não remover sessão que ainda possa ser validada |
| autorizacao_redefinicao_senha | expira_em vencido e sete dias após o maior entre expira_em, consumida_em e invalidada_em, quando existentes | Não excluir autorização ainda válida ou em transação de emissão/consumo; não recuperar código em texto puro |
| contador_abuso | Janela vencida há pelo menos 24 horas | Nunca excluir janela vigente, zerar limite ativo ou liberar tentativas por falha de limpeza |
| auditoria_usuario | Fora da limpeza | Preservar integralmente até existir política específica aprovada; não apagar ator/alvo para contornar FK |
| usuario/controle_seguranca | Fora da limpeza | Não excluir contas, credenciais, versões de segurança nem singleton |

Os prazos de sete dias/24 horas foram aprovados especificamente para este plano; não alteram validade de sessão/código nem limites de tentativas. Não alegar conformidade legal de retenção com base somente neste plano.

## Execução futura recomendada

- Frequência inicial diária, fora do pico. Retenção e frequência de limpeza são independentes da validade da sessão/código.
- Lotes iniciais de até 500 linhas e orçamento de até 30 segundos por execução, interrompendo normalmente ao atingir o orçamento e retomando na próxima execução. Medir duração, removidos e falhas; ajustar depois de medição no ambiente apropriado.
- Seleção por prazo e chave estável, sem paginação OFFSET, usando os índices existentes. Não criar novo índice sem plano/EXPLAIN e evidência de necessidade.
- Transação curta por lote, sem transação abrangendo a execução inteira, chamada externa ou espera artificial. Não carregar entidades/segredos apenas para remover linhas técnicas.
- PostgreSQL futuro: estudar seleção com FOR UPDATE SKIP LOCKED e revalidar o predicado de expiração/retenção após adquirir os locks. A compatibilidade e as corridas precisam de teste real antes de implementar; isto não autoriza PostgreSQL agora.
- Limpeza não deve adquirir singleton/usuário depois de travar filho, invertendo a ordem dos serviços. Operar somente nas linhas técnicas elegíveis, sem lock de usuário ou alteração de versões. Confirmar plano de locks/FKs antes de implementar. Um registro em uso deve ser ignorado para tentativa posterior, não causar espera longa.
- Nunca executar DDL, cascade sobre usuários/auditoria, TRUNCATE ou reuso de SID/ID. A manutenção não deve emitir JWT/código, restaurar sessão ou publicar identificadores em logs.

## Falhas e concorrência

Falha, timeout ou deadlock desfaz apenas o lote atual; lotes anteriores confirmados permanecem removidos. Registrar somente contagens, duração e categoria segura de erro. Parar/adiar sem retry infinito; a próxima execução repete a seleção idempotente. Não compensar falha liberando acesso, zerando contadores ou alterando versões de segurança. Indisponibilidade do banco mantém a recusa segura já implementada.

Usuário autenticado depende de estado persistido e de agora < expiração absoluta e agora - última atividade < dez minutos, além de revogação/versão/conta. Código depende de autorização vigente, tentativas e agora < expira_em. Ausência física de sessão/autorização não autentica nem autoriza redefinição. Predicados e horário são reavaliados pelo serviço após espera conforme o design; limpeza física não é parte da decisão de acesso. Mesmo com job parado indefinidamente, registros expirados continuam inválidos.

Antes de qualquer implementação futura, testar transações reais: lote elegível com dados ativos preservados, bordas exatas de retenção, rollback de lote, repetição idempotente, corrida com atividade/logout/consumo/emissão, contador de janela atual preservado, FKs/auditoria intactas e plano PostgreSQL com locks observados. Não repetir manualmente bordas de autenticação já automatizadas.

## Medição já entregue

SessionCoordinator possui teste de cem interações enquanto a primeira atividade está em voo: uma chamada inicial e uma pendente, duas chamadas sequenciais no total; não há fila de cem chamadas, debounce temporal ou perda do último marcador. A medição é controlada por HTTP/navegador/relógio de teste; não equivale a teste de carga real ou dimensionamento do job futuro.
