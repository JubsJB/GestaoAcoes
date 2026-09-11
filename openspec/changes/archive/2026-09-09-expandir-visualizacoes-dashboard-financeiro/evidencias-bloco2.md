# Bloco 2 — implementação e evidências (09/09/2026 local)

## Autorização e limites

Executadas tasks já previstas 2.1–2.7. Task 2.8 aguarda revisão humana. Task 1.8 continua aberta: usuário considerou insuficiente a diferenciação visual do Bloco 1 e autorizou explicitamente avançar sem desfazê-lo/refiná-lo. Não declarar atingido o objetivo visual da change. Total: 27 tasks, 14 concluídas e 13 abertas. Bloco 3 não iniciado.

Referências conceituais A/B preservadas: painel financeiro verde/neutro, roscas e linha/área como famílias diferentes dos comparativos. Imagens originais indisponíveis nesta sessão; inspeção técnica de capturas locais não equivale a fidelidade verificada às imagens nem aceite humano.

## Entregas e dados

- Composição P0: rosca BRL e USD separadas, centro com moeda/finalidade, legendas integrais com ticker/empresa/valor autoritativo e numeração correspondente nas fatias legíveis. Textura alternada complementa cor e números. Escalas são próprias de cada moeda. Não há total financeiro central, percentual textual derivado, FX ou reconciliação com resumo.
- compositionGeometry reutiliza projectFinancialValues sem modificá-lo: soma somente pesos normalizados limitados para obter frações da volta e coordenadas. Nunca soma valores monetários. Resultados geométricos não alimentam labels, payloads, persistência ou outras métricas.
- Uma posição positiva preenche a volta. Zero permanece em legenda sem fatia; todos zero mostram estado textual. Negativo/inválido indisponibiliza somente o grupo, sem módulo ou renormalização de subconjunto. Valores subpixel não ganham mínimo artificial; legenda informa limite visual. Todos os ativos permanecem na ordem recebida; sem Outros/top-N/foco por fatia. Em datasets extensos, cores/texturas podem repetir e fatias pequenas não comportam número no desenho: valores e identificação integral permanecem na legenda, sem alegar distinção infinita de cores.
- Histórico P0: linha de snapshots, valores registrados em destaque por moeda com data, eixo temporal real, grid discreto e tooltip com hierarquia melhorada. Área P1 adotada exclusivamente como decoração: mesmo path dos segmentos existentes, fechado até y=222 (base visual vigente), sem alterar domínio/linha/coordenadas. Nenhuma área para ponto isolado nem cruzamento de gap. Não representa lucro/retorno/cotação contínua.
- Histórico textual único, snapshots vazios, IDs, timestamps, datas locais, Enter/Espaço/Escape, foco/toque e botão manual preservados. evolution-geometry.ts, component TS de evolução e serviços não alterados.
- Quatro GET financeiros antes/depois: resumo, posições, resultados-realizados e evolução-patrimonial. Listagem de carteiras do shell continua separada. Zero request adicional do novo componente; nenhum snapshot automático.

## Arquivos e preservação

Criados em frontend/src/app/features/dashboard/position-analysis: composition-geometry.ts/.spec.ts e portfolio-composition.component.ts/.html/.scss/.spec.ts (seis arquivos).

Alterados nesta rodada: position-analysis.component.ts (import/template de integração), position-performance-chart.component.spec.ts (somente teste de integração inclui quarto painel mantendo os três anteriores), evolution/portfolio-evolution.component.html/.scss/.spec.ts. Documentação: proposal/design/tasks, adendo em evidencias-bloco1 e esta evidência.

SHA-256 comparado com frontend/.cache/expansion-bloco2/before.json confirma todos os demais arquivos preexistentes de position-analysis idênticos ao início desta rodada, inclusive implementação/testes próprios de custo, apresentação lossless e implementação de resultado/dot plot. Apenas contêiner e expectativa de integração mudaram. Backend, DTOs, HTTP, parsing, fórmulas, contexto, routes, budgets e dependências intactos. Nenhuma biblioteca nova.

## Testes e build

- Focados Dashboard: 151/151 em 14 arquivos; VITEST_MAX_WORKERS=2.
- Suíte frontend: 484/484 em 68 arquivos; VITEST_MAX_WORKERS=2.
- Production com stats-json: aprovado.
- Nova cobertura: proporções 100/100/200, zeros científicos, inválidos/negativos, extremos/underflow, 100 ativos, BRL/USD, uma moeda/uma posição, legenda sem hover, imutabilidade, limpeza por troca de coleção; área por segmento, gaps e mesmas linhas/coordenadas, ponto único sem área, histórico integral, consulta única e ausência de POST automático.
- Primeira compilação focada detectou tipagem incorreta em it.each do teste novo; corrigida antes dos resultados finais. Execução fora do sandbox autorizada devido à restrição esbuild já conhecida. Sem falhas residuais.

## Renderização real e limites da evidência

Reutilizada infraestrutura Edge headless existente em frontend/.cache/expansion-after/verify.mjs, com destinos/seletores adaptados em frontend/.cache/expansion-bloco2/. Nenhuma infraestrutura de projeto/dependência nova. Dados são fixtures, não banco de produção.

1440×900, 768×1024, 1280×600, 390×844 e 320×740 passaram no ensaio existente de limites globais/controles. Quatro GET financeiros observados em cada carga; listagem do shell preservada. Tooltip por teclado/Escape, selector e drawer verificados pelo ensaio existente. Capturas desktop-composition.png e desktop-evolution.png inspecionadas: roscas/legendas, área interrompida, valores/datas visíveis. Diagnósticos internos dot-scale, menu e células de tabelas continuam detectáveis em larguras reduzidas, sem overflow global; não são atribuídos como corrigidos nesta rodada. Revisão mobile detalhada, texto ampliado e leitor de tela adiados ao fechamento.

Capturas/JSON/scripts/hash apenas em .cache ignorado, sem staging. Aprovação estética pendente.

## Bundle (bytes)

| Item | Antes desta rodada (Bloco 1) | Depois | Delta |
| --- | ---: | ---: | ---: |
| Initial | 514416 | 514417 | +1 |
| Dashboard lazy | 54144 | 63134 | +8990 |
| CSS Dashboard | 2522 | 2522 | 0 |
| CSS evolução | 4466 | 4875 | +409 |
| CSS custo | 1905 | 1905 | 0 |
| CSS resultado/dot | 2500 | 2500 | 0 |
| CSS contêiner analítico | 178 | 178 | 0 |
| CSS composição (novo) | 0 | 1882 | +1882 |

Baseline inicial da change permanece documentado: initial 514417, lazy 52387; delta acumulado lazy +10747. Fonte stats.json: imports estáticos transitivos para initial, entryPoint Dashboard e CSS outputs. Crescimento lazy desta rodada ~16,6% devido a componente/template SVG/legenda/estilos e geometria da rosca, mais apresentação de área/valor do histórico. Não somar novamente CSS embutido ao JS. Initial permanece dentro de 514418; variação de 1 byte acompanha referências de chunks do build, sem promoção de componente lazy.

Warnings: initial +14417 B sobre aviso 500 kB; CSS evolução +875 B sobre aviso 4 kB (antes +466). Limites de erro 1 MB/8 kB e todos budgets intactos. CSS composição abaixo de 4 kB. Não houve otimização ou refatoração dos gráficos anteriores.

## Ponto de parada

Revisar desktop: leitura das roscas, associação com legenda, pesos visuais e densidade; linha/área patrimonial, interrupções, datas/valores e consulta por teclado/toque. Task 2.8 permanece aberta, assim como 1.8. Não avançar ao Bloco 3 nem archive/versionamento.

Validação documental final: strict da change aprovado; strict global 33/33; git diff --check aprovado. Working tree conserva alterações dos Blocos 1 e 2 e artefatos da change, sem staging. Caches/capturas não aparecem no status.

## Revisão humana posterior — conjunto aceito

Usuário aprovou explicitamente Custo × Valor atual (leitura com dados variados/PETR4), Resultado divergente e Rentabilidade dot plot no conjunto. Task 1.8 concluída por este aceite posterior, preservando o registro da rejeição anterior. Tipo donut aprovado; task 2.8 continua aberta exclusivamente quanto ao Histórico. Não realizar novos refinamentos nos três comparativos nem enriquecer donut com percentuais/totais derivados.
