# Exceções de homologação aceitas para esta entrega

Registro de 29/10/2026, após aprovação pelo usuário da auditoria 6.9, disponível em `target/custodia-auditoria69-20261029-145858/relatorio.md`. O aceite é limitado a esta entrega e não constitui aprovação das verificações não executadas, mudança dos requisitos ou garantia de compatibilidade não demonstrada.

Os critérios originais de [tasks.md](tasks.md), design e specs permanecem intactos. As tarefas 5.10 e 6.5 continuam desmarcadas e parcialmente comprovadas. A validação final 6.10 foi concluída, aprovada tecnicamente e marcada por autorização posterior do usuário; essa aprovação não preenche retroativamente as lacunas manuais aqui registradas.

## Exceção 1 — Leitor de tela real

- **Tarefa relacionada:** 5.10.
- **Verificação não executada:** avaliação sonora com leitor de tela real, incluindo anúncios de títulos, rótulos, validações, carregamento, sucesso e erro.
- **Evidência disponível:** testes automatizados de acessibilidade e verificações em navegador real de teclado, foco e restauração após estorno aprovados. Responsividade e legibilidade também foram verificadas em viewport real de 360 px; essas provas não substituem a avaliação sonora.
- **Limite observado:** o usuário informou que o Narrador abre uma janela, mas não fala. Não houve confirmação humana de anúncios efetivamente ouvidos. A compatibilidade sonora real não está comprovada; não foi demonstrado um defeito sonoro da aplicação.
- **Risco residual:** problemas de anúncio, ordem, duplicidade ou interrupção da fala podem não ter sido detectados pelos testes e pelas verificações de teclado/foco.
- **Decisão:** dispensa aceita explicitamente pelo usuário para esta entrega, com aceitação desse risco residual.
- **Classificação:** avaliação sonora **não executada e não aprovada**; tarefa 5.10 parcialmente comprovada.

Fontes:

- `target/custodia-etapa5-tarefa510-20261027-221437/relatorio.md`: 113 testes direcionados, com limites do JSDOM explícitos.
- `target/custodia-correcao-foco-20261028-223740/relatorio.md`: 75 testes direcionados e correção de foco, posteriormente confirmada em navegador na retomada.
- `target/custodia-homologacao-retomada-20261029-065213/relatorio.md`: estornos residuais, teclado e foco reais.
- `target/custodia-correcao-layout-20261029-071541/relatorio.md`: 82 testes e validação real em 360×800/1280×900.
- `target/custodia-homologacao-residual-20261029-073111/relatorio.md`: massa visual extensa aprovada e ausência de prova sonora.
- `target/custodia-homologacao-assistida-20261029-143802/relatorio.md` e relato posterior do usuário sobre a janela sem fala.

**Relação com o OpenSpec:** 5.10 exige verificar leitor de tela; a spec `frontend-portfolio-management` exige nomes/foco e anúncios no cenário de acessibilidade e responsividade. A implementação e a evidência automatizada estão disponíveis, mas não comprovam a avaliação sonora real prevista. A dispensa aceita não autoriza registrar esse critério manual como integralmente verificado.

## Exceção 2 — Falha seletiva de GET após POST

- **Tarefa relacionada:** 6.5.
- **Verificação não executada:** reprodução manual no navegador de POST confirmado seguido de falha exclusiva no GET da distribuição, com recuperação por retry de leitura.
- **Evidência disponível:** cobertura automatizada aprovada para **POST 201 → GET 500 → retry GET 200**, preservando sucesso da escrita, erro independente da leitura e ausência de POST duplicado. A prova utiliza componentes/diálogos e HTTP simulado; não é apresentada como homologação manual do comportamento visual.
- **Limite observado:** a ferramenta de navegador não ofereceu interceptação seletiva confiável. A única atribuição adicional de 10 PETR4, condicionada à comprovação prévia do bloqueio, não foi executada. Uma recarga normal de GET não foi tratada como prova de falha e recuperação.
- **Risco residual:** o comportamento visual percebido durante a falha seletiva e sua recuperação não foi homologado manualmente.
- **Decisão:** dispensa aceita explicitamente pelo usuário para esta entrega, com aceitação desse risco residual.
- **Classificação:** verificação manual **não executada e não aprovada**; tarefa 6.5 parcialmente comprovada.

Fontes:

- `frontend/src/app/features/carteiras/custodia-recarga.integration.spec.ts`: matriz com falha exclusiva da distribuição, confirmação preservada, retry somente GET e asserção de ausência de novo POST.
- `target/custodia-etapa5-incremento4-20261027-215100/relatorio.md`: cobertura integrada e 426 testes únicos na seleção daquela rodada.
- `target/custodia-correcao-layout-20261029-071541/green-regressoes.log`: última regressão direcionada aplicável, 82 testes em três arquivos; não representa 82 cenários de POST/GET/retry.
- `target/custodia-homologacao-residual-20261029-073111/relatorio.md`: reconciliação das asserções automatizadas e limites da observação em navegador.
- `target/custodia-homologacao-assistida-20261029-143802/relatorio.md`: bloqueio seletivo não comprovado, nenhuma escrita adicional.

**Relação com o OpenSpec:** o requisito funcional “Sucesso de escrita separado da falha de recarga” tem comprovação automatizada suficiente para 5.7/5.9. Já 6.5 é explicitamente uma validação manual que inclui erro de recarga após sucesso. O requisito funcional comprovado, o método automatizado e a exceção de homologação manual são registros distintos.

## Alcance do aceite e acompanhamento

Não se declara encerramento integral sem ressalvas das tarefas 5.10/6.5, da Etapa 5 ou da change inteira. Não há autorização implícita para novas movimentações ou sessões manuais. Um futuro encerramento integral dessas duas tarefas exige a evidência faltante ou revisão formal autorizada de seus critérios/aceite; nenhum critério foi alterado aqui.

As demais evidências aprovadas permanecem válidas em seus escopos. Logs e relatórios em `target/` não são versionados e devem ser conservados com seus hashes. O resumo durável da decisão fica neste documento e em [evidencias.md](evidencias.md). O procedimento aprovado e o registro da execução concluída de 6.10 estão em [validacao-final.md](validacao-final.md); as duas exceções permanecem não homologadas nos pontos dispensados.
