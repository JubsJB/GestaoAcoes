## Context

POST /acoes → AcaoResource.cadastrar → AcaoService.cadastrar → AlphaVantageAdapter.consultar. Service chama o provider uma vez; validação e persistência não consultam HTTP. O RestClient não configura retry. Antes: SYMBOL_SEARCH, OVERVIEW somente sem nome, GLOBAL_QUOTE. Sucesso normal: 2 requests; fallback: 3; falhas interrompem na etapa correspondente; validação local/duplicidade/chave ausente: 0. Reuse/cooldown existentes pertencem a PATCH.

## Goals / Non-Goals

Limitar cada cadastro a duas requests, mantendo validação exata de ticker, região EUA, USD, nome obrigatório e cotação positiva representável. Sem cache novo, retry, consultas reais, Graphify ou mudanças no refresh.

## Decisions

Após a evidência real de burst fornecida pelo usuário, aguardar `integration.alpha-vantage.registration-interval-ms` (variável `ALPHA_VANTAGE_REGISTRATION_INTERVAL_MS`, default 1100, inteiro positivo) depois de receber e validar SYMBOL_SEARCH e antes de GLOBAL_QUOTE. A espera integral após a resposta garante pelo menos esse intervalo entre as duas chamadas, sem depender do relógio civil. Falhas de busca/nome ausente encerram antes da espera. Atualização de cotação permanece sem espera nova. Sleeper substituível em teste; interrupção restaura o flag da thread e encerra com o erro existente de indisponibilidade, sem chamar a cotação.

A configuração é validada na inicialização. No container, uma customização exige passar a variável ao serviço (uma variável apenas no .env não é automaticamente injetada pelo Compose); o default já funciona sem alterar Docker. Manter a instrumentação diagnóstica durante a homologação.

O escopo é o par de chamadas de um cadastro: não há coordenador global entre cadastros concorrentes, outros fluxos ou consumidores da mesma chave. A quota diária de 25 chamadas continua sob controle do provider; respostas HTTP 429/Note/Information continuam sendo tratadas como antes.

Manter SYMBOL_SEARCH para reutilizar identidade e nome; remover OVERVIEW e rejeitar nome ausente com 422 DADOS_EXTERNOS_INCOMPLETOS. Consultar GLOBAL_QUOTE uma vez após validar a busca e conferir seu símbolo antes da validação financeira existente no service.

Uma chamada não satisfaz todos os campos: SYMBOL_SEARCH fornece identificação/nome sem preço; GLOBAL_QUOTE fornece símbolo/preço sem nome. OVERVIEW não fornece a cotação atual requerida e seria redundante com uma busca completa. Referência: https://www.alphavantage.co/documentation/ (Search Endpoint, Quote Endpoint e Company Overview; consulta documental, sem execução da API).

Testes MockMvc com controller, service e adapter reais e HTTP simulado contarão requests; somente persistência será substituída. Cobrir sucesso NVDA/AAPL, identidade/moeda/nome inválidos, cotação inválida, duplicidade e limites em ambas as etapas.

## Risks / Trade-offs

- Busca sem nome que antes poderia ser recuperada por OVERVIEW agora falha explicitamente → não inventar nome nem persistir dados incompletos.
- O caminho normal já usava duas chamadas → esta mudança não resolve cota já esgotada; preservar 429 e aguardar disponibilidade do provider para homologação humana.

## Migration Plan

Sem migration. Aplicar o backend atualizado pelo fluxo habitual; rollback consiste em reverter a alteração do adapter. Nenhum deploy ou comando Docker faz parte desta rodada.
