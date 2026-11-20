# Resultado do consumidor Angular

## Escopo e arquivos

2026-10-11:modelos,contrato/parser e consumidor HTTP,sem painel visual ou alteracao de componentes. Backend,contrato HTTP,specs consolidadas,migrations e dados locais intactos. Arquivos experimentais preservados. Nenhuma operacao Git de escrita.

Criados em frontend/src/app/features/carteiras:
- models/distribuicao-corretoras.ts:seis interfaces completas com quantidades textuais,nulos explicitos,IDs number e dataOperacao textual.
- distribuicao-corretoras.contract.ts:parser lossless e guard estrutural dedicado.
- distribuicao-corretoras.service.ts:Observable HTTP frio,pela configuracao central da API.
- distribuicao-corretoras.contract.spec.ts e distribuicao-corretoras.service.spec.ts:testes de contrato e transporte.
- testing/distribuicao-corretoras.fixtures.ts:cinco exemplos JSON normativos,minificados sem alterar valores e utilizados somente pelos testes.

Modificado frontend/src/app/core/http/lossless-json.ts:opcao numericTokensOnly com defaultfalse,sem mudar comportamento dos consumidores anteriores. Quando ativada pelo novo consumidor,rejeita campos numericos fornecidos como strings no wire antes da conversao lossless. Alteracao localizada ao scanner existente,sem nova dependencia ou parser financeiro paralelo. Documentacao:tasks.md e este relatorio.

## Modelos e precisao

DistribuicaoCarteiraResponse,PosicaoDistribuicaoResponse,QuantidadeCorretoraResponse,DiagnosticoCorretoraResponse,PrimeiroDeficitResponse eDiagnosticoReconciliacaoResponse preservam nomes e nulabilidade do contrato. Numero JSON do backend nao e alterado para string no servidor. HTTP usa responseType:text;scanner preserva literalmente tokens de quantidadeConsolidada,quantidadeAtribuidaTotal,quantidadeLiquida,quantidadeFinal e saldoAposOperacao como strings ANTES de JSON.parse. Exemplo999999999999999999999.123457 mantido integralmente;escala e expoentes tambem preservados. Nenhuma conversao de quantidade por Number/parseFloat,arredondamento,soma ou limite artificialNUMERIC19.

Identificadores/ordem sao capturados primeiro como tokens,verificados como inteiros positivos e convertidos para number somente quando exatamente representaveis. Padrao atual Angular usa IDs number seguros;IDs acima deNumber.MAX_SAFE_INTEGER geram falha explicita,nao arredondamento ou identidade incorreta. Isso nao reduz o dominio int64 do backend:suporte futuro a IDs acima da faixa segura do frontend exigira representacao consistente dos identificadores em toda navegacao/contexto,fora desta etapa. Ordem tambem respeita int32 positivo.

Data da primeira evidencia e LocalDate de OPERACAO,nao DATA financeira de cotacao:calendario gregoriano proleptico sem Date/timezone,com suporte a ano zero,negativo e expandido legitimamente serializados por Java,sem introduzir piso1970 ou limite0001-9999. Texto original preservado.

## Validacao e autoridade

Todos os campos obrigatorios e ausencia de campos adicionais em cada objeto;arrays nunca null;IDs positivos e unicos;texto nao vazio;mercado/moeda coerentes;tokens numericos validos;consolidado positivo;linhas quantitativas nao zero e evidencia com saldo negativo. Corretora null exige rotuloSem corretora informada. Cada linha deve encontrar diagnostico identificado com mesmo nome;duplicatas rejeitadas. Diagnostico global possui somente codigoDIVERGENCIA_QUANTIDADES,sem corretoraId.

Flags sao recebidas,nao produzidas pelo guard. Nulabilidade condicional validada:historico exige primeira evidencia,saudavel exige null;reconciliada exige diagnosticoglobalnull e divergente exige objeto com codigo previsto. Primeira evidencia valida id,data,ordem e saldo negativo. Nao soma grupos/compara totais,reexecuta operacoes,calcula primeira ocorrencia nem compara com outro GET. Ordem das arrays e mantida,sem sort ou correcao. Nenhum registro e ocultado ou transformado em saudavel. JSON/shape invalido falha como erro tecnico,nao sucesso vazio.

## Transporte, erros e cancelamento

GET /carteiras/{carteiraId}/posicoes/distribuicao-corretoras usa API_BASE_URL e caminho codificado. Confere carteiraId do proprio response contra contexto solicitado;nenhuma comparacao entre snapshots independentes. Inconsistencias informativas HTTP200 permanecem sucesso com diagnosticos originais.

Erros usam NormalizedHttpError/normalizeHttpError e interceptor existentes. StandardError textual e interpretado separadamente,inclusive detalhes quantitativos lossless,com originalError preservado. Erros de rede/HTTP nao JSON preservam status/classificacao tecnica. Resposta invalida e erro tecnico generico,sem expor corpo na mensagem. Nao ha retry/polling/subscription interna/cache/globalstate. Nova chamada permite tentativa manual. Unsubscribe cancela transporte;switchMap substitui requisicao antiga sem entregar resposta anterior. Painel futuro podera usar Subscription/generation existente no detalhe da Carteira,sem introduzir estado visual nesta etapa.

## Testes e resultados

Primeira rodada focada:94 testes,6 arquivos aprovados. TypeScript encontrou dois casts intencionais para mutacao de fixtures nos testes;corrigidos por cast via unknown,sem relaxar modelos de producao. Rodada final:

```powershell
npx ng test --watch=false --include='src/app/features/carteiras/**/*.spec.ts' --include='src/app/core/http/*.spec.ts' --include='src/app/features/operacoes/*.spec.ts' --include='src/app/features/dashboard/dashboard.service.spec.ts' --include='src/app/features/dashboard/evolution/*.spec.ts'
npx tsc --noEmit -p tsconfig.app.json
npx tsc --noEmit -p tsconfig.spec.json
```

222 testes aprovados em23arquivos;zero falhas. TypeScript app/spec aprovados. Regressoes incluem Carteiras/detalhe,HTTP/interceptor/parser,servicos de Operacoes/Dashboard/Evolution e parser de Operacoes. Nao somar reexecucoes como cobertura nova. Nenhuma suite backend ou prova temporal JDBC/BC repetida.

Novos testes cobrem cinco exemplos aprovados,vazio,negativo/regularizado/zero/null/divergencia,primeira evidencia,nulos/campos obrigatorios/extras,tokens invalidos/quotednumbers,magnitude/escala/expoentes,IDs fracionarios/nao seguros,ordem recebida,ausencia de recalc,calendario civil legado,contexto incorreto,HTTP500 StandardError,503 nao JSON/rede,status,sem retryautomatico,nova chamada manual,cancelamento e substituicao. Fixtures com totais diferentes eflagreconciliadatrue comprovam que guard NAO inventa reconciliacao;isso nao certifica o calculo do backend ou autoriza dados inconsistentes em producao.

## Tasks e riscos

Validacoes finais:OpenSpec strict da change aprovado;git diff --check aprovado;whitespace/EOF verificado explicitamente nos9arquivos desta etapa,inclusive novos,sem problemas. Avisos LF/CRLF preexistentes nao sao erros de whitespace. Diff dos arquivos novos e alteracao localizada do parser revisados. Graphify AST atualizado:10092nodes,18399edges,810communities;avisos conhecidos de6configs semnodes,parserSQL opcional ausente e rotulos comunitarios defasados,sem bloqueio funcional.

Somente5.1 e6.3 concluidas:29 tasks,14 concluidas,15 pendentes. 6.1/6.2 permanecem abertas pela parte visual/avisos/estados/snapshots do painel;5.2/5.3/5.4 nao implementadas. Reexecutar6.3 apos novas alteracoes do painel. PostgreSQL temporario4.6 ecertificacao financeira4.5 continuam pendentes.

Riscos:IDs int64 acima da faixa segura Angular falham explicitamente;guard estrutural nao certifica aritmetica/cronologia e confia nos diagnosticos do backend;estado/acessibilidade/retry do painel ainda nao implementados. Nenhuma nova decisao funcional ou mudanca de contrato necessaria nesta etapa. Proximo passo:painel localizado no detalhe,formatters lossless existentes,estados independentes,cancelamento por contexto eavisos autoritativos sem soma/percentuais ou comparacao com GETposicoes.
