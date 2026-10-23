# Revisão: construção, cenário 05

## Visão geral

O agente montou um serviço Javalin em camadas: `controller` (as rotas, ligadas em `Application.java:51-64`), `service` (preço, situação da reserva, relatórios, e-mail), `repository` (JDBC puro com HikariCP e SQL com `?`), `domain` (os objetos de dados) e `guesttype` (os três tipos de hóspede e a fábrica deles). Uma requisição entra no controller, que lê o corpo como `Map`, valida e chama o `ReservationService`. O serviço calcula com `PricingService`, grava pelo `ReservationRepository` e pede o documento ao tipo de hóspede (`GuestType.generateDocument`). Os dados ficam em três tabelas que `Database.java:37-95` cria com `CREATE TABLE IF NOT EXISTS`. O cenário pedia um Factory Method, e ele foi resolvido com um método polimórfico no tipo de hóspede. Há três problemas sérios. (1) Nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos: não há restrição, trava nem transação, e eu reproduzi a duplicação em 10 de 10 rodadas. (2) O controller faz cast de `roomNumber` para `String` (`ReservationController.java:26`), e o `catch (Exception)` (`:84`) transforma em 400 qualquer pedido que mande o quarto como número JSON. Por isso o roteiro do comportamento, que manda `201`, não conseguiu criar nenhuma reserva. (3) O dinheiro sai no JSON como `double` (`779.8`, `0.0`), e não com 2 casas.

## Arquitetura

**Números:** 7 pacotes, 15 dependências entre eles, 1 ciclo (`domain` ↔ `guesttype`). As maiores classes são `ReservationRepository` (262 linhas), `ReservationController` (231) e `ReservationService` (201).
**Nota:** com problemas

As camadas existem e apontam para o lado certo:
- Só `controller` e o `main` usam Javalin.
- Só `repository` e `database` usam JDBC.
- `service` não conhece nenhum dos dois (tabela de bibliotecas em `measures/dependencies.md`).
- Toda a montagem acontece num lugar só, `Application.java:25-42`, com injeção por construtor. Não há `new` de repositório dentro de serviço nem `static` global.
- A borda é fina, com validação de formato (datas em `ReservationController.java:49-50`, saída depois da entrada em `:56`), o que é aceitável no HTTP.

Os defeitos:
- **O ciclo `domain` ↔ `guesttype`.** `Reservation` guarda um `GuestType` (`domain/Reservation.java:3`, campo `guestType`), e os `GuestType` criam o `domain.Document` (`guesttype/GuestType.java:11`). Os dois pacotes não se separam: o documento, que é o produto da fábrica, mora no pacote da reserva, e a fábrica mora noutro pacote. O ciclo é pequeno, entre vizinhos, mas é novo, porque o projeto foi construído do zero.
- **A mistura localizada no `ReportService`.** Ele monta o CSV inteiro e consulta o quarto linha a linha (`service/ReportService.java:33`, um N+1 sobre `RoomService`).
- **O controller monta o domínio a partir do `Map` cru.** `ReservationController.java:40` chama `GuestTypeFactory.createFromRequest(body)` com o `Map<String,Object>` do JSON, e assim a fábrica do domínio depende do formato do corpo HTTP (`guesttype/GuestTypeFactory.java:7-12`).

## Design de baixo nível

**Números:** 3 tipos de hóspede, os 3 na estrutura (`IndividualGuest`, `CorporateGuest`, `ForeignGuest`, que implementam `GuestType.generateDocument`). Nenhum fica fora dela. Os 3 lugares que emitem o documento (o e-mail de confirmação, o check-out e a segunda via) pedem o documento ao tipo de hóspede. Não há nenhum `if` ou `switch` sobre o tipo fora de `GuestTypeFactory`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A escolha de qual documento criar mora no próprio tipo de hóspede: `guesttype/IndividualGuest.java:34`, `CorporateGuest.java:38` e `ForeignGuest.java:36` sobrescrevem `generateDocument`. Os três lugares chamam esse método:
- a confirmação, em `service/ReservationService.java:104`;
- o check-out, em `:165`, por `generateDocument` privado (`:178-193`);
- a segunda via, em `:175`.

Quem chama nunca pergunta qual é o caso. O tipo é escolhido uma vez, na criação, por `GuestTypeFactory.createFromRequest` (`GuestTypeFactory.java:7-36`, que também faz a regra "exatamente um jeito"), e na leitura do banco por `createFromDatabase` (`:38-58`). A agência de turismo seria uma classe nova e um ramo na fábrica. A referência aceita isso como acerto (o método polimórfico no tipo de hóspede).

Dois pontos afastam um pouco:
- A confirmação não usa o `generateDocument` privado. Ela repete a chamada com os 10 argumentos à mão (`ReservationService.java:103-115` contra `:178-193`). Não cria o documento sem a estrutura, mas duplica.
- A interface `GuestType` expõe os cinco campos de identificação como getters que devolvem `null` em cada tipo (`GuestType.java:18-22`, `IndividualGuest.java:94-116`). Cada classe nova terá de saber dos campos das outras.

**O exagero.** Não achei estrutura sem necessidade. A interface tem três implementações. A fábrica é usada em dois lugares (o pedido e o banco). `RoomService` (`service/RoomService.java:15-21`) só repassa ao repositório, e é o único caso. Há também código morto: `getGuestIdentificationLine` (`GuestType.java:9`) não é chamado em lugar nenhum, `Database.getDataSource` (`database/Database.java:29`) também não, e o parâmetro `excludeId` de `findConflicting` (`ReservationRepository.java:137`) chega sempre como `null` (`ReservationService.java:52`).

**O resto do código:**
- **`DecimalFormat` estático compartilhado entre as threads do Jetty** (`IndividualGuest.java:15-21`, `CorporateGuest.java:17-24`, `ForeignGuest.java:16-22`). `DecimalFormat` não é thread-safe, então dois comprovantes gerados ao mesmo tempo podem sair com o valor embaralhado.
- **Duplicação entre os documentos.** `IndividualGuest` e `CorporateGuest` repetem o mesmo bloco de formatação em português e as mesmas linhas de quarto, datas, diárias e desconto (`IndividualGuest.java:46-58` contra `CorporateGuest.java:52-66`).
- **Listas longas de parâmetros:** `generateDocument` recebe 10 argumentos, o construtor de `Document` 17 e o de `Reservation` 16.
- **Dinheiro em `double` em dois pontos:**
  - a diária está em `double` no enum (`domain/RoomType.java:6-10`), servida como `double` em `/rooms` (`domain/Room.java:24`, que sai `249.9`);
  - os valores da reserva saem com `BigDecimal.doubleValue()` no JSON (`controller/ReservationController.java:218-219`), o que perde as 2 casas.
- **Erros engolidos.** `catch (Exception e)` devolve 400 genérico (`ReservationController.java:84`, `ReportController.java:37` e `:62`), e foi ele que escondeu o `ClassCastException` de `ReservationController.java:26`. Os casts sem conferência (`(String) body.get(...)`, `:26-32`) tornam o contrato frágil. O mapeamento exceção→HTTP está copiado em 6 handlers (`:76-169`), sem um `app.exception(...)`.
- **Estado mutável.** `Reservation` tem setters públicos de situação e valores (`domain/Reservation.java:118-136`). As regras de transição, porém, estão no próprio objeto (`canConfirm`/`canCancel`/`isPaid`, `:138-158`), e isso é bom.

## Testes

**Números:** 12 testes rodaram, 0 falharam, 0 pulados; 12 métodos escritos em 2 arquivos. Cobertura de linhas de 130 de 902 (14,4%) e de ramos de 22 de 176 (12,5%). Os pacotes `controller`, `repository`, `database` e o `main` estão a 0%, e `service` a 27 de 170 linhas. Nenhum teste pela API nem contra o banco.
**Nota:** ruim

O que existe é bom e confere valores:
- o preço por temporada, incluindo a borda de 14/12 contra 15/12 e o exemplo 599,76 (`PricingServiceTest.java:26-51`);
- o desconto de 15% e o total (`:54-73`);
- os três documentos, com o ISS de R$ 38,99 (`GuestTypeTest.java:60`), "1 diária" e a linha de desconto (`:104-106`).

Mas os testes ficam em duas classes puras, e quase todas as regras do sistema estão sem nenhuma rede:
- as situações da reserva (`Reservation.java:138-158`);
- o cancelamento e a devolução (`ReservationService.java:131-134`);
- o conflito de noites;
- os códigos de erro;
- os dois relatórios;
- o e-mail.

A regra nova "exatamente um jeito de identificar" também não tem teste: trocar `providedFields != 1` por `< 1` em `GuestTypeFactory.java:19` não faz nenhum teste falhar. No *booking receipt* também ficariam sem rede a linha "Discount:", o "1 night" e o separador de milhar.

As asserções dos documentos são `contains` de trechos (`GuestTypeTest.java:33-37`), não o texto inteiro. Uma linha trocada de ordem ou a falta da linha "Total:" na fatura passaria. Os testes também não pegariam o defeito que derruba o roteiro do comportamento, porque nenhum chama a API.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram:
- 1 chave estrangeira sem índice (`reservations.room_number`);
- 4 colunas de situação ou tipo sem CHECK;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- 0 transações explícitas e 0 `FOR UPDATE`.

O esquema é criado no código, sem migração.
**Nota:** ruim

Os tipos estão certos:
- dinheiro em `DECIMAL(10, 2)` (`database/Database.java:63-68`);
- a reserva ligada ao quarto por FK (`:50`);
- `NOT NULL` no que é obrigatório;
- o SQL usa sempre parâmetros. A única concatenação é constante (`ReservationRepository.java:150`).
- O pool é HikariCP, e as conexões ficam em try-with-resources.

O defeito sério é que a regra central do dono não tem proteção nenhuma: "um quarto não pode ter duas reservas na mesma noite". `ReservationService.java:52` consulta os conflitos e só depois grava (`:81`), em conexões separadas, sem transação, sem trava e sem uma restrição `EXCLUDE` no banco. Não há sequer um `synchronized` no processo. Com o serviço de pé, mandei dois pedidos simultâneos para o 102 nas mesmas noites (com `roomNumber` como texto), em 10 rodadas. Todas as 20 reservas foram criadas (`201 201` em cada rodada), e o banco ficou com duas reservas para cada uma das 10 datas. Isso é dado errado gravado.

Os outros defeitos:
- **O esquema não evolui.** É `CREATE TABLE IF NOT EXISTS` no código (`Database.java:41`, `:48`, `:73`), que não acrescenta coluna numa tabela que já existe.
- **O índice que falta** é o de `reservations.room_number`, que é exatamente a coluna da consulta de conflito.
- **Não há CHECK** em `status`, `rate_plan` nem na regra de "um e só um" dos cinco campos de identificação (`Database.java:53-57`).
- **A confirmação não é atômica.** Ela grava a reserva (`ReservationService.java:101`) e o e-mail (`:117`) em operações separadas. Se o segundo falhar, a reserva fica confirmada sem o e-mail.

## Comportamento

**Números:** 18 de 83 conferências passaram no roteiro (`measures/behavior.md`):

| parte do roteiro | passaram |
|---|---|
| parte comum | 12 de 46 |
| depois de reiniciar | 1 de 3 |
| cenário 05 | 4 de 32 |
| reservas simultâneas | 1 de 2 |

**Nota:** com problemas

Quase todas as falhas têm uma causa só. O roteiro manda `"roomNumber": 201` (número JSON), e o controller faz `(String) body.get("roomNumber")` (`ReservationController.java:26`). O `ClassCastException` cai no `catch (Exception)` (`:84`) e vira `400 INVALID_REQUEST`, então nenhuma reserva foi criada. Pelo mesmo motivo, `/rooms` devolve `"number":"201"` como texto, e o roteiro não achou o quarto 201 para conferir a capacidade e a diária. As falhas de confirmação, check-in, check-out, cancelamento, relatórios, reinício e documentos são todas consequência disso: elas receberam 404 ou vazio por não haver reserva.

O texto não fixa o tipo JSON do número do quarto, e a API do agente é coerente consigo mesma (texto em `/rooms` e texto no pedido). Por isso refiz o roteiro à mão com `"roomNumber":"201"`, e quase tudo funciona:
- O preço sai certo: 599,76, desconto 37,48, total 212,42 e 2.783,52 em julho.
- Os erros 404, 409 e 422 estão certos.
- O e-mail sai em português com o documento certo para cada tipo: comprovante, fatura com ISS de R$ 38,99 e *booking receipt* com `BRL 2,783.52`, "Discount:" e as datas ISO.
- Os cinco campos de identificação aparecem com `null` nos que não se aplicam, e o documento tem o campo `text`.
- A segunda via de reserva pendente dá 409.
- Os relatórios estão certos, inclusive a NON_REFUNDABLE cancelada que continua contando, e o `total,1/5`.
- Os dados continuam depois de reiniciar.

O que falha de verdade:
1. **O dinheiro não sai com 2 casas.** O JSON traz `"total":779.8`, `"discount":0.0` e `"amountPaid":0.0`, contra o "número com 2 casas (599.76)" que o texto pede. A causa é `ReservationController.java:218-219`.
2. **As reservas simultâneas viram duas.** Isso pesa na coluna do banco.
3. **O contrato é frágil.** Um site que mande o quarto como número recebe 400 em toda reserva, sem nenhuma pista do motivo ("Invalid request").

Para o dono da pousada, isso quer dizer o seguinte. Se o site mandar o número do quarto como texto, o sistema calcula e emite os documentos certos. Se mandar como número, nenhuma reserva entra. E dois hóspedes que reservem o mesmo quarto ao mesmo tempo podem ficar os dois com a reserva.

## Dúvidas

- **O comportamento.** A nota pesa a minha leitura de que o tipo do `roomNumber` era um ponto aberto do texto. Se o tipo for considerado parte do contrato (o quarto como número), a parte comum e o cenário quebram, e a nota seria `ruim`. Os 18 de 83 do roteiro sustentam essa leitura mais dura.
- **A forma do pattern.** O agente usou o método polimórfico no tipo de hóspede (`guest.generateDocument(...)`), e não um criador clássico com subclasses. A referência aceita essa forma como acerto.
- **O banco.** Dei `ruim` porque a duplicação de reserva não tem proteção nenhuma, nem no processo, e porque eu a reproduzi gravando dado errado. Fora isso, o esquema seria `com problemas` (sem migração, FK sem índice, sem CHECK).
- **O exagero.** Contei `RoomService`, uma camada que só repassa, como pequena demais para virar `exagero`.
