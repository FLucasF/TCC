# Revisão: construção, cenário 07

## Visão geral

O projeto tem cinco pacotes: `App` (a montagem), `api` (um `ApiRouter` com todas as rotas do Javalin), `service` (preço, reservas, relatórios, mensagens), `db` (três repositórios JDBC com `DriverManager` e uma classe `Database` que cria as tabelas com `CREATE TABLE IF NOT EXISTS`) e `model`. Uma requisição entra no `ApiRouter`, que lê o corpo como `Map`, chama o serviço (ou, em alguns casos, o repositório direto) e devolve um `Map` como JSON. Os dados ficam no PostgreSQL, em três tabelas. O que há de mais sério está no comportamento e no banco.

- **A confirmação não funciona do jeito que o texto descreve.** Ela exige um `amountPaid` no corpo e responde 400 sem ele (`api/ApiRouter.java:128`), e grava o valor que o cliente mandou, em vez do total da reserva.
- **As rotas entram depois do `start`** (`App.java:41-48`), e as primeiras chamadas recebem 404.
- **O serviço não sobe de novo** sobre um banco que já tem reservas: o `DELETE FROM rooms` esbarra na chave estrangeira (`db/Database.java:86`).
- **Nada impede duas reservas simultâneas do mesmo quarto:** as duas foram criadas nas 10 de 10 rodadas.
- **As regras de envio não seguem nenhum pattern:** ficam em três `if` dentro de `MessageService.sendMessage`, com o modo de teste num `static` lido do ambiente.

## Arquitetura

**Números:** 5 pacotes, 9 dependências entre eles, nenhum ciclo. O Javalin só aparece em `com.pousadamarealta` e `api`, e o JDBC só em `db`. A maior classe é `ApiRouter` (328 linhas), seguida de `ReservationService` (188) e `ReservationRepository` (168).
**Nota:** com problemas

As camadas existem e as dependências apontam na direção certa: borda → serviço → dados, sem ciclo. Os serviços não conhecem o Javalin nem o SQL. A borda, porém, não é fina:

- **O `ApiRouter` depende direto dos três repositórios** (`api/ApiRouter.java:23-25`) e os usa no lugar do serviço em `getReservation` (`api/ApiRouter.java:114`), `getDocument` e `getMessages` (`api/ApiRouter.java:231`).
- **Uma regra de negócio mora no handler:** "segunda via só para reserva paga" está em `api/ApiRouter.java:198-199`.
- **A montagem do comprovante também está na borda,** duplicada em `checkOut` e `getDocument` (`api/ApiRouter.java:163-189` e `192-226`).
- **A montagem fica no `main`** (`App.java:27-48`), mas registra as rotas depois de `Javalin.create().start(port)` (`App.java:41-48`). É um erro de montagem que aparece no comportamento.
- **Há configuração escondida nas peças:** o modo de teste é lido do ambiente dentro do serviço (`service/MessageService.java:13`), e `Database` tem um singleton `static` (`db/Database.java:9,20-25`) que o `main` nem precisaria.

## Design de baixo nível

**Números:** 3 regras de envio (validar o endereço, desviar no modo de teste, copiar para o arquivo). As 3 estão como `if` dentro de um único método (`service/MessageService.java:24,35,45`) e nenhuma está numa estrutura. Não há interface de envio nem embrulhos.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern pedido (Decorator).** Nada aproxima a solução do pattern. `MessageService.sendMessage` (`service/MessageService.java:21-59`) faz tudo em sequência:

- testa o endereço e grava `BLOCKED`;
- pergunta por `IS_TEST_MODE` para reescrever o destino, o assunto e o texto;
- pergunta de novo por `!IS_TEST_MODE` para gravar a cópia.

A combinação por modo está escrita dentro do envio, e não na montagem. O modo vem de um `static final` lido de `System.getenv` (`service/MessageService.java:13`), que nenhum teste consegue trocar. As regras anunciadas (madrugada, limite por dia, "a segunda pousada não quer a cópia") vão exigir mexer nesse mesmo método e em mais `if`. É exatamente o caso que a referência chama de omissão.

**Exagero:** nenhum. Não há interface com uma implementação só, fábrica ou camada que só repassa.

**O resto do código** tem defeitos espalhados:

- **Tipos:** a situação da reserva e o plano são `String` soltos, comparados com literais em vários lugares (`service/ReservationService.java:67,87,100,113,118`, `api/ApiRouter.java:198-199`, `db/ReservationRepository.java:70,95`). Um plano inválido como `"FOO"` é aceito e gravado (conferido pela API).
- **Dinheiro sem escala fixa:** `BigDecimal.ZERO` deixa `discount` e `amountPaid` sair como `0` no JSON, sem as duas casas (`service/ReservationService.java:58`, `service/PricingService.java:36`).
- **Formato do comprovante:** `formatMoney` não põe o separador de milhar (`service/ReservationService.java:178-180`). O comprovante sai "R$ 1739,70", e não "R$ 1.739,70" (conferido pela API).
- **Duplicação:**
  - `formatCpf` está copiado em `api/ApiRouter.java:322` e `service/ReservationService.java:182`;
  - o mapa do comprovante aparece duas vezes no `ApiRouter`;
  - em `checkOut` e `getDocument`, o texto do comprovante é gerado e jogado fora (`api/ApiRouter.java:168,205`).
- **Erros:**
  - os códigos de erro viajam como mensagem de `IllegalArgumentException`;
  - todo handler tem um `catch (Exception)` que transforma qualquer falha, até a do banco, em 400 `INVALID_REQUEST` com a mensagem da exceção (`api/ApiRouter.java:106,119,134,…`);
  - em `createReservation`, o `((Number) body.get(...)).intValue()` estoura antes da checagem de `null` (`api/ApiRouter.java:81-91`) e só acerta o 400 por acaso.

## Testes

**Números:** 11 testes rodaram, 0 falharam, 11 métodos escritos. Cobertura de 67 de 703 linhas (9,5%) e de 17 de 150 ramos (11,3%). A API teve 0 de 228 linhas cobertas e o pacote `db`, 3 de 166 (`measures/tests.md`).
**Nota:** ruim

Os testes de preço conferem valores:

- 599,76 na alta temporada (`src/test/java/com/pousadamarealta/service/PricingServiceTest.java:116-124`);
- o desconto de 89,96 e o meio-para-o-par (`PricingServiceTest.java:127-155`).

O resto do sistema não tem teste nenhum: as situações da reserva, o cancelamento e a devolução, os códigos de erro, os relatórios, o comprovante e a API.

**A parte nova do cenário** tem só quatro testes de validação de endereço (`MessageServiceTest.java:24-50`). Todos usam `anyMatch` sobre o status, e nenhum confere:

- se a cópia para o arquivo é gerada, com "[Cópia] " no assunto e o mesmo texto;
- se o bloqueio deixa de gerar a cópia;
- nada do modo de teste, que nem é testável com o `static final` lido do ambiente.

A pergunta da mutação: apagar a cópia (`service/MessageService.java:45-56`), trocar "[TESTE] " ou tirar a linha "Destinatário original" não faria nenhum teste falhar. Com menos de 10% de cobertura e a regra nova quase sem rede, a coluna está quase ausente.

## Banco

**Números:** 3 tabelas, todas com chave primária e dinheiro em `numeric(10,2)`. As conferências acusaram (`measures/database.md`):

- a chave estrangeira `reservations(room_number)` sem índice;
- nenhum CHECK nas colunas de situação e plano;
- nenhuma restrição UNIQUE ou EXCLUDE;
- nenhum pool de conexões, nenhuma transação explícita e nenhum `FOR UPDATE`.

O esquema é criado no Java, sem migração. Todo o SQL usa parâmetros.

**Nota:** ruim

- **O serviço não sobe de novo sobre o próprio banco.** A cada subida, `insertRooms` executa `DELETE FROM rooms` (`db/Database.java:86`). Quando já existe uma reserva, a chave estrangeira recusa e o `main` cai com `PSQLException: ... violates foreign key constraint "reservations_room_number_fkey"`. Reproduzi isso à mão, e o roteiro do comportamento também viu: "o serviço sobe de novo: não subiu". Os dados ficam no banco, mas o sistema só volta se alguém apagar as reservas.
- **Não há reserva dupla protegida.** A disponibilidade é um `SELECT` seguido de um `INSERT` em conexões separadas, sem transação nem trava (`service/ReservationService.java:168`, `db/ReservationRepository.java:64-83`), e não existe restrição de exclusão. Nas 10 rodadas simultâneas, as duas reservas foram criadas.
- **O esquema não evolui:** `CREATE TABLE IF NOT EXISTS` no código (`db/Database.java:40-83`) não sabe acrescentar colunas.
- **As conexões** são abertas uma a uma com `DriverManager` (`db/Database.java:27-29`), sem pool, mas são devolvidas com try-with-resources.

## Comportamento

**Números:** 18 de 63 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 17 de 46 |
| os dados depois de reiniciar | 0 de 1 |
| o cenário no modo normal | 0 de 9 |
| o cenário no modo de teste | 0 de 5 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

Três defeitos explicam quase tudo:

1. **As rotas são registradas depois do `start`** (`App.java:41-48`). As primeiras chamadas, logo que a porta abre, recebem 404 "Endpoint ... not found" (reproduzi: o primeiro `GET /rooms` deu 404 e, dois segundos depois, 200). Isso derrubou as primeiras conferências (os quartos, a criação, o preço de 599,76) e, em cascata, as que dependiam daquela reserva.
2. **A confirmação exige `{"amountPaid": ...}` no corpo** (`api/ApiRouter.java:128`). O texto não prevê corpo e diz que o pago é o total. Sem corpo, a resposta é 400 `INVALID_REQUEST`, e nenhuma reserva fica confirmada. Daí vêm as falhas em cadeia:
   - não sai nenhum e-mail, e por isso as 14 conferências do cenário falham;
   - não há segunda via, check-in nem check-out;
   - o cancelamento STANDARD devolve 0,00 em vez de 299,88;
   - os relatórios saem vazios.

   Com o corpo, o sistema aceita qualquer valor (aceitou 1,00 por uma reserva de R$ 1.739,70). Para o dono da pousada, o site do jeito combinado não consegue confirmar nenhuma reserva, e o hóspede não recebe nada.
3. **O serviço não volta depois de reiniciar** (o `DELETE FROM rooms`, contado na coluna do banco).

**O que funciona.** Chamando a confirmação à mão com `amountPaid`, as regras de envio se comportam como o texto pede:

- a cópia vem logo depois da original;
- `ana@gmail` fica `BLOCKED`, sem cópia;
- no modo de teste, tudo vai para `testes@...`, com "[TESTE] " e a linha do destinatário original.

Esse caminho, porém, não é o que o site usa.

**Os defeitos que vi fora do roteiro:**

- o comprovante sem separador de milhar ("R$ 1739,70");
- `amountPaid` e `discount` como `0`, sem as duas casas, na criação;
- o relatório financeiro lista a STANDARD cancelada com devolução total, com 0,00, quando deveria omiti-la (`db/ReservationRepository.java:95`);
- duas reservas do mesmo quarto criadas ao mesmo tempo, nas 10 de 10 rodadas.

## Dúvidas

O roteiro marcou "o quarto ocupado numa das noites: 409 ROOM_UNAVAILABLE" como falho (veio 201). Provavelmente é uma cascata da primeira reserva perdida no 404 do início, já que a consulta de sobreposição (`db/ReservationRepository.java:70-71`) parece certa e as reservas canceladas liberam o quarto como devem. Não isolei esse caso à parte. Não muda a nota.
