# Revisão: construção, cenário 05

## Visão geral

O projeto tem 8 pacotes: `api` (o `Main`, que monta o Javalin, e o `JsonUtil`), `api.handler` (as rotas),
`service` (as regras e os relatórios), `document` (os três geradores de documento e a fábrica), `repository`
(JDBC à mão), `db` (a conexão e o `CREATE TABLE`), `domain` e `util` (o preço e a formatação). Uma requisição entra
num handler estático (`ReservationHandler.register`), que lê o JSON com `org.json`, valida a identificação, chama o
`ReservationService` (singleton), que usa os repositórios (singletons) e grava no PostgreSQL por `DriverManager`. A
escolha do documento está concentrada em `DocumentGeneratorFactory.create`, e os três lugares que emitem o
documento passam por ela. O defeito mais sério é que **o serviço não sobe**: as rotas usam a sintaxe `:id`
(`ReservationHandler.java:76`), que o Javalin 6 recusa com `IllegalArgumentException` no arranque, então nenhuma
chamada da API funciona. E, mesmo com a rota corrigida, criar uma reserva falharia sempre, porque
`ReservationRepository.save` prepara o `INSERT` e nunca o executa (`ReservationRepository.java:28-60`). Outros
erros de regra aparecem na leitura: a alta temporada cobra dezembro inteiro e não cobra o dia 29 de fevereiro
(`PriceCalculator.java:20-22`), o booking receipt sai sem separador de milhar (`Formatter.java:43`) e as colunas de
CPF e CNPJ não comportam os valores formatados que o texto mostra (`Database.java:52-53`). Os testes cobrem 6% das
linhas.

## Arquitetura

**Números:** 8 pacotes, 19 dependências entre eles e 1 ciclo (`com.pousada.api` ↔ `com.pousada.api.handler`). O
Javalin aparece só em `api` e `api.handler`, e o JDBC só em `db` e `repository`. As maiores classes são
`ReservationRepository` (219 linhas), `ReservationService` (205) e `ReservationHandler` (186).
**Nota:** com problemas

As camadas existem e as dependências vão no sentido certo: a borda chama o serviço, o serviço chama os
repositórios, os repositórios chamam o `db`. O serviço não conhece o Javalin nem o SQL, e todo o SQL está nos
repositórios. Os defeitos:

- **Regra na borda:** a regra de identificação ("exatamente um jeito, e completo") está no handler
  (`api/handler/ReservationHandler.java:17-46`), fora do serviço. O handler também traduz exceções em códigos HTTP
  procurando trechos da mensagem (`ReservationHandler.java:93`, `:169-184`). Assim, o significado do erro depende de
  um texto escrito em outra camada (`ReservationService.java:46`, `:53`, `:58`).
- **Borda que pula o serviço:** `RoomHandler.java:13` e `MessageHandler.java:12` leem o repositório direto. Num
  sistema deste tamanho, isso é aceitável.
- **Montagem espalhada:** não há um lugar que monte as peças. Cada classe pega as suas dependências por singletons
  estáticos (`ReservationService.java:19-22`, `ReportService.java:14-16`, `ReservationRepository.java:17`,
  `Database.java:8`). O `Main` só inicializa o banco e semeia os quartos (`Main.java:28-29`, `:42-54`).
- **O ciclo:** é pequeno e vem de os handlers usarem o `JsonUtil`, que mora no pacote do `Main`
  (`ReservationHandler.java:3`, `Main.java:3-6`).
- **Resto morto:** o `ReservationService` importa `com.pousada.db.Database` sem usar (`ReservationService.java:3`).

## Design de baixo nível

**Números:** 3 tipos de documento (comprovante, fatura, booking receipt), os 3 dentro da estrutura (uma interface
`DocumentGenerator` e três implementações). Há 1 ponto de decisão (`DocumentGeneratorFactory.java:10-19`), e os 3
lugares que emitem o documento pedem a ele (`ReservationService.java:145`, `:185`, `:193`). Nenhum `if` sobre o
tipo de hóspede fora da fábrica, a não ser a validação de entrada no handler.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O agente usou uma fábrica concentrada: `DocumentGeneratorFactory.create(reservation)` escolhe o
gerador pelo campo de identificação preenchido (`DocumentGeneratorFactory.java:11-17`). O e-mail de confirmação
(`ReservationService.java:193`), o check-out (`:145`) e a segunda via (`:185`) usam o documento que ela devolve, sem
perguntar qual é o caso. Um documento de agência seria uma nova implementação de `DocumentGenerator` e um ramo novo
na fábrica. O que afasta um pouco: o tipo de hóspede não é um tipo próprio, são cinco campos anuláveis na
`Reservation` (`domain/Reservation.java:11-15`). Por isso a fábrica decide pela presença de `null`, e um quarto jeito
de identificar exigiria mexer na reserva, no handler, no banco e no JSON. Mesmo assim, a decisão sobre qual documento
criar mora num lugar só, e isso é o que a referência pede.

**Exagero.** Nenhum. A interface com três implementações é a parte do pattern, e fora dela não há camada que só
repasse, nem builder, nem interface com uma implementação só.

**O resto do código.**

- **Erros que quebram o sistema:**
  - `ReservationRepository.java:28-60`: o `INSERT` é preparado e nunca executado, e o código chama
    `getGeneratedKeys()` sem `executeUpdate()`. Além disso, o SQL já tem `RETURNING id` e ainda pede
    `RETURN_GENERATED_KEYS`.
  - `ReservationHandler.java:76` e `:87-151`: a sintaxe `:id` derruba o arranque.
- **Regras erradas:**
  - `PriceCalculator.java:20`: dezembro inteiro conta como alta temporada, e o texto diz "de 15 de dezembro".
  - `PriceCalculator.java:22`: `dayOfMonth <= 28` deixa o 29 de fevereiro de anos bissextos fora da alta.
  - `Formatter.java:43`: o booking receipt sai `BRL 1234.56`, e o texto pede `BRL 1,234.56`. O
    `.replace(",", ".")` não faz sentido nessa linha.
- **Tratamento de erros frágil:**
  - O código usa `IllegalArgumentException` para tudo e decide o código HTTP por `message.contains(...)`
    (`ReservationHandler.java:93`, `:169-184`).
  - O `catch (Exception e)` (`ReservationHandler.java:70-73`) transforma qualquer falha interna, inclusive a falha do
    banco, em 400 `INVALID_REQUEST`, e assim esconde a causa.
- **Tipos:**
  - O dinheiro é `BigDecimal` com `HALF_EVEN` (`PriceCalculator.java:13`), o que está certo.
  - Mas o JSON sai por `doubleValue()` (`JsonUtil.java:56-58`), e 249,90 vira `249.9`, não o número com 2 casas
    pedido.
  - O canal da mensagem é texto solto (`ReservationService.java:202`).
- **Duplicação e tamanho:**
  - Os três geradores repetem a montagem do texto, a regra de "1 diária" e "N diárias" e a linha de desconto
    (`CpfDocumentGenerator.java:33-55`, `CnpjDocumentGenerator.java:34-59`, `PassportDocumentGenerator.java:33-56`).
  - `createReservation` recebe 12 parâmetros (`ReservationService.java:31-44`).
  - Os handlers repetem o mesmo bloco `try/catch` seis vezes (`ReservationHandler.java:87-165`).
- **Código morto:** `calculateSubtotal` e `formatIsoDate` não são usados (`PriceCalculator.java:35`,
  `Formatter.java:18`).
- **Estado:** `static` mutável em `Database.instance` (`Database.java:8`, `:20`).

## Testes

**Números:** 14 testes rodaram, 0 falharam e 0 foram pulados, em 14 métodos e 2 classes (`FormatterTest`,
`PriceCalculatorTest`). A cobertura é de 51 de 830 linhas (6,1%) e de 24 de 212 ramos (11,3%). Os pacotes `api`,
`api.handler`, `db`, `document`, `repository` e `service` estão com 0%. Não há nenhum teste pela API nem contra o
banco.
**Nota:** ruim

Os testes que existem conferem valores, e só do `util`:

- a diária da alta temporada, R$ 299,88 (`PriceCalculatorTest.java:93`);
- o desconto do não reembolsável, 89,96 (`:112`);
- o ISS de 38,99 (`:119`);
- as datas, o real e o CPF e o CNPJ formatados (`FormatterTest.java:14-42`).

Mas a coluna está quase ausente:

- **A parte nova do cenário não tem teste.** Nada testa os três documentos e o texto de cada um, a escolha da
  fábrica, a validação de "exatamente um jeito de identificar", o e-mail, as situações da reserva, o cancelamento, a
  devolução nem os relatórios.
- **Um teste trava o erro.** `FormatterTest.java:32` exige `"BRL 1234.56"`, o contrário do que o texto pede
  (`BRL 1,234.56`).
- **A pergunta da mutação:**
  - Trocar `RETURN_GENERATED_KEYS` ou apagar o `executeUpdate` (que já falta): nenhum teste falha.
  - Trocar o `"0.05"` do ISS (`PriceCalculator.java:54`): falha `testIss`.
  - Mudar a linha "ISS retido" da fatura, a escolha da fábrica ou o "1 night": nenhum teste falha.
  - O teste de dezembro só confere o dia 15 (`PriceCalculatorTest.java:63`), e por isso o erro de 1 a 14 de dezembro
    passa.
- **O que os testes não pegaram:** nenhum teste sobe o serviço ou cria uma reserva, e é por isso que o arranque
  quebrado e o `INSERT` que nunca roda passaram pelo `mvn verify`.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em
`numeric(10,2)` e as datas em `date`. As conferências acusaram:

- a chave estrangeira `reservations(room_number)` sem índice;
- `status`, `rate_plan`, `type` e `channel` sem `CHECK`;
- nenhuma restrição `UNIQUE`, `EXCLUDE` ou `CHECK`;
- nenhum pool, nenhuma transação e nenhum `FOR UPDATE`.

O script registra que **o serviço não subiu contra um banco vazio**. As tabelas foram criadas antes do erro de rota.
**Nota:** ruim

- **Não sobe e não grava.** As tabelas são criadas (`db/Database.java:35-83`), mas o serviço morre logo depois, no
  registro das rotas. E, mesmo de pé, não gravaria nenhuma reserva: `ReservationRepository.save` nunca executa o
  `INSERT` (`ReservationRepository.java:28-60`).
- **Colunas curtas demais.** `guest_cpf VARCHAR(11)` e `company_cnpj VARCHAR(14)` (`Database.java:52-53`) não
  comportam `111.222.333-44` (14 caracteres) nem `12.345.678/0001-90` (18). O handler grava o valor como chega, sem
  tirar a pontuação (`ReservationHandler.java:17-18`, `:58-59`), e o banco recusaria a reserva.
- **O esquema não evolui.** Ele nasce de `CREATE TABLE IF NOT EXISTS` no código (`Database.java:37-79`), sem
  migração versionada, e não saberia acrescentar uma coluna numa tabela que já existe.
- **Nada protege contra reserva dupla.** A conferência de conflito é um `SELECT` seguido de um `INSERT` sem
  transação nem trava (`ReservationService.java:56-59`, `ReservationRepository.java:111-137`), e o banco não tem
  restrição `EXCLUDE`. Duas reservas simultâneas passariam.
- **Conexões e consultas.** Cada chamada abre uma conexão nova por `DriverManager`, sem pool (`Database.java:31-33`).
  O relatório de ocupação faz uma consulta por quarto (`ReportService.java:31-32`). O relatório financeiro usa
  `EXTRACT` sobre `check_in`, o que impede o uso de índice (`ReservationRepository.java:176-177`).
- **O que está certo:** o SQL é todo parametrizado com `?`, e os recursos são fechados por try-with-resources.

## Comportamento

**Números:** 0 conferências passaram e 0 falharam: o roteiro não conseguiu testar nada. O serviço não subiu contra
o banco vazio e caiu com `IllegalArgumentException: Path '/reservations/:id' invalid - Javalin 4 switched from
':param' to '{param}'`, em `ReservationHandler.register` (`ReservationHandler.java:76`).
**Nota:** ruim

Para o dono da pousada, o sistema entregue não funciona: o `java -jar` termina com erro no arranque e o site não
consegue nem listar os quartos. Fora do roteiro, a leitura mostra que, se a rota fosse corrigida, ainda haveria
falhas em chamadas que o texto descreve:

- toda criação de reserva voltaria 400, porque o `INSERT` nunca roda (`ReservationRepository.java:60-68`) e o
  `catch (Exception)` do handler (`ReservationHandler.java:70-73`) transforma a falha em 400;
- uma reserva de 1 a 14 de dezembro sairia 20% mais cara, e uma de 29 de fevereiro de 2028 sairia mais barata
  (`PriceCalculator.java:20-22`);
- o booking receipt sairia `BRL 1234.56` (`Formatter.java:43`);
- os valores no JSON perderiam o zero final, `249.9` (`JsonUtil.java:57`);
- o CPF e o CNPJ formatados como no texto estourariam as colunas do banco (`Database.java:52-53`).

Esses defeitos estão contados nas colunas do banco e do design; aqui, o que decide a nota é que o serviço não sobe.

## Dúvidas

- Não foi possível observar nenhum comportamento pela API, porque o serviço não sobe. As falhas descritas além do
  arranque vêm da leitura do código, não de chamadas.
- O pattern foi resolvido como uma fábrica concentrada (uma classe estática que escolhe o gerador), não como a forma
  clássica com criador sobrescrito. A referência aceita as duas como acerto.
- Não está claro se o site manda o CPF e o CNPJ com ou sem pontuação. Com pontuação, as colunas estouram. Sem
  pontuação, caberiam, e o documento formata o valor (`Formatter.java:46-60`).
