# Preparação persistente Docker — 16/11/2026

Referência: `estabilizacao-evidencias-entrega-20261115.md` e diagnóstico Docker realizado em 16/11. Decisão do usuário: Compose operacional HTTPS, ambiente local exclusivo/persistente; apresentação principal com dados previamente preparados e providers reais como complemento. V2 permanece; V3 adiada. Não há novo aceite funcional ou visual implícito.

## Entrega localizada

- `ops/apresentacao.cjs`: preparação privada reproduzível, certificado SAN de 30 dias, imagens pelos mesmos digests, recursos exclusivos, verificação de propriedade, banco/identidade, bootstrap separado e opt-in, partida ordenada, prontidão HTTPS com CA explícita e parada sem exclusão. A importação do módulo não executa ações.
- `ops/apresentacao.test.cjs`: testes direcionados das fronteiras acima, sem Docker, secrets reais ou serviços externos; reutilizados os dois testes existentes do executor original.
- `docs/APRESENTACAO-DOCKER.md`: Windows/PowerShell, primeiras ações versus retomadas, confiança TLS manual, custódia dos arquivos, dados sintéticos planejados, limites e ensaio integrado mínimo.

Compose/Dockerfiles/proxy/perfis existentes não foram modificados. Não há novo banco, usuário, operação, certificado ou material secreto gerado nesta etapa. Nenhum container/build foi executado. Não houve alteração de confiança Windows ou autenticação real. Sem alteração de checkboxes; esta infraestrutura não conclui 1.6, 6.6, 7.x ou 8.5 da modernização.

## Histórico da decisão — proposta inicial superada

O backend operacional tem apenas redes internas e destinos de providers desabilitados. Saída livre por uma bridge não cumpre a restrição de conectividade limitada. Proxy de saída com allowlist HTTPS/DNS controlado adiciona componente/política relevante; essa parte foi interrompida antes de implementação, conforme instrução do usuário. Não há novo override ou conectividade habilitada silenciosamente.

A proposta inicial foi substituída pela autorização explícita da alternativa B abaixo. Não foi proposto SQL direto, resposta falsa ou importer para contornar bloqueios. O ambiente não está declarado pronto para a apresentação financeira completa.

## Alternativa B aprovada e implementada — somente demonstração local

O usuário aceitou expressamente a ausência de allowlist de destinos, exclusivamente para a apresentação acadêmica local de 24/11. Isso não autoriza política equivalente em produção. Não há proxy de saída ou mudança nos clientes Java/TLS.

- `compose.apresentacao-integracoes.yaml` é a extensão online exclusiva do Compose operacional original, que permanece intacto. Adiciona somente ao backend uma rede externa dedicada, com prioridade de gateway; mantém as redes API/banco. Configura quatro endpoints HTTPS reais e duas chaves externas por configtree, sem valores versionados. Frontend e bootstrap não recebem saída/chaves; PostgreSQL permanece somente no banco interno. Única porta publicada continua HTTPS loopback 8443.
- `resources/check/start` exigem `--modo sem-integracoes|com-integracoes`. O modo sem integrações usa somente o Compose original; backend não recebe rede ou chaves de providers. Rede opcional já criada permanece guardada, sem recriar/remover volume ou recursos na troca.
- A transição para frontend/backend, recria somente backend para aplicar a composição alvo e reinicia o mesmo frontend para atualizar DNS do upstream. Verifica redes efetivas, banco/volume/IDs e registra modo pendente até a conclusão; não executa bootstrap novamente nem regenera chaves/certificados. Falhas não declaram o modo novo concluído.
- Manifesto v2 com nome da rede opcional e hash da extensão; v1 recusado sem migração automática ou edição de recibos. Nenhuma execução prévia desta preparação foi autorizada. Materiais antigos, se existirem, exigem plano próprio, sem perda de dados.
- Runbook atualizado com modos, arquivos privados, troca, verificações, DNS/TLS/quotas e sequência exata do primeiro provisionamento. O plano de dados sintéticos continua não executado.

## Pendências externas e de execução

1. Confirmar revogação/rotação da credencial PostgreSQL histórica no destino afetado, sem divulgar valor. Credenciais novas no ambiente novo não comprovam revogação antiga.
2. Política B aprovada somente para demonstração; falta comprovar topologia e conectividade no Docker instalado, preservando PostgreSQL isolado e as proteções existentes.
3. Autorizar preparação efetiva de imagens/recursos/certificados e migrations no volume exclusivo; conferir hardware/Docker, subrede, origem e materiais atuais.
4. Autorizar a confiança TLS manual; nenhuma importação automática. A confiança por CA explícita do script não comprova confiança do navegador.
5. Autorizar bootstrap e dados sintéticos separadamente. O plano não cria contas ou dados nesta etapa.
6. Executar somente o ensaio mínimo do runbook, incluindo ADMIN/USER, uma escrita sintética e persistência após parada/retomada; não repetir homologação completa sem causa.

## Risco e prazo

Risco moderado de preparação, concentrado em confiança TLS no navegador, recursos/imagens atuais e saída aos providers. Ausência de allowlist aceita: backend pode alcançar outros destinos, inclusive rede local conforme roteamento do host; rede dedicada não equivale a filtro. Certificado de 30 dias gerado nos ensaios próximos cobre 24/11; confirmar datas efetivas. Conectividade e chaves reais ainda não verificadas impedem assumir carga inicial já disponível. Persistência depende da preservação conjunta do volume e dos arquivos privados. Falhas parciais interrompem e preservam recursos para revisão, sem limpeza automática.

Estimativa ainda não medida: meio dia a um dia para provisionar/ensaiar a parte persistente após autorização. A configuração da alternativa B está implementada; reservar 2–4 horas para o ensaio direcionado de conectividade, além da preparação inicial. Priorizar prova do roteiro integrado antes de refinamentos. Permanecem as dispensas de custódia e pendências visuais já registradas; não reabrir movimentação horizontal.

### Verificação da extensão de modos

RED: cinco testes novos falharam antes das novas fronteiras de modo/topologia/transição. GREEN: **22 testes aprovados**, incluindo os dois existentes de homologação e verificações adicionais de associação real/recursos. Sintaxe Node e validação estrita OpenSpec aprovadas. YAMLs base/extensão carregados com biblioteca já instalada, sem nova dependência; apenas backend alterado no delta e composição estática dos campos alterados aprovada nos dois modos. Não é renderização pelo Compose instalado. `docker compose config`, suporte a `gw_priority`, redes reais, rota, DNS/TLS, chaves/providers e persistência integrada continuam pendentes, pois nenhum Docker, build, recurso, certificado, credencial, bootstrap ou API real foi executado/criado nesta etapa. Sem alteração de checkboxes ou conclusão de critérios funcionais. Compose original, Dockerfiles, Nginx, executor de homologação e backend continuam sem diff.

## Verificação

RED observado antes da implementação; primeiro módulo ausente, depois nove testes de comportamento sem implementação. Três testes adicionais do bootstrap falharam antes da extração da fronteira testável; após extração, GREEN final: **15 testes direcionados aprovados (13 novos e 2 existentes)**. Sintaxe Node e validação estrita OpenSpec aprovadas. Essas evidências não substituem execução em Docker, confiança no navegador ou bootstrap. Nenhum checkbox encerrado por essas verificações.

Revisão dos quatro arquivos novos sem material secreto literal preparado; busca específica não encontrou blocos de chave privada. Isso não é nova auditoria de todo o histórico nem prova de revogação antiga. Compose, Dockerfiles operacionais, executor original e backend continuam sem diff. Graphify atualizado por AST local, sem LLM/API: 14.987 nós, 31.629 arestas, 996 comunidades. Avisos: 12 arquivos sem nós, parser SQL opcional ausente e nomes de comunidades desatualizados; nenhuma dependência instalada ou rotulagem por LLM executada. Documentação permanece referenciada neste registro/runbook, sem alegar extração semântica nova.
