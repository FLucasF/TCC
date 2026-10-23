# Revisão: construção, cenário 07

## Visão geral

O projeto tem 7 pacotes em camadas por nome: `routes` (os handlers do Javalin), `service` (as regras, com o SQL dentro), `db` (a conexão e o `CREATE TABLE`), `model`, `util` (preço e validação de e-mail) e `exception`. Tudo é `static`: não há objeto montado no `main`. Uma requisição entra em `Application.java:18-30`, passa por um método estático de `*Routes`, que lê o JSON com um `ObjectMapper` e chama um método estático de `*Service`. Esse método abre uma conexão com `DriverManager` (`db/Database.java:23-25`) e monta o SQL ali mesmo. Os dados ficam em três tabelas do PostgreSQL, criadas com `CREATE TABLE IF NOT EXISTS` no código. O mais sério:

- **A confirmação exige um campo `amountPaid` no corpo** (`routes/ReservationRoutes.java:116-122`), que o texto não pede. Sem ele, a resposta é 400, e o fluxo inteiro quebra: confirmação, e-mail, check-in, check-out, segunda via, relatórios e quase todo o cenário. Com o campo, o sistema aceita qualquer valor como pago, não o total.
- **O modo de teste manda para `testes@` sem o "[TESTE] " no assunto e sem a linha "Destinatário original"** (`service/MessageService.java:37-45`).
- **O cancelamento é aceito depois do check-in** (`service/ReservationService.java:132`).
- **Duas reservas simultâneas do mesmo quarto foram criadas nas 10 rodadas de 10.**
- **As três regras de envio estão escritas como `if` dentro de um único método, sem estrutura.**

## Arquitetura

**Números:** 7 pacotes, 9 dependências entre eles, nenhum ciclo. As maiores classes são `ReservationRoutes` (286 linhas) e `ReservationService` (281). O JDBC aparece em `db` e em `service`; o Javalin e o Jackson só em `routes` e na raiz.
**Nota:** com problemas

As dependências vão na direção certa (routes → service → db/model/util), e as regras não conhecem o Javalin. Mas não existe camada de dados: os quatro serviços montam e executam o SQL eles mesmos, misturando regra e acesso ao banco. Exemplos:

- `service/ReservationService.java:49-79`: o INSERT da reserva;
- `service/ReservationService.java:242-259`: a consulta de disponibilidade;
- `service/MessageService.java:58-100`;
- `service/ReportService.java:18-38`;
- `service/RoomService.java:15-50`.

Não há montagem:

- todas as classes são coleções de métodos `static` que se chamam diretamente (`ReservationService.java:25` → `RoomService.getRoomById`; `ReservationService.java:117` → `MessageService.sendMessage`);
- a configuração é lida em inicializadores estáticos (`db/Database.java:13-21`, `service/MessageService.java:18`).

Por isso, nada pode ser substituído nem combinado de outro jeito por instalação, que é justamente o que o cenário pede.

A borda HTTP também não é fina. Os handlers repetem validações que o serviço também faz (`ReservationRoutes.java:40-60` e `ReservationService.java:30-40`). O handler busca o tipo do quarto direto no `RoomService` (`ReservationRoutes.java:241-248`), em duplicata com `ReservationService.java:233-240`. E traduz `null` em código de erro (`ReservationRoutes.java:126-130`). É uma mistura localizada, sem ciclo, mas presente em todas as camadas.

## Design de baixo nível

**Números:** dos 3 casos pedidos (bloqueio, cópia para o arquivo, modo de teste), 0 estão numa estrutura própria; os 3 são `if` dentro de `MessageService.sendMessage` (`service/MessageService.java:20-56`). Não há interface de envio, embrulho nem montagem por modo.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto anuncia regras que se combinam por modo e por instalação: madrugada, limite por dia, "a segunda pousada não quer a cópia". A resposta esperada é um embrulho do envio por regra, empilhado na montagem. O agente fez o contrário: um único método estático com as três regras em `if`.

- O bloqueio está em `MessageService.java:26-31`.
- O desvio do modo de teste está em `:37-41`.
- A cópia está em `:47-55`, presa ao modo por `!TEST_MODE` dentro da própria regra.
- O modo vem de uma constante estática lida do ambiente (`:18`). Desligar a cópia numa instalação, ou acrescentar a regra da madrugada, só se faz editando esse método. É omissão.

O próprio `if` do modo de teste tem um defeito: calcula `finalSubject` e `finalBody` (`:38-39`), mas grava o `subject` e o `body` originais, atribuídos em `:23-24`. Só `message.to` usa o valor calculado (`:43`).

**Exagero.** Não há estrutura sem necessidade: contenção. `PriceCalculator.PriceBreakdown` (`util/PriceCalculator.java:45-59`) é só um portador de valores.

**O resto do código.** Os defeitos estão espalhados:

- **Tipos soltos.** A situação e o plano são `String` comparadas à mão em vários lugares (`ReservationService.java:101,132,137-138,159,176`; `ReservationRoutes.java:207-208`), sem `enum`.
- **Estado mutável exposto.** Os modelos têm todos os campos `public` e mutáveis (`model/Reservation.java:7-22`, `model/Message.java:4-9`).
- **Erros escondidos:**
  - `catch (Exception e)` transforma qualquer falha, inclusive do banco, em 400 `INVALID_REQUEST` com `e.getMessage()` (`ReservationRoutes.java:84-86, 133-136, 151-154, 169-172, 190-193, 218-221`);
  - `null` serve ao mesmo tempo para "não existe" e para "não pode" (`ReservationService.java:101-103`). Por isso, confirmar uma reserva que não existe dá 409, não 404 (`ReservationRoutes.java:126-130`).
- **Duplicação:**
  - `errorResponse` foi copiado em quatro classes (`RoomRoutes.java:23`, `MessageRoutes.java:23`, `ReportRoutes.java:70`, `ReservationRoutes.java:280`);
  - a busca do tipo do quarto aparece duas vezes;
  - as validações aparecem duas vezes;
  - há `ObjectMapper` declarados e nunca usados (`RoomRoutes.java:11`, `MessageRoutes.java:11`, `ReportRoutes.java:14`).
- **Dinheiro no comprovante.** É formatado com `String.format("R$ %.2f")` no locale da máquina (`ReservationService.java:229-231`). Sai "R$ 499.80" e "R$ 13337.70", não o "R$ 1.234,56" pedido.
- **Regras de domínio erradas:**
  - o cancelamento aceita `CHECKED_IN` (`ReservationService.java:132,137`);
  - a reserva pendente cancelada fica com `refund` null em vez de zero (`:136`);
  - o validador de e-mail aceita "ana@.com" e "ana@gmail.", porque conta o ponto em qualquer posição do domínio, não "no meio" (`util/EmailValidator.java:32-39`).

O preço está certo: `BigDecimal` com HALF_EVEN a cada passo (`util/PriceCalculator.java:23-43, 67-71`).

## Testes

**Números:** 15 testes rodaram, 0 falharam; 15 métodos escritos em 2 classes. Cobertura de 8,3% das linhas (55 de 664) e 19,9% dos ramos (35 de 176). Os pacotes `routes`, `service` e `db` têm 0% de cobertura. Não há teste pela API nem contra o banco.
**Nota:** ruim

Os testes cobrem só as duas classes utilitárias.

- **`PriceCalculatorTest`** confere valores reais: R$ 299,88 na alta temporada, 599,76 e o desconto 116,97/662,83 (`PriceCalculatorTest.java:28-69`).
- **`EmailValidatorTest`** confere os casos óbvios (`EmailValidatorTest.java:7-47`). Não testa "ana@.com" nem "ana@gmail.", por isso o defeito do ponto passou.

Ficaram sem nenhum teste:

- a situação da reserva e as transições permitidas;
- o cancelamento e a devolução;
- os códigos de erro;
- o comprovante e o e-mail;
- os relatórios;
- toda a parte nova do cenário além do validador.

Pela pergunta da mutação:

- apagar o `!TEST_MODE` da cópia (`MessageService.java:47`) não derruba nenhum teste;
- trocar o endereço de testes ou o prefixo "[Cópia] " também não;
- o defeito real do modo de teste, o assunto e o texto ignorados, passou sem rede;
- a confirmação que exige `amountPaid` e o cancelamento depois do check-in também passaram sem teste.

## Banco

**Números:** 3 tabelas, todas com chave primária. Todo o dinheiro está em `numeric(10,2)` e as datas em `date`. A chave estrangeira `reservations.room_number` não tem índice. Não há CHECK, UNIQUE nem EXCLUDE. Não há pool nem transação explícita. Há 8 trechos de SQL montados com `+`. No comportamento, as 10 rodadas de reserva simultânea criaram as duas reservas.
**Nota:** ruim

**Nada impede a reserva dupla.** A disponibilidade é verificada numa conexão (`service/ReservationService.java:42, 242-259`) e o INSERT acontece em outra (`:49-72`), sem transação, sem trava e sem restrição de exclusão no esquema (`db/Database.java:40-60`). O roteiro confirmou o resultado: dois hóspedes no mesmo quarto, na mesma noite, em 10 de 10 tentativas. É dado errado no banco.

**O valor pago vem do cliente.** O `amount_paid` é gravado com o número que o cliente manda (`ReservationService.java:107-110`), sem conferência com o total. Uma confirmação com `{"amountPaid": 1.00}` gravou 1,00 numa reserva de R$ 499,80, e o cancelamento devolveu 1,00. Isso contamina o relatório financeiro.

**SQL montado por concatenação.** As consultas concatenam valores vindos da requisição (`RoomService.java:38`, `ReservationService.java:89, 248-251`, `ReportService.java:26-27, 59`). Os valores já chegam convertidos em `int`, `LocalDate` ou `YearMonth`, então não há injeção explorável hoje, mas é o padrão errado.

**Outros pontos:**

- o esquema é criado com `CREATE TABLE IF NOT EXISTS` no código (`Database.java:31-71`), sem migrações, então não sabe evoluir uma tabela existente;
- não há pool: cada consulta abre uma conexão nova com `DriverManager` (`Database.java:23-25`);
- o relatório filtra o mês com `to_char(check_in, ...)` (`ReportService.java:59`), o que impede o uso de índice.

## Comportamento

**Números:** 31 de 65 conferências passaram.

| parte | passaram |
|---|---|
| a parte comum do enunciado | 28 de 46 |
| os dados depois de reiniciar | 1 de 3 |
| o cenário, no modo normal | 1 de 9 |
| o cenário, no modo de teste | 0 de 5 |
| as reservas simultâneas | 1 de 2 |

**Nota:** ruim

**A causa raiz da maior parte das falhas** é `POST /reservations/{id}/confirmation` responder 400 "amountPaid is required" quando o site chama sem corpo, como o texto descreve (`routes/ReservationRoutes.java:116-120`). O texto diz que o pago é o total da reserva. Sem confirmação, caem em cascata:

- o e-mail de confirmação;
- a segunda via;
- o check-in e o check-out;
- as devoluções;
- os relatórios, vazios ("total,0/5", "total,,,,0.00");
- quase todo o cenário, porque nenhuma mensagem é gerada.

Para o dono da pousada, isso quer dizer que o site não consegue confirmar nenhuma reserva.

Chamei a API à mão, mandando `amountPaid`, para ver o que existe por trás:

- **Modo normal:** funciona. O endereço válido sai SENT e a cópia "[Cópia] " para `arquivo@` vem logo depois. "ana@gmail" fica BLOCKED, sem cópia.
- **Modo de teste:** quebrado. A mensagem vai para `testes@pousadamarealta.com.br`, mas com o assunto original, sem "[TESTE] ", e sem a linha "Destinatário original" (`service/MessageService.java:37-45`). Quem testa não tem como saber para quem a mensagem iria.
- **O comprovante:** sai com "R$ 499.80" em vez de "R$ 499,80". A conferência "Total: R$ 599,76" falharia mesmo com a confirmação funcionando.

**Falhas que não vêm da confirmação:**

- **Cancelamento depois do check-in.** É aceito (`ReservationService.java:132`), quando devia dar 409. Uma reserva já ocupada pode ser cancelada com devolução integral.
- **Reserva pendente cancelada.** Devolve `refund` null em vez de 0,00 (`:136`).
- **Reservas simultâneas.** Em 10 de 10 rodadas, as duas foram criadas: dois hóspedes no mesmo quarto na mesma noite. Esse defeito pesa no banco e é citado aqui.
- **Os dados depois de reiniciar.** As 2 falhas dessa parte vêm das mesmas causas:
  - a reserva aparece CANCELLED porque o cancelamento depois do check-in foi aceito;
  - o quarto "livre" vem do check-out que nunca ocorreu.

  As mensagens persistem.

**Lido no código, sem chegar a ser medido** por causa da cascata: o relatório financeiro inclui a reserva cancelada com devolução total, com valor 0,00, porque o filtro é `amount_paid > 0` (`ReportService.java:61`). O texto pede só as que deixaram dinheiro com a pousada.

## Dúvidas

- A linha final do relatório financeiro sai como `total,,,,599.76` (`ReportService.java:89`), e o roteiro espera `total,599.76`. O texto só mostra o exemplo da ocupação (`total,2/5`), então o formato com colunas vazias pode ser uma leitura razoável. A falha medida, porém, veio da confirmação quebrada, não do formato.
- O comprovante usa o locale padrão da JVM (`ReservationService.java:230`). Numa máquina em pt-BR, sairia "499,80", mas ainda sem o separador de milhar ("13337,70" em vez de "13.337,70").
