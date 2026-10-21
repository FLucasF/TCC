---
name: review-database
description: Revisa o banco de um projeto Java da revisão de qualidade - o esquema que o serviço cria num PostgreSQL vazio (pg_dump, script measure_database.sh), as conferências simples (chave primária, dinheiro com 2 casas, chave estrangeira com índice, datas, restrições), como o esquema é criado e evolui, e o código que fala com o banco. Use na coluna "Banco" da revisão.
---

# O banco

Esta skill é material de trabalho para a revisão em andamento: rode o script, leia o resultado e o código, e escreva
a seção "Banco" do `review.md`.

## A medida

```bash
bash .claude/skills/review-database/measure_database.sh <projeto ou final>
```

Empacota o projeto, sobe o serviço uma vez contra um PostgreSQL vazio (o texto diz que o sistema cria o que precisa
ao subir), guarda o esquema em `measures/database-schema.sql` e grava `measures/database.md`: como o esquema é criado
(migrações do Flyway ou do Liquibase, ou `CREATE TABLE` no código), as tabelas, e as conferências simples. Na
manutenção, ele também diz quais migrações da base o agente manteve, editou ou acrescentou.

## O que olhar

- **O esquema evolui?** Migrações versionadas (uma nova para cada mudança, sem editar as que já existem) deixam o
  próximo pedido mudar o banco sem perder dados. `CREATE TABLE IF NOT EXISTS` no código não sabe acrescentar uma coluna
  numa tabela que já existe. **Na manutenção, editar uma migração que a base já tinha quebra o banco de quem já
  roda o sistema** (o Flyway recusa subir quando a migração aplicada mudou): isso é sério.
- **Os dados estão certos?** Dinheiro em `numeric` com 2 casas (nunca ponto flutuante); datas em `date`; chave
  primária em toda tabela; a reserva ligada ao quarto por chave estrangeira; `NOT NULL` no que é obrigatório.
- **As regras que o banco protege:** um quarto não pode ter duas reservas na mesma noite. O roteiro do comportamento
  testa duas reservas ao mesmo tempo; aqui, veja o que impede a duplicação (uma restrição, uma trava, uma transação,
  ou só um `synchronized` que vale para um processo).
- **O código:** SQL com parâmetros (`?`) e não montado com dado de fora (o script lista os candidatos; confira se o que
  entra é constante); conexões devolvidas (try-with-resources) e um pool; transação onde duas escritas têm de andar
  juntas.
- **A carga:** consultas que trazem a tabela inteira para filtrar em Java, quando a tabela cresce sem limite, valem
  uma nota (não derrubam a coluna sozinhas).

## A nota

- `bom`: o esquema criado e evoluído por migrações, os tipos certos, chaves e restrições no lugar, SQL com
  parâmetros, nada que perca ou corrompa dado.
- `com problemas`: funciona e guarda certo, mas com defeitos (uma chave estrangeira faltando ou sem índice, o esquema
  criado no código sem migração, a duplicação de reserva protegida só dentro do processo, uma consulta pesada).
- `ruim`: dinheiro em ponto flutuante, SQL montado com dado de fora, tabela sem chave, dados que não ficam no banco
  (ou somem ao reiniciar), uma migração da base editada, ou o serviço que não sobe contra um banco vazio.

Na seção, dê os números (tabelas, o que as conferências acusaram) e o porquê com `arquivo:linha`.
