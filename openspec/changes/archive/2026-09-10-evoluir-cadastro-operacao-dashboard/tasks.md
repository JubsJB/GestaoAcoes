## 1. Regra e contrato
- [x] 1.1 Auditar impactos e registrar preco manual e reaproveitamento do dialog.
## 2. Backend
- [x] 2.1 Exigir e validar preco COMPRA/VENDA sem consulta historica no POST; ajustar OpenAPI.
## 3. Frontend
- [x] 3.1 Reutilizar formulario com preco editavel COMPRA e payload lossless.
- [x] 3.2 Abrir dialog lazy com origem fixa, sucesso/erro e refresh contextual.
## 4. Validacao
- [x] 4.1 Testes backend focados e Maven verify completo.
- [x] 4.2 Testes frontend focados/completos (2 workers), production e bundles.
- [x] 4.3 Strict change/global, diff/status e evidencias; revisao humana aprovada e evidenciada.

## 5. Ajustes da revisao humana
- [x] 5.1 Restaurar sugestao historica editavel de COMPRA, cancelamento de contexto antigo e erros; preservar VENDA e POST.
- [x] 5.2 Reservar altura dinamica das mensagens e verificar dialog renderizado.
- [x] 5.3 Auditar PETR4 somente leitura e registrar operacoes/conta/classificacao.
- [x] 5.4 Validar testes, builds, OpenSpec e Git; registrar revisao humana aprovada.

Aprovacao humana final registrada: dialog Dashboard, origem fixa, sugestao COMPRA editavel, VENDA, preco positivo obrigatorio, estimativa, mensagens e desktop. 11/11 concluidas; zero abertas.
