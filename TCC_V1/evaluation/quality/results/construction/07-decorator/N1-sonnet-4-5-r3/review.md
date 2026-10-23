# Revisão: construção, cenário 07

## Visão geral

O projeto foi feito em camadas: `web` (um `WebApplication` com todas as rotas do Javalin e DTOs em `web.dto`),
`service` (`ReservationService`, `MessagingService`, `ReportService`), `domain` (a `Reservation` com o preço, as
transições e a devolução; enums para quarto, plano e situação), `repository` (JDBC puro com HikariCP, um repositório
por tabela) e `messaging` (as regras de envio do cenário, uma lista de `MessageRule` aplicada por um
`MessagePipeline`). Uma requisição entra pela rota, chama o serviço, que usa o domínio e o repositório; tudo é montado
em `config/Configuration.java`. Os dados ficam em três tabelas criadas com `CREATE TABLE IF NOT EXISTS` no código. A
estrutura das regras de envio está certa, mas há dois defeitos sérios no banco: a coluna `guest_cpf` é `VARCHAR(11)`
(`repository/DatabaseConnection.java:47`), então toda reserva com o CPF no formato que o próprio texto usa
("111.222.333-44") falha e volta como 400 INVALID_REQUEST, o que derruba quase todo o roteiro de comportamento (17 de
65); e nada impede duas reservas do mesmo quarto na mesma noite quando chegam juntas (10 de 10 rodadas com as duas
criadas, no meu teste com o CPF sem pontuação).

## Arquitetura

**Números:** 9 pacotes, 18 dependências entre eles, nenhum ciclo. Javalin só em `web`, `config` e no `Main`; JDBC
só em `repository`; `domain`, `service` e `messaging` sem Javalin nem JDBC. Maiores classes: `ReservationRepository`
(237 linhas), `WebApplication` (232), `Reservation` (222).
**Nota:** com problemas

As camadas existem e as dependências apontam do jeito certo (web → service → repository/domain, sem ciclo). As rotas
só leem a requisição, chamam o serviço e escrevem a resposta (`web/WebApplication.java:48-76`); as regras de preço e
de situação ficam no domínio (`domain/Reservation.java:52-153`); o SQL está todo nos repositórios. A montagem é feita
num lugar (`config/Configuration.java:25-52`), inclusive a combinação das regras de envio por modo
(`config/Configuration.java:54-66`).

Os defeitos são localizados:
- o acesso ao banco depende de um `static` global mutável: `DatabaseConnection` guarda o `HikariDataSource` num campo
  estático (`repository/DatabaseConnection.java:12,22`) e cada repositório o busca por chamada estática
  (`repository/ReservationRepository.java:32`, `repository/MessageRepository.java:20`,
  `repository/RoomRepository.java:23`), em vez de receber o `DataSource` na montagem; isso também cria o esquema como
  efeito colateral de `initialize` (`repository/DatabaseConnection.java:23`);
- a borda HTTP fala direto com o repositório para `GET /rooms` (`web/WebApplication.java:22,44`), pulando o serviço;
- as exceções de negócio são classes aninhadas de `ReservationService` (`service/ReservationService.java:117-151`),
  e a validação de campos obrigatórios mora na borda, lançando exceção do serviço
  (`web/WebApplication.java:209-231`), enquanto as transições lançam `IllegalStateException` genérica
  (`domain/Reservation.java:54`) que a borda traduz para 409 (`web/WebApplication.java:99`).

## Design de baixo nível

**Números:** as 3 regras do cenário (endereço inválido, cópia para o arquivo, desvio no modo de teste) estão todas na
estrutura (`messaging/rules/*.java`, uma classe por regra, todas implementando `MessageRule`); nenhuma fica fora; a
combinação por modo é decidida só na montagem (`config/Configuration.java:59-63`). 0 `if` de modo dentro das regras ou
do envio.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Não é um Decorator (não há embrulhos com a interface do envio chamando o próximo), e sim uma lista de
filtros aplicada pelo envio, que a referência aceita: cada regra é uma peça própria
(`messaging/rules/EmailValidationRule.java`, `ArchiveCopyRule.java`, `TestModeRedirectRule.java`) com a mesma
interface (`messaging/MessageRule.java:3-5`), o `MessagePipeline` só percorre a lista
(`messaging/MessagePipeline.java:19-24`), e a combinação por modo fica na montagem
(`config/Configuration.java:54-66`): no teste, validação + desvio; fora, validação + cópia. O `MessagingService` não
pergunta pelo modo (`service/MessagingService.java:21-31`). Uma nova regra (madrugada, limite diário) entra como mais
uma classe e mais uma linha na montagem. O que afasta um pouco: a semântica de "bloqueado" e de "mensagens a mais"
mora no pipeline (`messaging/MessagePipeline.java:21,37`) e depende da ordem da lista (a validação tem de vir antes
das outras, o que só a montagem garante); e uma regra que precise olhar o histórico (o limite por hóspede) não tem
acesso ao repositório por esse contrato. O defeito de regra está dentro da peça: a validação aceita qualquer domínio
que contenha um ponto em qualquer posição (`messaging/rules/EmailValidationRule.java:37`), então `bia@gmail.` e
`ana@.com` passam como válidos — o texto pede "um ponto no meio" (confirmei pela API: `bia@gmail.` saiu como SENT, com
cópia).

**O exagero.** Pouco e pequeno: `MessageContext` guarda um `original` que ninguém lê
(`messaging/MessageContext.java:7,37`) e `ProcessingResult` só embrulha uma lista
(`messaging/MessagePipeline.java:47-57`). Não chega a ser estrutura sem necessidade que pese; contenção.

**O resto do código.**
- Duplicação: o mesmo bloco `try/catch` com `NumberFormatException`, `ReservationNotFoundException` e
  `IllegalStateException` está copiado em seis rotas (`web/WebApplication.java:78-159`), em vez de um
  `app.exception(...)` por tipo.
- Erros engolidos: `catch (Exception e)` na criação da reserva transforma qualquer falha, inclusive erro do banco,
  em 400 INVALID_REQUEST (`web/WebApplication.java:73-75`); foi isso que escondeu o CPF que não cabe na coluna
  ("Invalid request: Failed to insert reservation"). As outras rotas não têm tratamento para erro inesperado.
- Dinheiro: `BigDecimal` com `HALF_EVEN` a cada passo, como pedido (`domain/Reservation.java:108-153`), mas o zero
  sai sem escala (`BigDecimal.ZERO` em `domain/Reservation.java:40,86,137`), e o JSON mostra `"discount":0`,
  `"amountPaid":0`, `"refund":0` em vez de `0.00`; o relatório financeiro vazio mostra `total,0`
  (`service/ReportService.java:49,63`). A formatação do comprovante depende do `Locale` padrão da JVM
  (`domain/ReservationDocument.java:64`): numa máquina em pt_BR, o `String.format("%,.2f")` já sai com vírgula e as
  trocas invertem tudo para "R$ 1,234.56".
- Código morto: `excludeReservationId` sempre `null` (`repository/ReservationRepository.java:122`,
  `service/ReservationService.java:40`); a variável `reservation` não usada no check-out
  (`web/WebApplication.java:135`); o campo `baseNightlyRate` da reserva é lido do banco mas gravado a partir do enum
  (`repository/ReservationRepository.java:78`).
- `guests` como `int` primitivo no pedido (`web/dto/CreateReservationRequest.java:15`): a falta do campo vira 0 e
  só por acaso dá 400; a ordem das validações também faz um quarto inexistente com 0 hóspedes dar 404 antes do 400
  (`service/ReservationService.java:25-30`).
- Os tipos do domínio estão certos (enums para situação, plano, tipo, canal; `LocalDate`), e as transições estão bem
  postas na própria `Reservation` (`domain/Reservation.java:52-82`).

## Testes

**Números:** 15 testes rodaram, 0 falhas, 0 erros, 0 pulados; 15 métodos escritos. Cobertura: linhas 198 de 827
(23,9%), ramos 46 de 166 (27,7%). `domain` 112/183 linhas, `messaging` 54/55, `messaging.rules` 32/35; `service`,
`repository`, `web` e `config` com 0%. Nenhum teste pela API nem contra o banco.
**Nota:** com problemas

O que existe confere valores: o preço com a alta temporada do exemplo do texto (599,76,
`src/test/java/com/pousadamarealta/domain/ReservationTest.java:39-41`), dezembro (`:55`), o desconto de 15% com
arredondamento (116,97 e 662,83, `:69-70`), a devolução no plano normal e no não reembolsável (`:130,150-151`) e
transições inválidas (`:164-170`). As regras de envio têm 6 testes de unidade
(`src/test/java/com/pousadamarealta/messaging/MessagePipelineTest.java`), com o endereço, o assunto e o status.

O que ficou sem rede:
- a validação do endereço só é testada com `ana.gmail.com` (sem @, `MessagePipelineTest.java:195`) e
  `ana@gmail.com`; apagar a conferência do ponto (`EmailValidationRule.java:37`) ou a de "um único @" (`:25`) não
  derruba nenhum teste; `ana@gmail`, `ana@@gmail.com` e `@gmail.com` não aparecem;
- o desvio do modo de teste confere com `contains` (`MessagePipelineTest.java:248`), não que a linha seja a
  primeira;
- a combinação por modo na montagem (`Configuration.createMessagePipeline`) não tem teste: trocar os ramos do `if`
  passaria;
- nada testa a criação pela API, os códigos de erro, os relatórios, o comprovante formatado, o e-mail de
  confirmação nem o banco. O agente escreveu um teste de integração, mas o desligou renomeando-o para
  `src/test/java/com/pousadamarealta/ReservationSystemIT.java.example`, que não compila nem roda; um teste desses
  teria pego o CPF que não cabe na coluna e a reserva duplicada.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`;
datas em `date`; 1 chave estrangeira (`reservations.room_number → rooms`) sem índice; nenhum CHECK nas 6 colunas de
situação/tipo; nenhuma restrição UNIQUE/EXCLUDE; esquema criado por `CREATE TABLE` no código, sem migrações; 0
transações explícitas, 0 `FOR UPDATE`; nenhum SQL montado com `+`.
**Nota:** ruim

O que está certo: SQL sempre com parâmetros, conexões em try-with-resources e pool HikariCP
(`repository/DatabaseConnection.java:15-22`), tipos de dinheiro e de data corretos, `NOT NULL` no obrigatório.

Os defeitos que pesam:
- **A reserva duplicada não é protegida em lugar nenhum.** A disponibilidade é uma consulta seguida de um `INSERT`
  separado, sem transação, trava ou restrição (`service/ReservationService.java:40-50`,
  `repository/ReservationRepository.java:122-153`). Subindo o serviço eu mesmo e mandando dois pedidos iguais ao
  mesmo tempo, com o CPF sem pontuação, as duas reservas foram criadas em 10 de 10 rodadas: dois hóspedes no mesmo
  quarto na mesma noite. É risco de dado errado.
- **`guest_cpf VARCHAR(11)`** (`repository/DatabaseConnection.java:47`): o CPF no formato que o texto mostra
  ("111.222.333-44", 14 caracteres) não cabe, e o `INSERT` falha; o erro vira 400 na borda. Nenhuma validação na
  aplicação explica ou trata esse limite.
- O esquema é criado com `CREATE TABLE IF NOT EXISTS` (`repository/DatabaseConnection.java:30-68`), que não sabe
  evoluir uma tabela que já existe; a chave estrangeira não tem índice; as colunas de situação não têm CHECK.
- O relatório financeiro filtra com `EXTRACT(YEAR/MONTH FROM check_in)` (`repository/ReservationRepository.java:194-195`),
  que não usa índice; pesa pouco neste tamanho.
- A tabela `rooms` guarda `capacity` e `nightly_rate`, mas o código ignora essas colunas e lê do enum
  (`repository/RoomRepository.java:47-48`); a fonte da verdade fica em dois lugares.

## Comportamento

**Números:** 17 de 65 conferências passaram. Parte comum: 14 de 46; dados depois de reiniciar: 1 de 3; cenário no modo
normal: 1 de 9; cenário no modo de teste: 0 de 5; duas reservas ao mesmo tempo: 1 de 2 (as duas recusadas com 400 em
todas as rodadas).
**Nota:** ruim

Quase todas as falhas têm a mesma causa: o roteiro cria as reservas com o CPF "111.222.333-44", como no texto, e a
criação responde 400 `{"error":"INVALID_REQUEST","message":"Invalid request: Failed to insert reservation"}`
(o CPF não cabe em `VARCHAR(11)`). Sem reserva, caem a confirmação, o e-mail, o check-in e o check-out, os
cancelamentos, os relatórios com dados, a persistência e todo o cenário das mensagens. Para o dono da pousada: com o
CPF digitado com pontos e traço, o site não consegue reservar nada. As recusas que não dependem de criar reserva
passaram (422 CAPACITY_EXCEEDED, 404 ROOM_NOT_FOUND, 400 nos campos, 404 RESERVATION_NOT_FOUND), e "a cópia tem o
mesmo texto" passou só porque as duas mensagens vieram vazias.

Para separar o que o CPF esconde, subi o serviço com o CPF sem pontuação: o preço (599,76; desconto 37,48 e total
212,42), as transições, o e-mail com o comprovante, a cópia para o arquivo logo depois da original, o bloqueio de
`ana@gmail` sem cópia, o desvio no modo de teste (para testes@…, "[TESTE] ", primeira linha "Destinatário original:")
e os dois relatórios funcionam. Mesmo assim aparecem outros defeitos que o roteiro não alcançou:
- duas reservas simultâneas do mesmo quarto foram as duas criadas (10 de 10 rodadas): quarto vendido duas vezes;
- `bia@gmail.` é aceito como endereço válido e a mensagem sai, com cópia;
- o dinheiro zero sai como `0` no JSON (`"discount":0`, `"amountPaid":0`, `"refund":0`), não com 2 casas;
- `GET /reports/revenue?month=2027-13` responde 200 com um relatório vazio, em vez de 400.

Duas conferências de `GET /rooms` falharam porque o agente devolve `number` como texto ("201") e o roteiro procura o
número 201; o texto não fixa o tipo, então isso é um ponto em aberto e não pesou.

## Dúvidas

- A classe do pattern: a estrutura é uma lista de regras com contexto imutável, e não embrulhos do envio; dei
  `alternativa equivalente` porque cada regra é uma peça e a combinação é decidida na montagem. A dependência da ordem
  (a validação tem de vir primeiro) e o "bloqueado" interpretado pelo pipeline puxam um pouco para `malfeito`, mas não
  há regra fora da estrutura nem pergunta pelo modo dentro dela.
- Se o CPF deveria ser aceito formatado: o texto não fixa o formato da entrada, mas mostra o CPF formatado e não diz
  para recusá-lo; e o sistema não recusa de propósito, ele quebra no banco e responde um erro genérico. Tratei como
  defeito.
