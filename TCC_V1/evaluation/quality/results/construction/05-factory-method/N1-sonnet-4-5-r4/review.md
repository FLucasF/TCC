# Revisão: construção, cenário 05

## Visão geral

O projeto tem 6 pacotes: `api` (os controladores do Javalin), `api.dto` (os records de entrada e saída), `service`
(`ReservationService`, `ReportService` e as exceções de negócio), `domain` (a `Reservation` com as transições de
estado, o `PricingCalculator`, os enums e a família `DocumentGenerator`), `infrastructure` (JDBC puro: `Database` e
três repositórios) e a raiz (`Application`, que monta tudo e mapeia as exceções para os códigos HTTP). Uma requisição
`POST /reservations` vai do `ReservationController` (que lê o corpo, valida e monta a `GuestIdentification`) para o
`ReservationService` (que confere o quarto, a capacidade, as datas e o conflito), que grava pelo
`ReservationRepository` no PostgreSQL. O esquema é criado por `CREATE TABLE IF NOT EXISTS` dentro do código. O pattern
pedido está bem resolvido: uma fábrica concentrada escolhe o documento pelo tipo de hóspede, e os três lugares passam
por ela. O mais sério está em outro ponto: **nada impede que um quarto receba duas reservas na mesma noite**. Nas 10
rodadas de pedidos simultâneos, as 10 criaram as duas reservas, porque a conferência e o insert não estão numa
transação e não há restrição no banco. Além disso, os documentos em português saem com espaço não separável depois de
"R$" (não é o texto que o dono mostrou), um corpo que não é JSON dá 500, e os testes são quase inexistentes (3 testes,
3,5% das linhas).

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, nenhum ciclo. Javalin só em `api` e na raiz, JDBC só em
`infrastructure`, `domain` e `service` sem Javalin nem JDBC. A maior classe é `ReservationRepository` (260 linhas).
**Nota:** com problemas

As camadas existem e as dependências vão no sentido certo (api → service → infrastructure → domain), sem ciclo. A
montagem fica toda num lugar só (`Application.java:24-39`). As regras de estado ficam na entidade
(`domain/Reservation.java:47-80`), o preço num calculador sem dependências (`domain/PricingCalculator.java`), e o SQL
fica todo nos repositórios.

As misturas são localizadas, mas existem:
- A regra "a segunda via só existe para reserva paga" mora no handler (`api/ReservationController.java:81-83`), e não
  no serviço. Por isso ela também saiu com o código errado (400 em vez de 409).
- O documento do check-out e da segunda via é gerado no mapeamento do DTO (`api/dto/DocumentDto.java:52-53`), e o do
  e-mail é gerado no serviço (`service/ReservationService.java:122-123`). A criação do documento fica dividida entre a
  borda HTTP e a camada de regras.
- `RoomController` e `MessageController` falam direto com os repositórios (`api/RoomController.java:4,17`,
  `api/MessageController.java:4,17`), sem passar por um serviço. Para leituras simples isso é aceitável, mas é a
  origem da dependência `api → infrastructure`.
- As exceções de negócio, que são do domínio, ficam no pacote `service` (`service/InvalidRequestException.java`) e
  são lançadas pelo controlador (`api/ReservationController.java:92,107,123`). Pesa pouco.

## Design de baixo nível

**Números:** 3 tipos de hóspede e 3 documentos; os 3 lugares que emitem o documento (o e-mail em
`ReservationService.java:122`, o check-out e a segunda via pelo `DocumentDto.java:52`) pedem o documento à mesma
fábrica (`DocumentGenerator.java:12-18`). Nenhum lugar cria o documento à mão. Fora da fábrica, ainda há 4 cadeias de
`instanceof` sobre o tipo de hóspede (`DocumentDto.java:38-50`, `ReservationDto.java:39-47`,
`ReservationRepository.java:184-202` e `228-236`) e 5 cópias do `switch` de número do quarto para tipo.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A forma é a de uma fábrica concentrada: `DocumentGenerator.forIdentification` faz um `switch` sobre o
`sealed interface GuestIdentification` (`domain/GuestIdentification.java:3-7`) e devolve um dos três geradores
(`DocumentGenerator.java:13-17`). Os três geradores implementam a mesma interface. Como o `switch` sobre um tipo
selado é exaustivo, quem acrescentar o tipo "agência" é avisado pelo compilador. O e-mail, o check-out e a segunda via
recebem o texto por esse único ponto, e por isso o documento sai igual nos três lugares. É o que a referência chama de
acerto. O que afasta um pouco:
- o título do documento ("Comprovante de reserva", "Fatura de hospedagem", "Booking receipt") é escolhido de novo,
  fora da fábrica, por uma cadeia de `instanceof` (`api/dto/DocumentDto.java:38-50`). O `else` dessa cadeia faz um cast
  direto para `Passport` (`DocumentDto.java:45-46`), e o mesmo vale para `ReservationRepository.java:233-235`. Um quarto
  tipo de hóspede compila sem erro nesses dois lugares e quebra em execução (`ClassCastException`, ou um passaporte
  nulo lido do banco);
- cada gerador faz um cast da identificação que a fábrica já conhecia (`DocumentGenerator.java:27,70,115`). A fábrica
  poderia passar o registro já tipado ao construtor.

**O exagero.** Nenhum: não há interfaces com uma implementação só, nem camadas que só repassam. A única interface é a
do pattern.

**O resto do código.**
- Duplicação: o mapa número do quarto → tipo está escrito à mão em 5 lugares (`DocumentGenerator.java:53-60`,
  `100-107`, `149-156`, `DocumentDto.java:76-83`, `ReportService.java:69-76`). Isso acontece mesmo com `Room` e
  `RoomType` existindo e com o tipo gravado na tabela `rooms`: um quarto novo no banco sairia com o tipo vazio nos
  documentos e no relatório. Os geradores em português também repetem, linha por linha, o corpo do comprovante
  (`DocumentGenerator.java:29-44` e `72-88`), além de `formatNights` e dos formatadores.
- Formatação: `NumberFormat.getCurrencyInstance(pt-BR)` (`DocumentGenerator.java:23,65`) escreve "R$" seguido de um
  espaço não separável (U+00A0), e não de um espaço comum. Conferi o byte: `R$ 302 240`. Além disso, ele está num campo
  `static` compartilhado entre as threads do Jetty, e `NumberFormat` não é seguro entre threads.
- Estado: a `Reservation` protege as transições (`Reservation.java:47-80`), mas expõe `setStatus`, `setAmountPaid` e
  `setRefund` públicos (`Reservation.java:164-182`), o que deixa contornar a máquina de estados. Ao ler do banco, o
  preço é recalculado pela diária (`ReservationRepository.java:238-248` → `Reservation.java:39-41`), e os valores
  gravados de `subtotal`, `discount` e `total` são ignorados.
- Tipos: `BigDecimal` com `HALF_EVEN` em cada passo (`PricingCalculator.java:51-53`), `LocalDate` e enums, tudo
  certo. Mas `amountPaid` começa como `BigDecimal.ZERO`, com escala 0 (`Reservation.java:43`), e sai `0` no JSON em vez
  de `0.00`.
- Erros: `RatePlan.valueOf` sem tratamento (`ReservationController.java:31`) faz um plano inválido virar 500, e o
  `bodyAsClass` sem `try` (`ReservationController.java:24`) faz um corpo que não é JSON virar 500. Os repositórios
  embrulham `SQLException` em `RuntimeException` genérica. A causa não se perde, mas tudo cai no 500 de
  `Application.java:91-95`.

## Testes

**Números:** 3 testes rodaram, 0 falharam, 0 erros, 0 pulados; 3 métodos escritos, num único arquivo. Cobertura de
linhas 28 de 807 (3,5%) e de ramos 11 de 187 (5,9%), toda em `domain`. `api`, `api.dto`, `service` e `infrastructure`
estão em 0%. Não há nenhum teste pela API nem contra o banco.
**Nota:** ruim

Os três testes (`src/test/java/com/pousada/PricingCalculatorTest.java:16-39`) conferem com valores a alta temporada
(só datas dentro dela, nenhuma fora, `:17-21`), o exemplo de 599,76 (`:31`) e o desconto de 89,96 (`:38`). Ficou sem
teste nenhum:
- a parte nova do cenário: os três documentos, o ISS de 5%, o formato em inglês, a escolha da fábrica, a recusa de
  identificação dupla ou incompleta;
- as transições de estado, o cancelamento e a devolução;
- os códigos de erro, o conflito de datas, os relatórios.

A pergunta da mutação: trocar `0.05` por `0.50` no ISS (`DocumentGenerator.java:66`), trocar o `CnpjDocumentGenerator`
por `CpfDocumentGenerator` na fábrica (`DocumentGenerator.java:15`) ou mudar `day >= 15` para `day > 15`
(`PricingCalculator.java:15`) não derrubaria nenhum teste. Só o último chega perto, mas `:18` testa o dia 15 como
dentro e nada testa o 14 como fora. Um teste que conferisse o texto exato do comprovante teria pegado o espaço não
separável. É uma coluna quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. O script acusou: a chave estrangeira `reservations(room_number)` sem índice;
nenhum CHECK em `status`, `rate_plan`, `type` e `channel`; nenhuma restrição UNIQUE/EXCLUDE; 0 transações explícitas;
0 `FOR UPDATE`; nenhum pool (`DriverManager.getConnection` a cada chamada). Nenhum SQL montado com `+`.
**Nota:** ruim

O que está certo: os tipos de dinheiro e de data, o `NOT NULL` no que é obrigatório, a chave estrangeira da reserva
para o quarto, o SQL todo com `?` e as conexões fechadas por try-with-resources (`ReservationRepository.java:29,53,74`).
Os dados sobrevivem ao reinício (3 de 3 no roteiro).

O defeito sério é a regra central da pousada, que não tem proteção nenhuma. `createReservation` consulta os
conflitos (`ReservationService.java:42-47`) e depois insere (`:54`) em duas conexões separadas, em autocommit, sem
transação, sem trava e sem restrição de exclusão no esquema. No roteiro, as 10 rodadas de pedidos simultâneos criaram
as duas reservas. Isso é um quarto vendido duas vezes na mesma noite, um dado errado que o sistema grava sem reclamar.
Não há nem uma trava dentro do processo, que aqui, com um processo só, bastaria para passar.

Os outros defeitos:
- o esquema é criado por `CREATE TABLE IF NOT EXISTS` num texto dentro do código (`infrastructure/Database.java:24-69`),
  sem migrações. O próximo pedido que precisar de uma coluna nova (a agência de turismo, que o texto já anuncia) não
  tem como alterar um banco que já existe;
- cinco colunas de identificação, todas anuláveis, sem CHECK de que exatamente um jeito está preenchido
  (`Database.java:35-39`). Na leitura, uma linha sem nenhuma vira um passaporte nulo (`ReservationRepository.java:233-235`);
- uma conexão nova por consulta, sem pool (`Database.java:19-21`);
- o relatório financeiro filtra com `EXTRACT(YEAR/MONTH FROM check_in)` (`ReservationRepository.java:152-153`), o que
  impede o uso de índice. Pesa pouco neste tamanho.

## Comportamento

**Números:** 74 de 83 conferências passaram. A parte comum, 40 de 46. Os dados depois de reiniciar, 3 de 3. O
cenário 05, 30 de 32. Duas reservas ao mesmo tempo, 1 de 2.
**Nota:** com problemas

O que falhou e o que quer dizer:
- **Duas reservas ao mesmo tempo:** em 10 de 10 rodadas, as duas foram criadas. Quando dois clientes reservam o mesmo
  quarto no mesmo instante pelo site, os dois recebem 201. O defeito é contado na coluna do banco.
- **O "R$" dos documentos em português** (3 falhas: o e-mail com o comprovante, a fatura com o total e a fatura com o
  ISS). Os valores estão certos (779,80, ISS de 38,99), mas o espaço depois de "R$" é um espaço não separável, e não o
  espaço do modelo que o dono mostrou. Um sistema que comparar ou buscar o texto do comprovante não vai achar. Na tela
  parece igual.
- **Corpo que não é JSON:** dá 500 INTERNAL_ERROR em vez de 400 INVALID_REQUEST (`ReservationController.java:24`). Um
  `ratePlan` inválido também dá 500 (conferi à mão; `ReservationController.java:31`).
- **Segunda via de reserva pendente:** dá 400 INVALID_REQUEST em vez de 409 OPERATION_NOT_ALLOWED
  (`ReservationController.java:81-83`). O texto diz que uma operação que a situação não permite é 409.
- **`amountPaid` antes da confirmação:** sai `0` em vez de `0.00` (`Reservation.java:43`). O texto pede 2 casas.
- **"O 201 comporta 3" e "a diária do 201 é 389.90":** não pesam. O sistema devolve `"number":"201"` como texto, e o
  roteiro procura o número 201. Conferi à mão: `{"number":"201","type":"DELUXE","capacity":3,"nightlyRate":389.90}`.
  O texto não diz o tipo do campo, e a escolha é razoável.

O cenário em si funciona: as três formas de identificação, as quatro recusas, a fatura com a empresa e o ISS, o
booking receipt com BRL e datas ISO, "1 night", o e-mail em português com o documento certo, e os cinco campos com
`null` na reserva e no documento. Para o dono, a recepção e o site funcionam no dia a dia. O risco real é o overbooking
quando dois pedidos chegam juntos.

## Dúvidas

- A forma do pattern é a de uma fábrica concentrada (um método estático de escolha na interface `DocumentGenerator`,
  com um `switch` exaustivo sobre um tipo selado), e não o Factory Method clássico com criadores sobrescritos. A
  referência aceita essa forma como acerto. O título do documento escolhido fora da fábrica
  (`DocumentDto.java:38-50`) aproxima o caso do "malfeito", mas o documento em si (o texto) sai sempre da fábrica, e
  por isso ficou acerto.
- Banco `ruim`: a skill põe "a duplicação protegida só dentro do processo" em `com problemas`, mas aqui não há
  proteção nem dentro do processo, e a duplicação aconteceu em 10 de 10 rodadas. Tratei isso como o risco de dado
  errado que a nota `ruim` descreve.
- O `NumberFormat` estático compartilhado entre threads (`DocumentGenerator.java:23,65`) é um risco de texto
  corrompido com carga, mas o roteiro não o provocou.
