## Decisions
Usar implementações em memória das interfaces de integração no perfil exclusivo. O perfil não carrega adaptadores ou clientes HTTP reais. Reutilizar serviços, validações e contratos. A ausência de fixture retorna erro local. Validar os perfis ativos, origem e identidade da apresentação. O modo convencional usa a configuração original.

A carga usa sessão ADMIN e CSRF, inventário prévio, plano cronológico e diário privado. Uma resposta ambígua deixa a ação pendente para conciliação, sem reenvio automático. O operador define manualmente a senha de USER pelo fluxo existente.

O retorno ao modo offline normal restaura a imagem/configuração original do backend e mantém dados já cadastrados.
