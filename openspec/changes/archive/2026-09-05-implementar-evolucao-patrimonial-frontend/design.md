## Context

Ver `proposal.md` para motivação e escopo. O Dashboard já possui seleção canônica por `carteiraId`, carregamento cancelável por contexto, parser JSON lossless, formatadores textuais, feedback normalizado e layout responsivo. O backend fornece uma série completa, ordenada e potencialmente esparsa, além de criação manual append-only; ele não pagina, normaliza lacunas ou cria snapshots automaticamente. Não existe biblioteca gráfica instalada.

## Goals / Non-Goals

**Goals:**

- Integrar evolução sem duplicar seleção, rota ou regras contextuais do Dashboard.
- Isolar models, HTTP, transformação visual e apresentação em fronteiras testáveis.
- Preservar a string financeira como fonte autoritativa em toda saída textual.
- Produzir SVG acessível e responsivo sem dependência nova.
- Manter criação manual explícita e segura contra double-submit e races.

**Non-Goals:**

- Alterar backend, contratos, persistência ou geração de snapshots.
- Criar filtros, paginação, agregação, interpolação, métricas derivadas ou conversão cambial.
- Resolver nesta entrega o crescimento indefinido do histórico no servidor.
- Criar rota, biblioteca gráfica, Canvas, polling ou automação de snapshot.

## Decisions

### 1. Seção modular dentro do Dashboard

A evolução será um componente próprio renderizado pelo Dashboard somente com Carteira válida. O componente receberá a identidade/contexto atual e manterá service, models, estados e apresentação específicos, enquanto o pai continuará autoritativo para seleção e query string. Mudança de `carteiraId` reinicia o contexto e invalida imediatamente o anterior.

Alternativas rejeitadas: rota própria, por duplicar contexto e contrariar o escopo; inclusão no detalhe de Carteira, por afastar a evolução dos demais indicadores e replicar infraestrutura financeira.

### 2. Service dedicado com parsing lossless compartilhado

Um service específico solicitará GET e POST como texto quando houver payload financeiro, aplicará `parseLosslessJson` com allowlist mínima para `patrimonioAtual` e validará toda a estrutura. Models usarão IDs `number` seguros, timestamp e decimal como `string`, e moeda estrita. Erros continuarão atravessando a normalização compartilhada, incluindo reinterpretação lossless de `StandardError` textual quando necessária.

Alternativa rejeitada: adicionar evolução ao `DashboardService` monolítico, pois ampliaria acoplamento e dificultaria testes isolados.

### 3. Orquestração independente, mas reload global coerente

A evolução terá pipeline cancelável próprio para não fazer sua falha derrubar resumo, posições e resultados. O botão global “Atualizar dados” disparará tanto a carga financeira atual quanto a evolução, oferecendo comportamento previsível de atualização completa. Um retry local repetirá apenas o GET da evolução. Nenhum reload executará POST.

Alternativa rejeitada: incluir o GET no mesmo `forkJoin` atual, pois tornaria a página financeira inteira indisponível quando somente o histórico falhar. Também foi rejeitado deixar “Atualizar dados” sem atualizar evolução, pois o usuário esperaria que a página inteira fosse renovada.

### 4. POST manual como único produtor de snapshot

“Registrar snapshot” será ação explícita e separada. Um estado `creating` impedirá novo POST enquanto a operação estiver pendente, sem bloquear consultas ou outras ações do Dashboard. No sucesso, haverá confirmação e novo GET somente se a carteira de origem ainda for a seleção atual. No erro, o botão será reabilitado e a mensagem normalizada permanecerá associada ao contexto de origem.

Nenhum lifecycle, reload, troca, operação, cotação, timer ou polling chamará o POST.

### 5. SVG próprio e dois gráficos independentes

Cada moeda originará um gráfico SVG separado, com seu título e escala. A série usará ordem backend e manterá referências ao snapshot original. O SVG será fluido por `viewBox` e dimensões CSS, sem biblioteca ou Canvas. A representação textual será a fonte acessível principal; o SVG será complementar.

Alternativa rejeitada: um único eixo, por sugerir equivalência; Chart.js/ng2-charts, por dependência e Canvas; Canvas próprio, por custo de acessibilidade e testes.

### 6. Duas representações do decimal, com fronteira explícita

O model guardará somente o token string lossless. Uma transformação efêmera poderá produzir `number` aproximado para domínio e coordenada Y; esse número viverá apenas no modelo geométrico do SVG. Labels, tooltip, lista, comparação apresentada e confirmação sempre referenciarão a string original. A geometria não será persistida, enviada nem reutilizada como indicador.

Essa exceção não autoriza `Number` ou `parseFloat` em parsing, formatação ou regra financeira. Testes arquiteturais devem delimitar o único helper de geometria autorizado e provar que consumidores textuais não recebem o aproximado.

### 7. Segmentação por gaps, não normalização

Para cada moeda, a transformação percorrerá snapshots em ordem e abrirá novo segmento após qualquer snapshot sem componente daquela moeda. Não será criado zero, carry-forward ou interpolação. Um segmento com um ponto renderizará apenas marcador; segmentos com dois ou mais poderão renderizar linha. Snapshot vazio continuará na lista histórica e interromperá ambas as séries.

Alternativa rejeitada: ligar observações através da lacuna, pois sugere continuidade não observada.

### 8. Eixo temporal e densidade visual

Coordenadas X respeitarão o instante real, sem assumir granularidade diária; empates defensivos permanecerão distinguíveis pelo `snapshotId`. O model preservará ISO original e a apresentação reutilizará horário local do navegador, rotulado como local. Tooltip/lista terão data e hora completas; o eixo selecionará subconjunto de labels conforme largura, sem remover pontos do dataset.

### 9. Estados independentes e preservação do Dashboard

A seção distinguirá loading, empty, single point, content, snapshot vazio, erros de GET e criação. Os estados superiores de nenhuma Carteira, seleção pendente ou inválida impedirão sua montagem/carga. Falha de evolução não apagará dados atuais das outras seções; troca de carteira apagará imediatamente apenas a evolução anterior e cancelará logicamente seus efeitos.

### 10. Acessibilidade e responsividade

A seção terá heading, descrição, moeda e símbolos textuais. Loading/criação usarão anúncio polido; erros usarão feedback urgente. Se pontos oferecerem tooltip, serão focáveis e acionáveis por mouse, teclado e toque, com foco visível. A lista histórica sempre estará visível ou será revelável por controle com estado acessível, preservando snapshots vazios.

Desktop exibirá gráficos fluidos e histórico detalhado; tablet reduzirá labels; mobile empilhará gráficos/legenda e usará lista estreita, sem overflow horizontal da página. Nenhuma informação financeira será removida por breakpoint.

### 11. Limitação de volume

Todos os snapshots continuarão no dataset. É permitido reduzir labels, gridlines, marcadores puramente decorativos e custo de DOM sem descartar observações ou recalcular valores. Centenas de pontos são risco baixo; milhares podem pressionar SVG e lista; volumes muito grandes exigirão futura evolução backend com contrato explícito.

## Risks / Trade-offs

- [Conversão geométrica perde precisão] → restringir ao pixel, manter string em toda saída e testar a fronteira.
- [GET completo cresce indefinidamente] → evitar DOM decorativo excessivo e registrar necessidade futura de paginação/filtro backend.
- [Muitos timestamps próximos poluem o eixo] → reduzir somente labels e manter tooltip/lista completos.
- [POST rápido pode gerar muitos snapshots distintos] → bloquear apenas double-submit pendente; não inventar deduplicação além do backend.
- [Falha isolada da evolução pode degradar a página] → estado e retry locais sem remover indicadores existentes.
- [Troca durante POST] → associar resultado à carteira de origem e ignorar efeitos no contexto novo.

## Migration Plan

1. Introduzir models, parser/guards e service sem alterar o Dashboard visível.
2. Construir transformação geométrica, SVG e alternativa textual isoladamente.
3. Integrar a seção e o reload global ao contexto existente.
4. Habilitar criação manual e seus estados/races.
5. Validar regressões do Dashboard, shell, Operações e infraestrutura lossless.

Rollback: remover a seção e sua integração de reload, preservando integralmente o Dashboard anterior; nenhuma migração de dados ou rollback backend é necessário.
