## Decisions
Publicar somente o Nginx em 127.0.0.1:8081. Aceitar HTTP no Spring apenas do IPv4 real do Nginx no perfil explícito, validando Host e Origin. O Nginx remove forwarding do cliente e o Spring mantém ambos os cookies Secure. Ensaiar localhost no navegador escolhido.

O override usa o mesmo projeto e PostgreSQL, com imagem separada para o backend HTTP e recibo próprio. Após aprovação, retornar com start --transporte https e o modo de integrações escolhido. Preservar imagens, volume, chaves, changelog e bootstrap existentes.

## Risks
HTTP não cifra o tráfego. A compatibilidade depende do navegador. Exigir Docker Engine 28 ou superior para publicação local.
