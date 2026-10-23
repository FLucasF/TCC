# Revisão: construção, cenário 05

## Visão geral

O projeto tem 25 classes num pacote só (`com.pousadamarealta`, 1373 linhas). Uma requisição entra num "controller" que registra as próprias rotas no Javalin. As de reserva chamam o `ReservationService`, que guarda as regras e o SQL juntos. As de relatório e de mensagens fazem o SQL ali mesmo, dentro da classe de rotas (`ReportController.java:84`, `MessageController.java:37`). Os dados ficam em duas tabelas (`reservations` e `messages`) criadas com `CREATE TABLE IF NOT EXISTS` no código (`Database.java:40`). Os quartos não estão no banco: o catálogo está copiado em quatro `switch`/listas.

A escolha do documento fica num lugar só (`DocumentService.generateDocument`), e isso é o melhor do projeto. O mais sério está no resto:
- A confirmação exige um corpo com `amountPaid` que o cliente inventa (`ReservationController.java:86`, `ReservationService.java:115`). Sem corpo, ela responde 400. Com `{}`, a reserva fica CONFIRMED com R$ 0,00 pago.
- Dá para cancelar uma reserva que já fez check-in (`ReservationService.java:128`).
- O relatório de ocupação conta reservas pendentes (`ReportController.java:88-89`).
- Duas reservas simultâneas para o mesmo quarto passam nas 10 rodadas.
- O dinheiro é `double` em todo o código.
- Os únicos testes são 7, só do preço, com 5,8% de cobertura de linhas.

## Arquitetura

**Números:** 1 pacote, 25 classes, 1373 linhas; 0 dependências entre pacotes, 0 ciclos (num pacote só, não há o que medir). O mesmo pacote usa Javalin, JDBC e Jackson. As maiores classes: `ReservationService` (268), `ReservationController` (219), `DocumentService` (212), `ReportController` (203).
**Nota:** ruim

- **As camadas quase não existem.** Tudo está num pacote. Não há camada de acesso a dados: o SQL está no serviço de reservas (`ReservationService.java:49-84`, `111-120`, `222-242`, `257-267`) e dentro das classes de rotas.
- **Relatórios e mensagens fazem tudo na mesma classe.** No `ReportController`, a classe de rotas lê a query string, monta o SQL (`ReportController.java:84-113`, `134-172`), aplica a regra de negócio (quem conta, `ReportController.java:88-89` e `141`; a conta do líquido, `ReportController.java:162`) e escreve o CSV. O `MessageController` também faz o SQL dentro da rota (`MessageController.java:37`).
- **Regras dentro dos handlers.** A regra de quando a segunda via existe está no handler (`ReservationController.java:140-143`), e as validações de entrada também (`ReservationController.java:155-190`).
- **A montagem fica pela metade.** O `Main` cria o `Database` e os controllers (`Main.java:13-27`), mas o controller cria o próprio serviço com `new` (`ReservationController.java:13`).
- **Funções estáticas com regra.** `DocumentService` e `PricingService` são só funções estáticas chamadas direto de qualquer camada (`ReservationController.java:126`, `ReservationService.java:246`).
- **O catálogo de quartos aparece em quatro lugares:**
  - `ReservationService.java:213`;
  - `ReservationController.java:192`;
  - `ReportController.java:179`, mais o número fixo de quartos em `ReportController.java:80`;
  - `RoomController.java:14`.

  Mudar um quarto obriga a mexer em todos eles.

## Design de baixo nível

**Números:** 3 tipos de documento, os 3 numa função de fábrica só (`DocumentService.java:10-20`). Os 3 lugares que emitem o documento chamam essa função: o e-mail (`ReservationService.java:246`), o check-out (`ReservationController.java:126`) e a segunda via (`ReservationController.java:146`). Nenhum lugar decide fora dela.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** É uma fábrica concentrada: `generateDocument` decide pelo dado de identificação da reserva (CPF, CNPJ ou passaporte) e devolve o `Document` com o título, os campos e o texto de cada tipo (`DocumentService.java:22-133`).
- O que aproxima: os três lugares pedem o documento à fábrica e não perguntam o tipo. Uma agência de turismo seria um ramo a mais nessa função e um gerador a mais.
- O que afasta:
  - a decisão é por `null` em campos soltos da reserva (`DocumentService.java:11-15`), não por um tipo de hóspede;
  - a validação de "exatamente um jeito" fica repetida em outro lugar (`ReservationController.java:155-172`);
  - os três `format*` repetem quase linha a linha a mesma montagem (`DocumentService.java:135-189`).

  Mesmo assim, a escolha de qual documento criar mora num lugar só.

**O exagero.** Nenhuma interface, fábrica ou camada sem necessidade.

**O resto do código** tem defeitos espalhados:
- **Dinheiro em `double`** do começo ao fim:
  - `RoomType.java:10`, `Reservation.java:22-27`, `PricingService.java:10-62` e `ReportController.java:132`/`162`;
  - o arredondamento faz `new BigDecimal(double)` (`PricingService.java:62`), então o "meio para o par" depende da representação binária;
  - o valor vai ao banco por `new BigDecimal(double)` (`ReservationService.java:71-73`, `115`, `144`).
- **Erros escondidos:**
  - todo handler tem `catch (Exception)` que troca qualquer causa por 404 RESERVATION_NOT_FOUND (`ReservationController.java:78-80`, `104-106`, `116-118`, `130-132`, `150-152`), por 400 (`ReservationController.java:63-67`) ou por 500 INTERNAL_ERROR (`ReportController.java:40-46`, `66-72`);
  - por isso uma data inválida no relatório vira 500;
  - um `roomNumber` ausente vira 0 e responde ROOM_NOT_FOUND (`ReservationController.java:202`).
- **Duplicação:**
  - o catálogo de quartos em quatro lugares (ver Arquitetura);
  - as respostas de erro montadas à mão com `HashMap` anônimo (`ReportController.java:28`, `MessageController.java:25`) em vez do `ApiException`.
- **Listas de parâmetros longas:** 12 parâmetros em `createReservation` (`ReservationService.java:21-25`), 17 no construtor de `Document` (`Document.java:25-29`) e 20 no de `Reservation`.
- **Regra de domínio errada no lugar errado:**
  - o valor pago vem do cliente (`ReservationController.java:217`);
  - "refund" fica `null` quando a devolução é zero (`ReservationService.java:144`);
  - o cancelamento é decidido por lista de exclusão, e por isso aceita CHECKED_IN (`ReservationService.java:128-131`).

## Testes

**Números:** 7 testes rodaram, 0 falharam (7 métodos escritos, num arquivo só: `PricingServiceTest`). Cobertura: 38 de 653 linhas (5,8%) e 11 de 150 ramos (7,3%). Nenhum teste pela API nem contra o banco. O Testcontainers está no `pom.xml`, mas nenhum teste o usa.
**Nota:** ruim

Os testes conferem só o preço, com valores:
- a diária baixa e a alta (`PricingServiceTest.java:11-20`);
- 599,76 (`PricingServiceTest.java:27`);
- o desconto de 89,96 (`PricingServiceTest.java:33`).

O teste de arredondamento usa 299,885 (`PricingServiceTest.java:44`). Esse `double` já é 299,8849…, então ele não confere o "meio para o par".

Nada confere a parte nova do cenário:
- os três documentos;
- o ISS de 5%;
- "1 night"/"N nights" e o formato BRL;
- a regra de exatamente um jeito de identificar.

Pela pergunta da mutação: trocar `0.05` por `0.5` em `DocumentService.java:59`, apagar a linha do desconto (`DocumentService.java:146-148`) ou mudar `identificationCount != 1` para `> 1` (`ReservationController.java:161`) não derrubaria nenhum teste. As situações da reserva, o cancelamento com devolução, os relatórios e os códigos de erro também ficam sem teste. Por isso nenhum teste pegou o cancelamento depois do check-in, nem a confirmação que exige `amountPaid`.

## Banco

**Números:** 2 tabelas (`messages`, `reservations`), as duas com chave primária. O dinheiro está em `numeric(10,2)` (ok) e as datas em `date`. Não há tabela de quartos, nem chave estrangeira, nem CHECK, UNIQUE ou EXCLUDE. Status, plano e canal são `varchar` sem CHECK. O código não tem pool (`DriverManager.getConnection` a cada chamada), não tem transação explícita nem `FOR UPDATE`, e não monta SQL com `+`.
**Nota:** ruim

- **O esquema não evolui.** Ele nasce de `CREATE TABLE IF NOT EXISTS` em `Database.java:44-78`, sem migração; a próxima coluna não chega a um banco que já existe.
- **Nada protege contra duas reservas na mesma noite.** A disponibilidade é um `SELECT COUNT(*)` numa conexão (`ReservationService.java:222-242`) e o `INSERT` sai em outra (`ReservationService.java:49-77`), sem transação, trava nem restrição. No roteiro, as 10 rodadas simultâneas criaram as duas reservas: dado errado no banco.
- **A confirmação não é atômica.** O `UPDATE` da reserva e o `INSERT` da mensagem saem em conexões diferentes (`ReservationService.java:111-119`, `257-266`); uma falha no meio deixa a reserva confirmada sem e-mail.
- **O dinheiro passa por `double`** entre o código e a coluna (`ReservationService.java:71-73`, `204-209`).
- **`PreparedStatement` e `ResultSet` não são fechados.** Só a conexão está no try-with-resources (`ReservationService.java:50`, `79`, `95`; `Database.java:41`).
- **O que está certo:** o SQL é todo com parâmetros (`?`), e o filtro de ocupação e de mês é feito no banco.

## Comportamento

**Números:** 39 de 83 conferências passaram (44 falharam):
- a parte comum: 26 de 46;
- depois de reiniciar: 1 de 3;
- o cenário 05: 11 de 32;
- duas reservas ao mesmo tempo: 1 de 2 (as duas criadas em 10 de 10 rodadas).

**Nota:** ruim

A parte comum quebra em vários pontos:

- **A confirmação.** O site chama `POST /reservations/{id}/confirmation` como o texto descreve, sem corpo, e recebe 400 INVALID_REQUEST. O sistema exige `{"amountPaid": ...}`, enquanto o texto diz que o pago é o total da reserva. Com corpo `{}`, testei à mão e a reserva fica CONFIRMED com `amountPaid` 0.0: uma reserva "paga" sem nada pago. Em cadeia, falham o e-mail, a segunda via, o check-in e o check-out do roteiro. Quase todo o cenário 05 falha por isso: 21 das 32 conferências.
- **O conteúdo dos documentos está certo, mas o JSON não.** Testei à mão com corpo na confirmação: a fatura sai com "ISS retido (5%): R$ 38,99" e o booking receipt com "BRL", as datas 2027-03-10 e "1 night". Mas o JSON do documento:
  - omite os campos que não se aplicam, em vez de mandá-los como `null` (`@JsonInclude(NON_NULL)`, `Document.java:5`);
  - omite o desconto quando ele é zero (`DocumentService.java:52`);
  - traz o dinheiro como texto ("R$ 779,80", `Document.java:20-22`) e as datas como "10/03/2027".
- **O formato do JSON da reserva.** As datas saem como `[2027,3,10]`, não como 2027-07-10, e o dinheiro sai como `779.8`/`0.0`, não com 2 casas.
- **Cancelar depois do check-in responde 200 e devolve o pago** (`ReservationService.java:128`). O texto proíbe isso. Este erro também derruba 2 das 3 conferências de "depois de reiniciar": a reserva que devia estar CHECKED_OUT ficou CANCELLED, e o quarto ficou livre. As reservas e as mensagens persistem.
- **A devolução zero vem `null`, não 0.00** (`ReservationService.java:144`), no não reembolsável e na pendente.
- **A ocupação conta reservas pendentes.** No teste à mão, uma pendente apareceu no relatório e no `total,3/5`.
- **O financeiro lista cancelamentos que não deixaram dinheiro.** Aparecem linhas com 0.00 da pendente cancelada e da STANDARD devolvida.
- **Mês fora do formato responde 500**, não 400.
- **Duas reservas simultâneas** para o mesmo quarto são criadas.

Para o dono da pousada, isso quer dizer: o site não consegue confirmar uma reserva como foi combinado, a recepção cancela (e devolve dinheiro) de quem já está hospedado, o relatório de ocupação mostra quartos que ninguém pagou, e o mesmo quarto pode ser vendido duas vezes.

O que funciona:
- os quartos e os preços com temporada e desconto (599,76; 37,48; 212,42);
- as recusas na criação (409, 422, 404, 400);
- a regra de exatamente um jeito de identificar (as 4 recusas passam).

## Dúvidas

- A forma do pattern é uma fábrica concentrada: uma função estática que decide por qual campo de identificação está preenchido, não um tipo de hóspede com método polimórfico. A referência aceita essa forma como acerto. A dúvida é se decidir por `null` em campos soltos é concentração bastante; a evidência (os três lugares chamam a função e nenhum decide sozinho) sustenta o acerto.
- Exigir o `amountPaid` no corpo da confirmação pode ter sido uma leitura do agente: o texto não descreve corpo nessa chamada. Mesmo assim, contraria "o que ele pagou é o total da reserva" e deixa confirmar com 0. Por isso contei como erro.
