# Revisão: construção, cenário 01

## Visão geral

O projeto (`projeto/`) tem seis pacotes: `com.marealta` com o `main`, `api` com as rotas e o `ErrorResponse`,
`api.handlers` com os handlers do Javalin, `service` com as regras e todo o SQL, `database` com o pool Hikari estático,
o `CREATE TABLE IF NOT EXISTS` e a carga dos quartos, e `domain` com classes de dados de campos públicos. Uma requisição
passa assim: `Routes.register` (`api/Routes.java:8-21`) liga a rota a um método estático do handler, que lê o JSON com
Gson e chama um método estático do serviço. O serviço valida, calcula e grava pelo `Database.getConnection()` estático.
Os dados ficam em três tabelas no PostgreSQL. O defeito mais sério é que o `pom.xml` não traz o Jackson
(`pom.xml:21-57`), e o Javalin 6 precisa dele para o `ctx.json`. Por isso **toda resposta JSON da API é um 500**,
inclusive as de erro: o `catch` dos handlers chama `ctx.json` de novo e falha outra vez. A reserva, a confirmação e o
e-mail são gravados no banco, mas o cliente recebe 500 (conferi à mão: `POST /reservations` deu 500 e a linha foi
gravada; `POST /reservations/1/confirmation` deu 500 e a reserva ficou `CONFIRMED`, com a mensagem gravada). O roteiro
passou só 6 de 72 conferências. O agente nunca chamou a própria API: o único teste é de unidade no cálculo de preço.
Além disso, nada impede a reserva em duplicidade. Em 10 rodadas de dois pedidos simultâneos, as 10 gravaram duas
reservas do mesmo quarto nas mesmas noites. Os planos de tarifa, que são o pattern pedido, ficaram em `if`/`switch`
sobre a string do plano.

## Arquitetura

**Números:** 6 pacotes, 8 dependências entre eles, 1 ciclo (`com.marealta.api` ↔ `com.marealta.api.handlers`). As
maiores classes são `ReservationService` (306 linhas) e `ReservationHandler` (271). JDBC só em `service` e `database`,
Javalin só na borda (`measures/dependencies.md`).
**Nota:** com problemas

- Uma parte está certa: o Javalin não entra no `service`, e o SQL não entra nos handlers.
- Não existe uma camada de dados. Cada serviço monta e executa o seu SQL junto com a regra.
  - `ReservationService.create` valida, calcula o preço e faz o `INSERT` no mesmo método
    (`service/ReservationService.java:16-61`).
  - `validateReservation` mistura as regras dos planos com a consulta de disponibilidade
    (`service/ReservationService.java:200-237`).
  - `ReportService` monta o CSV e faz as consultas (`service/ReportService.java:46-100`).
- Há regra de negócio nos handlers:
  - a confirmação decide que o valor pago é o total e dispara o e-mail no handler
    (`api/handlers/ReservationHandler.java:88-93`);
  - a regra de quem tem segunda via está no handler (`api/handlers/ReservationHandler.java:205-209`);
  - o handler monta o comprovante, copiado duas vezes (`ReservationHandler.java:169-182` e `213-226`).
- Não há montagem: tudo é `static`. O `Database` cria o pool num inicializador estático
  (`database/Database.java:11-18`) e os serviços chamam `Database.getConnection()` direto, sem nada injetado.
- O ciclo `api` ↔ `api.handlers` é pequeno: `Routes` usa os handlers, e os handlers usam o `api.ErrorResponse`.

Juntando tudo, as camadas existem pela metade: a borda separada das regras, mas regras e dados fundidos.

## Design de baixo nível

**Números:** dos cinco planos, nenhum tem estrutura própria. O comportamento por plano está espalhado em `if`/`switch`
sobre a string `ratePlan`, em 4 métodos de 2 classes:

- preço: `PricingService.java:42`, `55-62` e `71-77`;
- validação: `ReservationService.java:214-230`;
- preço do pacote: `ReservationService.java:256-265`;
- devolução: `ReservationService.java:270-282`.

**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto avisa que "o comercial vive inventando plano", cada um com o seu preço, a sua regra de uso e a
sua devolução. O agente não criou nenhuma estrutura de plano: o plano é uma `String`
(`domain/Reservation.java:15`), e cada comportamento pergunta qual é o plano.

- O desconto é um `if/else if` em `service/PricingService.java:55-62`.
- A taxa do convênio é um `switch` em `PricingService.java:71-77`.
- As regras de uso são três `if` no serviço (`service/ReservationService.java:214-230`).
- O preço do pacote é um `switch` no serviço (`ReservationService.java:256-265`).
- A devolução é outro `switch` (`ReservationService.java:270-282`).

Um sexto plano mexeria em pelo menos quatro lugares de duas classes. É o caso de omissão descrito na referência.

**Exagero.** Não há estrutura sobrando: nenhuma interface, fábrica ou camada que só repassa. Por isso, contenção.

**O resto do código.** Há defeitos espalhados:

- **Duplicação e código morto.**
  - A lista de empresas está em dois lugares: `PricingService.java:72-74` e `ReservationService.java:215-216`.
  - O pacote de fim de semana tem dois cálculos. Um é morto e errado: `PricingService.java:80-95` dá R$ 520,00 para
    qualquer quarto com 2 noites, e o comentário em `:89` admite isso. O outro é o que vale, em
    `ReservationService.java:256-265`.
  - O `SELECT` da reserva está copiado três vezes (`ReservationService.java:64-71`, `147-156` e `174-183`).
  - O comprovante JSON está copiado duas vezes (`ReservationHandler.java:169-182` e `213-226`).
  - Os dois ramos do `if` são idênticos em `service/ReportService.java:74-84`.
  - Nunca são usados: o parâmetro `ratePlan` de `cancel` (`ReservationService.java:125`), o `CURRENCY_FORMAT` em
    `service/ReceiptService.java:9` e o construtor de `domain/Reservation.java:27`.
- **Tipos.**
  - Plano e situação são texto solto, não `enum` (`domain/Reservation.java:15` e `21`).
  - O dinheiro sai no JSON como `double` (`ReservationHandler.java:247-252`), então `0.00` vira `0.0`.
  - A taxa de convênio cai para zero quando o código é desconhecido (`PricingService.java:76`); só a validação em
    outra classe impede isso.
- **Estado.** As classes de domínio têm só campos públicos (`domain/Reservation.java:7-23`), e o sistema inteiro é
  feito de métodos `static`.
- **Erros.**
  - Todo handler tem `catch (Exception)` que vira 400 e esconde a causa (`ReservationHandler.java:53-56`, `71-74`,
    `100-103` etc.).
  - O tratamento global devolve 500 sem corpo e sem log (`api/Routes.java:23-25`). Por isso o erro do Jackson não
    aparece em `measures/behavior-service.log`.
  - Os erros de negócio viajam como texto na mensagem de `IllegalArgumentException`
    (`ReservationService.java:21`, `204-235`) e são decodificados por comparação de strings
    (`ReservationHandler.java:256-270`).
- **Regras erradas pela leitura.**
  - O dinheiro no comprovante sai sem separador de milhar, `R$ 1749,30` em vez de `R$ 1.749,30`
    (`service/ReceiptService.java:43-44`).
  - A segunda via é decidida por `amountPaid == 0` em vez da situação (`ReservationHandler.java:205`). Uma reserva
    paga e cancelada ainda tem comprovante, e uma pendente recebe 404 em vez de 409.
  - O "/5" do relatório de ocupação é fixo (`service/ReportService.java:38`).

## Testes

**Números:** 4 testes rodaram, 0 falharam (4 métodos escritos, 1 arquivo). Cobertura de 40 de 717 linhas (5,6%) e de
20 de 192 ramos (10,4%). `api`, `api.handlers`, `database` e `domain` estão com 0%. Nenhum teste chama a API ou o banco
(`measures/tests.md`).
**Nota:** ruim

O que os testes conferem, com valores:

- a temporada, inclusive o limite de 14/12 (`src/test/java/com/marealta/service/PricingServiceTest.java:11-20`);
- o exemplo do Tecnosul, 779,80 / 93,58 / 686,22 (`:23-36`);
- o exemplo da estadia longa, 1.749,30 / 174,93 / 1.574,37 (`:39-52`);
- uma estadia longa em julho (`:55-72`).

O que fica sem rede, pela pergunta da mutação:

- trocar 0.18 por 0.81 na Agrovale, ou 0.25 na Farmavida (`PricingService.java:73-74`);
- mudar o preço do pacote ou o `0.5` da devolução (`ReservationService.java:257-260` e `277`);
- trocar `nights < 7` por `<= 7` (`ReservationService.java:227`);
- aceitar o pacote de sábado a segunda (`ReservationService.java:248`).

Também não há teste do STANDARD, do NON_REFUNDABLE, do arredondamento meio para o par num caso que o distinga, das
transições de situação, das devoluções, do comprovante, do e-mail ou dos relatórios. E nenhum teste sobe o serviço: por
isso o `mvn verify` passa com uma API em que toda resposta JSON é 500. É quase nada diante do que o sistema faz.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro é `numeric(10,2)`
em todas as 6 colunas e as datas são `date`. A chave estrangeira `reservations.room_id` tem índice. Não há nenhum
CHECK em `status`, `rate_plan`, `type` ou `channel`, e a única restrição de regra é `UNIQUE (rooms.number)`. Há 0
transações explícitas e 0 `FOR UPDATE`, e nenhum SQL montado com `+` (`measures/database.md`).
**Nota:** ruim

**Tipos e chaves.** Estão certos: dinheiro e datas
(`database/SchemaCreator.java:22-43`), e a chave estrangeira com índice composto (`SchemaCreator.java:41` e `56`).
O SQL usa sempre `?`, e as conexões passam pelo pool com try-with-resources.

**A reserva em duplicidade.** É o defeito sério. A disponibilidade é conferida numa consulta
(`service/ReservationService.java:233`), e o `INSERT` acontece depois, em outra conexão, sem transação
(`ReservationService.java:37-55`). Não há restrição de exclusão nem trava. Subi o serviço contra um banco vazio e
mandei 10 rodadas de dois `POST /reservations` simultâneos para o 201 nas mesmas noites. As 10 rodadas gravaram as
duas reservas: 20 linhas, 10 pares sobrepostos. O dono teria o mesmo quarto vendido duas vezes.

**Outras escritas sem proteção.**

- O cancelamento lê a reserva e depois grava `CANCELLED` sem conferir a situação no `UPDATE`
  (`ReservationService.java:126-143`). Um check-in que chegue no meio passa por cima.
- A confirmação e a gravação do e-mail são duas escritas em conexões separadas, sem transação
  (`api/handlers/ReservationHandler.java:89-93`).

**Como o esquema evolui.** Ele é criado no código com `CREATE TABLE IF NOT EXISTS` (`SchemaCreator.java:11`, `22` e
`46`), sem migração versionada, e não saberá acrescentar uma coluna numa tabela que já existe.

**Carga.** O relatório de ocupação faz 2 consultas por quarto, um N+1 (`service/ReportService.java:24` e `29`).

## Comportamento

**Números:** 6 de 72 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 4 de 46 |
| dados depois de reiniciar | 1 de 3 |
| cenário 01 (planos novos) | 0 de 21 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

**A causa principal.** O serviço sobe, mas qualquer resposta JSON vira 500. O corpo da resposta diz "It looks like you
don't have an object mapper configured… add the 'jackson-databind' dependency", e o jar não tem nenhuma classe
`com.fasterxml`. As consequências:

- `GET /rooms`, `POST /reservations` e todos os códigos de erro (400, 404, 409, 422) respondem 500.
- O roteiro não recebe o id da reserva, e por isso a confirmação, o check-in, o check-out, o cancelamento e a segunda
  via falham em cadeia.
- Os relatórios respondem em CSV, mas saem vazios: as reservas do roteiro nunca chegaram a ser confirmadas, porque as
  chamadas dependiam do id.
- Os 21 casos dos planos novos falham pelo mesmo motivo. O cálculo em si confere nos testes de unidade (Tecnosul e
  estadia longa), mas nenhum valor chega ao cliente.
- O "OK" das reservas simultâneas é enganoso: as duas respostas foram 500 em todas as rodadas, e no banco as duas
  reservas foram gravadas (ver Banco).

**Falhas que existiriam mesmo com o Jackson**, pela leitura:

- a segunda via de reserva pendente dá 404 em vez de 409 (`api/handlers/ReservationHandler.java:205-208`);
- `amountPaid` sai como `0.0`, não `0.00` (`ReservationHandler.java:251`);
- o comprovante sem separador de milhar (`service/ReceiptService.java:44`);
- a linha final do financeiro sai `total,,,,599.76` em vez de `total,599.76` (`service/ReportService.java:98`).

Para o dono, o site não conseguiria criar nem consultar nenhuma reserva. As que ele tentasse criar ficariam gravadas
sem que o site soubesse, e um quarto poderia ser vendido duas vezes.

## Dúvidas

- **O formato da linha `total` do financeiro.** O texto diz só "uma linha 'total' com a soma". O agente preencheu as
  colunas vazias (`total,,,,599.76`), e o roteiro espera `total,599.76`. Fica em aberto se isso é erro ou uma leitura
  razoável. Não pesou na nota, porque o resto já decide.
- **A arquitetura entre `com problemas` e `ruim`.** Ficou na fronteira: a borda está separada das regras, mas não
  existe camada de dados e tudo é `static`. Dei `com problemas` porque o Javalin não entra nas regras e o SQL não entra
  nas rotas.
