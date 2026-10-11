# Auditoria documental do domínio temporal/F4 — 2026-10-10

Estado desta auditoria: Etapa 1, interrompida antes de produção e de qualquer alteração de checkbox. Não constitui correção implementada nem ratificação da F4.

## Evidência e tasks afetadas

- CotacaoCandidata atualmente valida coerência dos campos e microssegundos, mas não restringe ano DATA, piso INSTANTE ou referência versus obtenção própria.
- isCamposTemporaisCotacao é guard de leitura usado pelos contratos frontend; suas expressões regulares aceitam anos negativos/expandidos e não validam calendário real ou relação entre referência e obtenção.
- cotacao-temporal.fixtures.ts e cotacao-temporal.formatter.spec.ts incluem DATA +10000-10-02 e -0001-10-02 como casos aceitos, sem separação explícita entre compatibilidade persistida e nova aquisição.
- AcaoMapper copia metadados persistidos diretamente, sem construir CotacaoCandidata. Isso confirma fronteiras distintas de aquisição e leitura; os novos limites não devem migrar automaticamente para o mapper.
- Tasks 9.1 e 12.3 precisam de reabertura formal para implementar e comprovar o domínio aprovado antes de nova ratificação. Seus checkboxes permanecem intactos por instrução humana; as evidências históricas não são apagadas.
- Tasks 2.1, 2.2, 7.1, 7.2, 10.1 e 11.1–11.4 exigem revalidação direcionada após a correção. Tasks 1.1–1.3, 3.1–3.3, 4.1–4.2, 5.3, 6.1–6.2 e 9.2 requerem comprovação de preservação/compatibilidade; não há evidência nesta auditoria de necessidade de alterar migration, ordenação, lock, atomicidade ou cooldown/reuso.
- 12.1 permanece pendente para validação visual/assistiva apropriada; nenhuma etapa manual final foi iniciada.

## Compatibilidade e dados disponíveis

Foram inspecionados somente código, artefatos e fixtures locais. Fixtures demonstram representação anterior de DATA negativa/expandida, mas não comprovam existência desses valores em banco real. Não houve conexão com banco, saneamento ou alteração de dados.

Nova aquisição exige os limites aprovados. DTO persistido exige tratamento separado e preservação sem fallback. A política exata de leitura anterior ainda deve ser verificada antes de endurecer guards; esta auditoria não aprova novos limites públicos de leitura nem uma migration.

## Parada exigida

Antes de alterar produção, solicitar autorização para reabertura formal de 9.1 e 12.3. Não desmarcar automaticamente. Contagem preservada: 40 tasks, 29 concluídas, 11 pendentes. Se ambas forem reabertas, a contagem será 40/27/13; isso ainda não ocorreu.

Testes backend/frontend, TypeScript, timezones, atualização Graphify pós-implementação e revisão de diff de produção não executados nesta auditoria. Consulta Graphify de relacionamentos executada; não equivale a atualização após implementação.

## Continuidade autorizada e parada por incompatibilidade concreta

Em 2026-10-10 o usuário autorizou formalmente reabrir exclusivamente 9.1 e 12.3. Checkboxes desmarcados, evidências históricas preservadas: 40/27/13; 9.2 concluída e 12.1 pendente.

CotacaoCandidata passou a restringir DATA 0001–9999 e INSTANTE ao intervalo inclusivo entre epoch e obtenção própria, preservando microssegundos e UTC. Frontend acrescenta isNovaAquisicaoCotacao separado dos guards de leitura existentes, com aritmética inteira gregoriana/BigInt e sem Date/relógio. Os DTOs não identificam versão de aquisição; até SEM_NOVA_COTACAO/cooldown pode devolver estado antigo. Por isso os guards de leitura existentes permanecem com sua aceitação anterior, sem aplicar limites financeiros novos indiscriminadamente.

Testes HTTP demonstram leitura preservada de INSTANTE anterior a 1970 e posterior à obtenção persistida, sem provider/Clock. Nenhum banco real foi consultado. Fixtures anteriores de DATA negativa/expandida continuam cobertas como compatibilidade de apresentação, além das novas fronteiras 0001/9999.

Bloqueante concreto: em H2 2.4.240/Hibernate 7.4.1, cadastrar DATA 0001-01-01 responde com o valor correto, mas SQL registra 0000-12-29 e a releitura pela entidade produz 0001-12-31. A mesma classe HTTP executada em PostgreSQL 16.8 temporário aprova 17/17, inclusive o valor SQL e a releitura de DATA 0001. Portanto não é defeito do formatter e não se deve flexibilizar/reduzir o domínio civil aprovado.

Parada antes de modificar mapeamento JDBC, entidades ou migration. Alternativa proposta para autorização: avaliar binding JDBC direto de LocalDate restrito a referenciaData de Acao/HistoricoCotacao (SqlTypes.LOCAL_DATE disponível no Hibernate instalado), mantendo SQL DATE, sem configuração global de calendário/timezone e sem migration/saneamento automático. Deve comprovar compatibilidade de leitura antiga antes de aplicar qualquer mudança de interpretação. Alternativa conservadora: reservar a correção de persistência a uma etapa separada, mantendo F4 bloqueada. Nenhuma das alternativas foi implementada.

## Diagnóstico autorizado da persistência — nova parada de compatibilidade

Autorização humana posterior permitiu investigar e corrigir pontualmente, condicionada à preservação da interpretação de dados antigos. A investigação comprovou interação entre calendário híbrido da ponte java.sql.Date usada pelo Hibernate e calendário proléptico do H2, com influência dos offsets históricos na conversão. PostgreSQL preservou as 12 fronteiras testadas; H2 apresentou diferenças em Ação/histórico e GET. JDBC direto preservou dados novos nos dois bancos, mas reinterpretou valores antigos H2 quando comparado ao leitor atual. Não aplicar a correção sem decisão adicional de compatibilidade. Nenhuma entidade, migration, dado real ou checkbox foi alterado nesta etapa. Diagnóstico completo, matriz/versões/comandos e alternativas em diagnostico-persistencia-referencia-data.md. Contagem mantida em 40/27/13.

## Confirmação H2 efêmero e correção pontual autorizada

Decisão humana subsequente: PostgreSQL persistente oficial; H2 exclusivamente para testes automatizados efêmeros/recriáveis, sem migração de dados H2 descartáveis. Inspeção positiva de perfis/dependências/URLs/arquivos/documentação confirmou o escopo; PRD alinhado à decisão. Aplicado JdbcTypeCode(SqlTypes.LOCAL_DATE) somente nos dois referenciaData, preservando SQL DATE/migration 007/outros campos. Compatibilidade PostgreSQL testada por escrita antiga setDate e leitura nova nas duas entidades/API, para as 12 fronteiras. Round-trip H2 originalmente falho passou. Resultados definitivos, comandos e limitações constam da seção de correção em diagnostico-persistencia-referencia-data.md. Nenhum checkbox ratificado.

Revisão final posterior encontrou nova incompatibilidade PostgreSQL BC fora do domínio novo, mas possível no contrato anterior: SQL 0001 BC tinha leitura pública 0001 AD no binding anterior, enquanto a leitura direta retorna ano zero. Protótipo das duas anotações retirado imediatamente; nenhuma alteração incompatível ficou nas entidades. Suíte completa 1084/1084 havia passado no protótipo, não representa a situação final restaurada. Dois novos testes de leitura BC comprovam manutenção do comportamento anterior em Ação/histórico/API após retirada. Parada por decisão humana de compatibilidade PostgreSQL, sem alteração de dados/migration ou acesso ao banco real. Contagem 40/27/13 mantida; detalhes e alternativas no relatório atualizado.
