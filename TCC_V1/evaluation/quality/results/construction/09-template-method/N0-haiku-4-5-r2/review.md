# Revisão: construção, cenário 09

## Visão geral

O projeto tem 5 pacotes. O pacote raiz `com.pousada` tem `Application`, `Database` (cria o pool e o esquema) e `Api`
(as rotas). Também há `models`, `repositories` (JDBC), `services` (regras e relatórios) e `utils` (preço e datas).
Tudo é feito com métodos `static`. Uma requisição entra em `Api` (`Api.java:50-71`), que lê o JSON com Gson à mão e
chama um serviço estático. O serviço chama um repositório estático, que pega a conexão em `Database.getDataSource()`.
Os dados ficam em três tabelas do PostgreSQL, criadas com `CREATE TABLE IF NOT EXISTS` no código
(`Database.java:35-75`).

Há três defeitos sérios:

1. **O jar não sobe com `java -jar`.** O shade copia para o `app.jar` a assinatura de uma dependência
   (`META-INF/SIGNINGC.SF` e `SIGNINGC.RSA`). A JVM recusa o jar com
   `SecurityException: Invalid signature file digest` (`measures/database-service.log:2`). Por isso os scripts do banco
   e do comportamento não conseguiram medir nada.
2. **O serviço não volta depois de ter dados.** Ao subir, ele roda `DELETE FROM rooms` (`Database.java:105`). Quando
   já existem reservas, a chave estrangeira recusa esse `DELETE`, e o serviço sai com `System.exit(1)`
   (`Application.java:18`).
3. **A confirmação foge do texto.** Ela exige um `amountPaid` no corpo (`Api.java:121-125`) e aceita qualquer valor.
   O texto diz que o valor pago é o total da reserva.

Há outros erros de regra. A alta temporada cobra os 20% em todas as noites quando uma só cai na temporada
(`PriceCalculator.java:30-32`). O cancelamento deixa `refund` como `null` quando não há devolução
(`ReservationService.java:143`). O pattern pedido não foi feito: os quatro relatórios estão escritos inteiros, um a um.

## Arquitetura

**Números:** 5 pacotes, 9 dependências entre eles e 1 ciclo (`com.pousada` → `services` → `repositories` →
`com.pousada`), segundo `measures/dependencies.md`. A maior classe é `Api`, com 306 linhas. O Javalin só aparece em
`com.pousada`, e o JDBC aparece em `com.pousada` e em `repositories`.
**Nota:** com problemas

As camadas existem e, na maior parte, apontam para o lado certo. Os serviços não importam Javalin nem JDBC
(`measures/dependencies.md`, tabela de bibliotecas), e o SQL fica nos repositórios. Há quatro problemas:

- **O ciclo de pacotes.** Ele existe porque os repositórios buscam o pool num `static` global da classe raiz
  (`ReservationRepository.java:3,21` → `Database.java:118`), e a raiz também é a borda HTTP.
- **Não há montagem das peças.** Tudo é `static`: `Api` chama `ReservationService.createReservation` direto
  (`Api.java:99`), e o serviço chama `ReservationRepository.create` direto (`ReservationService.java:57`). Nada pode
  ser trocado nem testado em isolamento.
- **A borda HTTP faz trabalho da regra e dos dados.**
  - `getReservation` e `getDocument` vão direto ao repositório (`Api.java:109`, `Api.java:154`).
  - `getDocument` decide sozinho quais situações dão direito à segunda via (`Api.java:159-163`).
  - A validação dos campos e a conversão do JSON estão no handler (`Api.java:80-97`).
- **As classes de exceção e de envio estão fora do lugar.** `ApiException` carrega o código HTTP e mora em `services`
  (`ApiException.java:3-10`), então a regra fala em status HTTP. O envio do e-mail está em `ReceiptService`
  (`ReceiptService.java:77-89`).

## Design de baixo nível

**Números:** dos 4 relatórios, nenhum passa por uma estrutura comum. Os quatro são métodos estáticos escritos inteiros
em `ReportService` (`ReportService.java:30`, `:52`, `:82`, `:114`). A linha de título está copiada quatro vezes
(`:34`, `:56`, `:86`, `:137`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern (Template Method).** O texto avisa que os relatórios vão crescer e que a última linha com a data "tem que
mudar em todos de uma vez". Mesmo assim, não há esqueleto. Cada método repete a mesma sequência: o título, o cabeçalho
escrito à mão, o laço das linhas e a linha do total (`ReportService.java:33-49`, `55-79`, `85-111`, `136-157`).

- Para pôr a linha de data, seria preciso editar os quatro métodos e lembrar de cada novo relatório.
- O cálculo de "pago menos devolvido" está copiado em `:62-65` e `:126-129`.
- A lista de situações pagas aparece em três lugares: `ReportService.java:160-163`, `Api.java:159-161` e
  `ReservationRepository.java:138`.

Não há classe abstrata, nem escritor genérico, nem funções passadas como parâmetro: é a omissão descrita na referência.

**O exagero.** Não há interfaces de uma implementação só, nem fábricas, nem camadas que só repassam. `MoneyRounder` é
uma função de uma linha (`MoneyRounder.java:7-9`), mas não chega a ser estrutura sem necessidade. Contenção.

**O resto do código.** Os defeitos estão espalhados:

- **Texto solto no lugar de tipos.** A situação, o plano e o tipo do quarto são `String`
  (`Reservation.java:15,20`), comparados em vários lugares (`ReservationService.java:81,101,115,130,137-138`). Um plano
  inválido, como `"X"`, não é recusado em lugar nenhum: `PriceCalculator.java:39` só testa `NON_REFUNDABLE`, e qualquer
  outro texto vira o plano normal.
- **Erros engolidos.** Todos os repositórios fazem `catch (SQLException)`, registram no log e devolvem `null` ou
  `false` (`ReservationRepository.java:49-52`, `:125-128`; `MessageRepository.java:32-35`).
  - Uma falha do banco em `hasConflict` vira "quarto livre".
  - Uma falha em `updateStatus` deixa a resposta dizer que a operação deu certo.
- **`null` sem cuidado.** Um campo enviado como `null` passa pelo `containsKey` (`Api.java:82-85`) e quebra mais à
  frente. No teste manual, um corpo com `"guestName": null` deu 500 `INTERNAL_ERROR`, quando deveria dar 400.
- **Uma regra de preço errada.** `isHighSeasonForAny` aplica os 20% a todas as noites se uma só for de alta temporada
  (`PriceCalculator.java:30-32`, `:50-59`). Uma estada de 29/06 a 02/07 no 201 saiu com subtotal R$ 1.403,64, quando o
  certo é R$ 1.247,68.
- **Campos públicos e mutáveis.** `CreateReservationRequest` (`ReservationService.java:19-28`) e `PriceResult`
  (`PriceCalculator.java:10-14`) têm campos públicos.
- **Formatação de dinheiro duplicada.** O `DecimalFormat` está copiado em `ReceiptService.java:16-26` e em
  `ReportService.java:17-28`.

## Testes

**Números:** 5 testes rodaram, com 0 falhas, 0 erros e 0 pulados. Há 5 métodos escritos, num arquivo só. A cobertura
é de 5,3% das linhas (40 de 756) e 10,8% dos ramos (17 de 158). O único pacote coberto é `utils`, com 40 de 51 linhas;
os pacotes `services`, `repositories` e `com.pousada` têm 0 linhas cobertas (`measures/tests.md`).
**Nota:** ruim

Os testes conferem só o preço e a data, e com valores certos:

- a alta temporada de 10/07 a 12/07 dá R$ 599,76 (`PriceCalculatorTest.java:28-39`);
- o desconto não reembolsável dá R$ 116,97 (`PriceCalculatorTest.java:42-53`);
- a detecção da temporada (`PriceCalculatorTest.java:64-70`).

Não há nenhum teste de:

- a parte nova do cenário: a taxa de turismo, o relatório por plano e a linha de título;
- os relatórios de ocupação e financeiro;
- as situações da reserva, o cancelamento e a devolução;
- o comprovante e o e-mail;
- as recusas e os códigos de erro;
- a API e o banco.

**A pergunta da mutação.** Nenhuma mutação na parte nova seria pega por algum teste: trocar `4.50` por `5.40`
(`ReportService.java:18`), tirar o filtro `isPaidReservation` (`:92`) ou trocar a ordem dos planos (`:143`).
O defeito da estada que cruza o início da temporada também passa sem teste, porque nenhum caso tem noites dos dois
lados.

O teste não sobe o jar, então também não viu que ele não roda com `java -jar`.

## Banco

**Números:** o serviço não subiu contra o banco vazio, e o esquema medido ficou com 0 tabelas (`measures/database.md`,
`measures/database-service.log:2`). Pela leitura de `Database.java:35-75`, seriam 3 tabelas:

- dinheiro em `DECIMAL(10,2)`, datas em `DATE`, chave primária nas três tabelas;
- chave estrangeira `reservations.room_number → rooms`, coberta pelo índice `idx_reservations_room_dates`;
- nenhum `CHECK` e nenhum `UNIQUE` ou `EXCLUDE`;
- 0 transações e 0 `FOR UPDATE`.

O script listou um SQL montado com `+`: `RoomRepository.java:23`. Há outro do mesmo tipo em
`ReservationRepository.java:62`.
**Nota:** ruim

- **O serviço não sobe.** O script não conseguiu medir porque a JVM recusa o `app.jar`, pelo defeito de empacotamento
  descrito na visão geral.
- **Os dados não sobrevivem a um reinício.** Rodei uma cópia do jar sem a assinatura, fora de `projeto/`. Criei uma
  reserva e subi o serviço de novo sobre o mesmo banco. A segunda subida falhou com
  `update or delete on table "rooms" violates foreign key constraint "reservations_room_number_fkey"`, e o processo
  saiu. A causa é o `DELETE FROM rooms` a cada subida (`Database.java:105`), que só funciona com o banco sem reservas.
  Os dados continuam no banco, mas o sistema não volta a atendê-los. É o risco que o texto pedia para evitar ("para
  nada se perder quando o sistema reiniciar").
- **O esquema não evolui.** Ele é criado no código com `CREATE TABLE IF NOT EXISTS`, sem migrações
  (`Database.java:45-85`). Uma coluna nova não chega num banco que já existe.
- **Nada protege contra duas reservas na mesma noite.** Há só um `SELECT` (`ReservationRepository.java:113-129`)
  seguido de um `INSERT` separado (`ReservationService.java:48-70`), sem transação, sem trava e sem restrição
  `EXCLUDE`.
- **A confirmação grava em dois passos.** A situação e o valor pago são duas escritas separadas, sem transação
  (`ReservationService.java:85-86`). A situação e a devolução do cancelamento também (`ReservationService.java:134-145`).
- **Faltam restrições nas colunas.** Não há `CHECK` em `status`, `rate_plan`, `guests` ou `check_out > check_in`
  (`Database.java:58-81`).
- **SQL montado com `+`.** Os dois casos usam um `int` que já passou por `Integer.parseInt`
  (`RoomRepository.java:23`, `ReservationRepository.java:62`), então não há injeção, mas o estilo é frágil.
- **Pontos a favor.** O pool (Hikari) e o try-with-resources estão no lugar, e os tipos de dinheiro e de data estão
  certos.

## Comportamento

**Números:** 0 conferências passaram e 0 falharam: o roteiro `check_09.sh` não pôde testar nada, porque "o serviço não
subiu contra um banco vazio" (`measures/behavior.md`). O log mostra
`java.lang.SecurityException: Invalid signature file digest for Manifest main attributes`
(`measures/behavior-service.log:2`).
**Nota:** ruim

Pelo jeito que o texto pede (`mvn package` e `java -jar`), o dono da pousada não teria sistema nenhum: o jar quebra
antes de chegar ao `main`. O `pom.xml:93-112` usa o shade sem filtrar `META-INF/*.SF` e `META-INF/*.RSA`.

Para saber o que viria depois disso, rodei uma cópia sem a assinatura, fora do roteiro. Esses resultados não entram
nos números acima:

- **Funcionou:** `GET /rooms`; a criação com R$ 599,76 no exemplo de julho; o e-mail de confirmação, no texto pedido;
  o relatório financeiro em `text/csv; charset=utf-8`, com o título.
- **A confirmação sem corpo** devolve 400 `Missing amountPaid field` (`Api.java:121-123`). O site, do jeito que o
  texto descreve, não consegue confirmar nenhuma reserva, e com isso falham também o check-in, o check-out, o
  comprovante e os relatórios de reservas pagas. Com um corpo `{"amountPaid":1}`, a reserva de R$ 599,76 ficou
  confirmada com R$ 1,00 pago.
- **A estada que cruza o início da alta temporada** sai cara demais: R$ 1.403,64 em vez de R$ 1.247,68.
- **O cancelamento de uma reserva pendente** devolve `refund: null`, quando deveria ser 0
  (`ReservationService.java:143-145`).
- **O serviço não volta depois de um reinício**, como descrito na coluna do banco.
- **Pela leitura, os relatórios têm mais dois defeitos:**
  - O financeiro e o relatório por plano contam também as canceladas que não deixaram dinheiro, com 0,00
    (`ReservationRepository.java:166` inclui todo `CANCELLED`, e `ReportService.java:61-75` e `:125-134` não filtram
    valor zero). Com isso, a contagem de reservas por plano fica maior que a real.
  - As linhas de total `total,,,,X` e `total,,,,,X` (`ReportService.java:77`, `:109`) fogem de "a última linha é
    'total' e o valor".

## Dúvidas

- O número de colunas vazias na linha de total dos relatórios financeiro e de taxa de turismo (`total,,,,X` contra
  `total,X`) depende de como o roteiro lê o CSV, e o roteiro não chegou a rodar.
- A falha do jar assinado aparece no JDK desta revisão. É possível que o agente tenha rodado o serviço de outro jeito,
  por exemplo com `mvn exec`, e nunca tenha visto a falha. Mesmo assim, o texto pede expressamente `java -jar`.
