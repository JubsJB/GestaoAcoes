## Context

Os quatro Resources são a referência para cobertura. Configurações Spring, pom.xml, package.json, proxy e migrations determinam as instruções de execução.

## Goals / Non-Goals

Entregar documentação utilizável sem mudar código ou provisionar ambientes.

## Decisions

Postman Collection v2.1, com pastas por domínio e variáveis locais sem chaves de providers. Exemplos de compra/venda usam preço manual; captura de IDs ocorre apenas após sucesso. Exclusão usa uma carteira descartável separada. Verificações de status distinguem sucesso de erro, sem aceitar falhas externas como sucesso.

Validar estaticamente a correspondência completa entre coleção e Resources, resolver variáveis nos exemplos e conferir links locais. Não executar a coleção contra banco/provedores reais nesta rodada.

## Risks / Trade-offs

Dados/limites externos variam → exemplos explicam pré-condições e não prometem sucesso dos providers. A coleção pode modificar dados quando executada → README orienta uso manual em base de desenvolvimento e diferencia a exclusão.
