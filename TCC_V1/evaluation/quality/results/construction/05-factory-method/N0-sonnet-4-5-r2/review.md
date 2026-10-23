# Revisão: construção, cenário 05

## Visão geral

O agente montou um projeto Javalin em camadas clássicas: `controller` (os handlers), `service` (preço, situação da
reserva, documento, e-mail, relatórios), `repository` (JDBC puro com HikariCP), `model` (a reserva mutável, enums de
quarto, plano e situação, e um `sealed interface GuestIdentification` com três records), `dto` e `exception`. Uma
requisição de reserva entra em `ReservationController.createReservation` (`ReservationController.java:33`), que valida
e monta a identificação, chama `ReservationService.createReservation` (`ReservationService.java:32`), que confere o
quarto, a capacidade, as datas e a disponibilidade, calcula o preço no `PricingService` e grava pelo
`ReservationRepository`. O esquema é criado com `CREATE TABLE IF NOT EXISTS` em `DatabaseInitializer.java:15-53`. O
documento por tipo de hóspede é escolhido num lugar só (`DocumentService.java:23-29`). O mais sério está fora do
pattern: as colunas `guest_cpf VARCHAR(11)` e `company_cnpj VARCHAR(14)` (`DatabaseInitializer.java:27-28`) não
comportam o CPF e o CNPJ escritos como o texto os mostra (`111.222.333-44`, `12.345.678/0001-90`). A criação da
reserva responde 500, e o roteiro do comportamento quebra quase inteiro (30 de 83). Além disso, nada impede duas
reservas simultâneas do mesmo quarto: em 10 rodadas que rodei à mão, as 10 criaram as duas. Os valores em dinheiro
saem do JSON como `double` (`0.0`, `249.9`), e os testes não tocam a parte nova.

## Arquitetura

**Números:** 8 pacotes, 16 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em
`controller` e na classe de montagem, JDBC só em `repository`, `database` e na montagem. Maior classe:
`ReservationRepository` (243 linhas).
**Nota:** com problemas

As dependências apontam no sentido certo (controller → service → repository → model), sem ciclo, e os serviços não
conhecem nem o Javalin nem o JDBC. A montagem fica num lugar só (`Application.java:26-55`), com injeção por
construtor. Os defeitos estão localizados:

- Há regra e orquestração no handler. `ReservationController.checkOut` e `getDocument`
  (`ReservationController.java:85-104`) buscam o quarto direto no `RoomRepository` e montam o documento chamando o
  `DocumentService`. A regra "a segunda via só existe para reserva paga" mora no controller e é decidida pelo valor
  pago (`ReservationController.java:97`), não pela situação. A confirmação, por outro lado, gera o documento no serviço
  (`ReservationService.java:108-110`), então o mesmo documento é montado em duas camadas.
- Os controllers vão direto ao repositório (`RoomController`, `MessageController`, `ReservationController.java:88,101`).
  Nas leituras simples isso é aceitável; no documento, não.
- A montagem não pode ser reaproveitada. O `main` cria tudo e registra as rotas inline, e o teste teve de copiar as
  70 linhas de ligação e de rotas (`PousadaSystemIT.java:36-96`). Qualquer rota nova precisa ser acrescentada nos dois
  lugares.
- `DatabaseInitializer.initialize` é `static` (`DatabaseInitializer.java:10`), o que pesa pouco.

## Design de baixo nível

**Números:** 3 tipos de hóspede, 3 documentos. Os três lugares que emitem o documento (a confirmação em
`ReservationService.java:109`, o check-out em `ReservationController.java:89` e a segunda via em
`ReservationController.java:102`) chamam o mesmo `DocumentService.generateDocument`. Um segundo `switch` sobre o tipo
de hóspede, para o título, fica fora da fábrica (`DocumentDTO.java:29-33`). Há 3 cadeias de `instanceof` repetidas
para achatar a identificação em cinco campos.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A escolha do documento está concentrada num método-fábrica: `generateDocument` faz um `switch`
exaustivo sobre o `sealed interface GuestIdentification` (`DocumentService.java:23-29`; o tipo está em
`GuestIdentification.java:3-13`). Os três lugares pedem o documento a ele, e nenhum monta o documento à mão. Uma
agência entraria como um quarto record, e o compilador apontaria cada `switch` que falta completar. A referência
aceita essa forma ("uma classe ou um método que escolhe o documento pelo tipo de hóspede"). Três pontos afastam do
ideal:

- A fábrica devolve só a `String` do texto. O título do documento é decidido de novo, fora dela, por outro `switch`
  em `DocumentDTO.java:29-33`, e repete o que já está na primeira linha de cada texto (`DocumentService.java:33,60,86`).
  Uma agência mexeria em dois `switch`.
- `generateComprovante` testa de novo se o hóspede é `IndividualGuest` (`DocumentService.java:36`), embora o `switch`
  já tenha despachado para ele.
- O documento não é um objeto. Os campos do JSON do documento são extraídos da reserva pelo DTO
  (`DocumentDTO.java:26-54`), e não pelo documento criado.

**O exagero.** Não há interface com uma implementação só, nem builder, nem camada que só repasse. O `sealed interface`
com records é o tipo mínimo para a variação.

**O resto do código.**

- **Dinheiro como `double` na borda.** Os DTOs convertem `BigDecimal` em `double` (`ReservationDTO.java:21-25,46-51`,
  `DocumentDTO.java:21-23,49-51`, `RoomDTO.java:9,16`). Por isso o JSON mostra `0.0`, `249.9` e `509.8`, e não 2
  casas. No CSV, `stripTrailingZeros()` (`ReportService.java:74`) faz o mesmo: `509.8`. O cálculo em si é bem feito,
  em `BigDecimal` com `HALF_EVEN` a cada passo (`PricingService.java:13-45`).
- **Duplicação.** `generateComprovante` e `generateFatura` repetem as mesmas 12 linhas (`DocumentService.java:31-56`
  e `58-82`), e o booking receipt repete a mesma estrutura em inglês (`84-105`). A cadeia de `instanceof` que transforma
  a identificação nos cinco campos aparece três vezes (`ReservationDTO.java:36-40`, `DocumentDTO.java:39-43`,
  `ReservationRepository.java:187-191`).
- **Nomes e testes frágeis.** `formatUSD` escreve "BRL" (`DocumentService.java:115-121`). Saber se a reserva foi paga
  depende de `getAmountPaid().doubleValue() == 0` (`ReservationController.java:97`), e não da situação. Por isso uma
  reserva STANDARD confirmada e cancelada continua com segunda via (verifiquei: 200 com o comprovante).
- **Estado.** `Reservation` é um saco de setters (`Reservation.java:28-155`). As transições ficam espalhadas no
  serviço, e o cancelamento enumera as situações proibidas em vez das permitidas (`ReservationService.java:118-120`).
- **Pontos a favor.** Enums com dados para quarto e plano (`RoomType.java:5-25`, `RatePlan.java:5-18`), `LocalDate`
  nas datas e uma hierarquia de exceções com código e status HTTP (`PousadaException.java`).

## Testes

**Números:** 7 testes rodaram, 0 falharam, 0 pulados; 7 métodos escritos, num único arquivo de integração pela API
contra o banco (`measures/tests.md`). Cobertura: linhas 496/829 (59,8%), ramos 73/216 (33,8%); `service` 121/230
linhas e 23/65 ramos.
**Nota:** com problemas

O que está testado, com valores: a lista de quartos (`PousadaSystemIT.java:111-123`), o preço normal e a alta
temporada (`152-156`, `181`, 599.76), o desconto NON_REFUNDABLE com arredondamento (`206-208`, 116.97 e 662.83), a
confirmação com o valor pago e o e-mail (`242-254`), o 409 ROOM_UNAVAILABLE (`308`) e o 422 CAPACITY_EXCEEDED (`333`).

O que fica sem rede:

- **A parte nova do cenário, inteira.** Nenhum teste cria reserva com CNPJ ou passaporte, nenhum confere a fatura, o
  ISS de 5%, o booking receipt, a recusa de duas identificações ou de uma incompleta, nem o campo `text` do documento.
  A pergunta da mutação: trocar `0.05` por `0.50` em `PricingService.java:44`, trocar os casos do `switch` em
  `DocumentService.java:25-27` ou apagar o `if (identificationCount != 1)` em `ReservationController.java:134` não
  derrubaria nenhum teste. O único teste que olha o documento confere só `containsString("Comprovante de reserva")`
  (`PousadaSystemIT.java:254`).
- **O resto do fluxo:** check-in, check-out, cancelamento e devolução, segunda via, os dois relatórios e os 400 de
  validação.
- **O formato do CPF.** Os testes usam o CPF só com dígitos (`PousadaSystemIT.java:132`). Isso escondeu o 500 que o
  CPF escrito como no texto provoca.

O teste também copia a montagem e as rotas do `main` (`PousadaSystemIT.java:36-96`) em vez de subir a aplicação real.
Uma rota mudada em `Application` não seria testada.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)`, ok, e as datas em `date`. Conferências que acusaram algo (`measures/database.md`): a chave estrangeira
`reservations(room_number)` sem índice, nenhum CHECK nas colunas de situação, plano, tipo e canal, nenhuma restrição
(UNIQUE, EXCLUDE ou CHECK), 0 transações explícitas, 0 `FOR UPDATE`. O esquema é criado no código, sem migração.
**Nota:** ruim

- **Nada impede a reserva dupla.** A disponibilidade é uma consulta (`ReservationRepository.java:93-121`) seguida de
  um `INSERT` em outra conexão (`ReservationRepository.java:20-47`), sem transação, sem trava e sem restrição de
  exclusão no banco. Nem um `synchronized` dentro do processo existe. O roteiro não conseguiu medir isso, porque as
  duas chamadas deram 500 pelo CPF. Rodei à mão 10 rodadas de dois pedidos simultâneos para o 301, com CPF só de
  dígitos: **as 10 criaram as duas reservas (201 e 201)**. É dado errado garantido em uso real: o mesmo quarto vendido
  duas vezes.
- **Colunas curtas para o dado do texto.** `guest_cpf VARCHAR(11)` e `company_cnpj VARCHAR(14)`
  (`DatabaseInitializer.java:27-28`) recusam o CPF e o CNPJ formatados, e o erro sobe como 500
  (`measures/behavior-service.log`: "value too long for type character varying(11)" e "(14)"). O efeito pesa no
  comportamento.
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` (`DatabaseInitializer.java:15,22,48`) não acrescenta uma
  coluna numa tabela que já existe. A próxima mudança (a agência) exigiria uma migração que não existe.
- **Faltam restrições.** Nenhum CHECK garante "exatamente uma identificação", situações e planos válidos, `guests >= 1`
  ou `check_out > check_in`. A chave estrangeira não tem índice.
- **O que está certo.** SQL sempre com `?`, conexões em try-with-resources e um pool HikariCP. As confirmações gravam
  primeiro a reserva e depois a mensagem (`ReservationService.java:106-110`), sem transação: se a mensagem falhar, a
  reserva fica confirmada sem e-mail. Isso pesa pouco.

## Comportamento

**Números:** 30 de 83 conferências passaram (`measures/behavior.md`). A parte comum: 13 de 46. Os dados depois de
reiniciar: 1 de 3. O cenário 05: 15 de 32. As reservas simultâneas: 1 de 2.
**Nota:** ruim

- **A reserva com CPF ou CNPJ escrito como o texto mostra dá 500.** É a causa de quase todas as falhas. O roteiro
  manda `111.222.333-44` e `12.345.678/0001-90`, e a gravação estoura as colunas `VARCHAR(11)` e `VARCHAR(14)`. Tudo o
  que depende de uma reserva criada falha em cascata: confirmação, e-mail, check-in, check-out, cancelamentos,
  relatórios e persistência. A fatura também falha inteira. Só o estrangeiro passa (11 de 11 no booking receipt), e as
  recusas de identificação passam (4 de 4). Para o dono da pousada, isso quer dizer que o hóspede brasileiro e a
  empresa não conseguem reservar se digitarem o documento com pontuação. A escolha de guardar só dígitos e formatar na
  saída (`DocumentService.java:123-138`) seria razoável. O defeito é não normalizar a entrada nem recusar com 400.
- **Com CPF e CNPJ só de dígitos, testei à mão.** O preço, a confirmação, o e-mail com o comprovante ou a fatura (ISS
  presente), os relatórios e as recusas funcionam. Restam estes erros:
  - o dinheiro no JSON não tem 2 casas (`"discount":0.0`, `"nightlyRate":249.9`, `"total":509.8`), e o CSV escreve
    `509.8`;
  - um corpo que não é JSON dá 500 em vez de 400 INVALID_REQUEST, porque o `bodyAsClass`
    (`ReservationController.java:34`) cai no `catch` genérico (`Application.java:84-88`);
  - a segunda via de reserva pendente dá 400 INVALID_REQUEST, quando o texto chama de operação que a situação não
    permite (o roteiro espera 409 OPERATION_NOT_ALLOWED);
  - a reserva cancelada continua emitindo segunda via (200), embora o texto diga que ela só existe para reserva paga
    (confirmada, com check-in ou com check-out);
  - o JSON devolve o CPF e o CNPJ só com dígitos, e não como foram enviados.
- **As reservas simultâneas.** O roteiro só viu 500/500 por causa do CPF. À mão, 10 de 10 rodadas criaram as duas
  reservas. O defeito é contado no banco, sem mudar esta nota.
- **"O 201 comporta 3" e "a diária do 201".** O roteiro procura `number == 201` como número, e o sistema devolve
  `"201"` como texto. O texto não fixa o tipo, então essa falha não pesa.

## Dúvidas

- **A classe do pattern.** Dei `acerto`, porque a escolha de qual texto de documento criar está num método só,
  chamado pelos três lugares. O `switch` do título em `DocumentDTO.java:29-33` é uma segunda decisão sobre o tipo de
  hóspede, fora da fábrica, e pode ser lido como `malfeito` ("quem usa a estrutura ainda perguntando qual é o caso").
  A forma usada foi a fábrica concentrada (`switch` sobre o tipo selado), não o Factory Method clássico com subclasses.
- **O formato do CPF e do CNPJ.** O texto mostra o CPF e o CNPJ formatados no documento, mas não diz em que formato
  chegam no pedido. Guardar só dígitos é uma leitura possível. O que não é defensável é o 500 com a forma pontuada.
- **O tipo de `number` do quarto** (texto ou número): o texto deixa em aberto.
- **A nota de testes.** Fica entre `com problemas` e `ruim`: os 7 testes passam e conferem valores na parte comum,
  mas a parte nova do cenário não tem nenhum teste.
