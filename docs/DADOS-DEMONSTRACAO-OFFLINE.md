# Dados fictícios para 24/11/2026

Este procedimento depende de autorização específica para a imagem temporária, para a troca do backend e para a carga no banco persistente. O modo normal continua sendo `http-local` com `sem-integracoes`.

O ambiente offline já ensaiado usa carteiras preparadas com ativos de mercado e operações simuladas. A [auditoria de 20/11](../openspec/changes/preparar-dados-ficticios-offline/tasks.md#auditoria-financeira-e-operacional-de-20112026) cobre esses dados persistidos. Este documento descreve o catálogo integralmente fictício e sua carga específica, com ensaio próprio ainda aberto no ambiente limpo.

## Catálogo

| Tipo | Identidade | Dados da demonstração |
|---|---|---|
| Ação | DEMA3 / BRASIL | Demo Aurora, BRL 24,00 |
| Ação | DEMB4 / BRASIL | Demo Horizonte, BRL 27,00 |
| Ação | ZDMO / EUA | Demo Orbit, USD 120,00 |
| Corretora | 99999999000191 | Corretora Demo Norte, CEP de fixture 99999001, Rua Fictícia Norte |
| Corretora | 99999998000147 | Corretora Demo Sul, CEP de fixture 99999002, Rua Fictícia Sul |

Os dois CNPJs têm dígitos verificadores válidos localmente e são identificadores do catálogo. A exclusividade verificada é dentro do banco de apresentação. Eles não foram confrontados com cadastros públicos. A razão social inclui “(fictícia)”, os endereços são simulados e `validadaMercadoFinanceiro` permanece falso. A situação `ATIVA` só satisfaz o fluxo cadastral simulado. As cotações são fixas, com referência de mercado desconhecida. O modo não carrega clientes nem adaptadores reais e não oferece fechamentos históricos fictícios para prévias.

## Ativação futura após autorização

1. Confira que o projeto confirmado está em HTTP local e sem integrações com `node ops/apresentacao.cjs check "$demo" --modo sem-integracoes --transporte http-local`. Registre o ID da imagem normal do backend, os IDs dos containers e a identidade do volume. Preserve o manifesto original.
2. Empacote o JAR com `mvn -o -DskipTests package` e compare os changesets do JAR com o banco via leitura. Exija zero pendências e lock livre. Prepare a imagem extra `<projeto>-backend:dados-demonstracao` com o Dockerfile de `ops/backend.Dockerfile` e o mesmo runtime local fixado no roteiro de apresentação. O contexto de build deve conter apenas o Dockerfile e o novo JAR copiado como `app.jar`. Registre separadamente o SHA-256 do JAR e do override e o ID da imagem. Confirme-os antes de qualquer troca.
3. Renderize o Compose abaixo sem imprimir valores de configuração. Confirme o mesmo projeto, o mesmo volume, o PostgreSQL e frontend originais, backend sem portas ou rede de saída, e o único ingresso em 127.0.0.1:8081. A terceira extensão substitui redes e montagens do backend pelos recursos internos normais e seleciona o perfil de catálogo. Use a janela de execução exclusiva e preserve os três hashes/IDs conferidos até o retorno.

```powershell
$demoCompose = @('--context','desktop-linux','compose','--env-file',"$demo/runtime.env",
  '-f','compose.operacional.yaml','-f','compose.apresentacao-http.yaml','-f','compose.apresentacao-dados.yaml')
docker @demoCompose config --quiet
```

4. Após a verificação, pare somente o frontend e substitua somente o backend pelo Compose acima. Exija `up -d --no-deps --no-build --pull never --force-recreate backend`. Reative exclusivamente o mesmo frontend com `start frontend` para renovar a resolução do upstream. Confira `/api/auth/csrf`, isolamento, Liquibase com zero alterações, container e volume PostgreSQL antes de carregar.
5. No mesmo projeto, execute o pré-voo com `--verificar`. Ele abre e encerra uma sessão ADMIN e não grava registros do cenário. O catálogo protegido de leitura confirma perfil e execução. O carregador também confere a identidade do banco por uma consulta Docker local somente leitura, redes, imagem, ingresso, cadastros, histórico de operações, custódia e snapshots.
6. Se a conta `julia` estiver ausente, cadastre-a como `USER` pela interface e finalize a definição de senha pelo fluxo de código inicial. Mantenha senha e código fora de logs, histórico e diário. Rode novamente o pré-voo. A carga exige uma conta ativa com credencial definida e usa a sessão de `admin_demo` para os cadastros financeiros.
7. Execute `--aplicar` em janela exclusiva, sem alterações simultâneas pela interface. Forneça a senha administrativa via stdin privado. Exemplo para PowerShell 7, trocando `--verificar` por `--aplicar` apenas depois da aprovação.

```powershell
$senha = Read-Host 'Senha administrativa' -AsSecureString
try {
  [System.Net.NetworkCredential]::new('', $senha).Password |
    node ops/dados-demonstracao.cjs --verificar "$demo"
} finally {
  $senha.Dispose()
}
```

As operações usam preços negociados informados no plano. O diário privado registra a execução, o ID da conta, os IDs retornados e a etapa pendente. A primeira carga exige ausência de colisões nos identificadores do cenário. Nomes de carteira são comparados sem distinguir caixa nem espaços externos para detectar colisões. A identificação de registros já confirmados exige o nome exato e o ID do diário. Nomes anteriores do cenário exigem conciliação antes de prosseguir. Na retomada, o script exige que registros, operações e snapshots correspondam exatamente ao prefixo confirmado. Uma resposta ambígua bloqueia nova execução. Preserve o diário e confronte o estado com as APIs de leitura antes de propor uma conciliação autorizada. O lock coordena processos dos roteiros no mesmo diretório. A exclusividade com usuários na interface é uma condição operacional.

## Resultado esperado

- **Carteira de Crescimento.** Compras de 100 DEMA3 a R$ 20,00 e 50 a R$ 26,00 pela Norte, 10 DEMB4 a R$ 30,00 pela Sul e 2,5 ZDMO a US$ 100,00 pela Norte. Venda posterior de 40 DEMA3 a R$ 28,00 pela Norte. DEMA3 fica com 110 unidades a preço médio de R$ 22,00. BRL final de R$ 2.910,00, ganho realizado de R$ 240,00, não realizado de R$ 190,00 e rentabilidade de cerca de 6,99%. USD final de US$ 300,00, não realizado de US$ 50,00 e rentabilidade de 20%.
- **Carteira Conservadora.** Compras de 20 DEMB4 a R$ 30,00 e 1,5 ZDMO a US$ 110,00, pela Sul. Venda posterior de 5 DEMB4 a R$ 28,00 na mesma corretora. BRL final de R$ 405,00, perda realizada de R$ 10,00, perda não realizada de R$ 45,00 e rentabilidade de −10%. USD final de US$ 180,00, ganho não realizado de US$ 15,00 e rentabilidade de cerca de 9,09%.
- **Evolução.** Duas observações por carteira, capturadas pelo servidor, uma após compras e outra após vendas. Crescimento em BRL passa de R$ 3.870,00 para R$ 2.910,00 e Equilíbrio de R$ 540,00 para R$ 405,00. As séries USD ficam em US$ 300,00 e US$ 180,00 respectivamente. O gráfico retrata patrimônio das posições abertas. A redução após venda é esperada.

## Retorno ao offline normal

Pare somente frontend e backend da apresentação, sem remover os containers ou o volume PostgreSQL. Use o roteiro oficial com a configuração HTTP original para restaurar a imagem web registrada:

```powershell
node ops/apresentacao.cjs start "$demo" --autorizar-migrations --modo sem-integracoes --transporte http-local
node ops/apresentacao.cjs check "$demo" --modo sem-integracoes --transporte http-local
```

Confira novamente zero changesets pendentes, o mesmo ID do PostgreSQL e o mesmo volume. A rota de catálogo volta a ficar indisponível. Use as consultas locais para conferir operações, dashboard e evolução persistida com ADMIN e USER. O frontend e o PostgreSQL não precisam de rebuild.
