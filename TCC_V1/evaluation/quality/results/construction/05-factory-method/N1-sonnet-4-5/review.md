# Revisão: construção, cenário 05

## Visão geral

O agente montou um projeto em camadas, em 7 pacotes: `controller` (as rotas do Javalin e os DTOs em `controller/dto`),
`service` (as regras: reserva, preço, documento, e-mail, relatórios), `domain` (a reserva, os quartos e um tipo
fechado `GuestIdentification` com três records, um por jeito de identificar o hóspede), `repository` (JDBC puro,
uma interface e uma classe `Jdbc*` por tabela) e `infrastructure` (o pool Hikari e os `CREATE TABLE`). Uma
requisição entra em `ApiController`, que valida o corpo e monta a identificação, chama `ReservationService`, que
consulta os repositórios, calcula o preço em `PricingService` e, na confirmação, no check-out e na segunda via, pede
o documento a `DocumentGenerator.generate`. Os dados ficam em três tabelas no PostgreSQL. A parte do cenário está bem
resolvida: 32 de 32 conferências do cenário passaram, e o documento é escolhido num lugar só. O que há de mais
sério: nada no banco nem no código impede duas reservas do mesmo quarto nas mesmas noites (o teste de concorrência
criou as duas em 9 de 10 rodadas), e os testes cobrem só 10% das linhas e nada da parte nova.

## Arquitetura

**Números:** 7 pacotes, 13 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só
em `com.pousada.maralta` e `controller`; o JDBC só em `repository` e `infrastructure`. A maior classe é
`JdbcReservationRepository` (259 linhas).
**Nota:** com problemas

As camadas existem e apontam para o lado certo: a borda HTTP só lê a requisição, chama o serviço e escreve a
resposta (`controller/ApiController.java:101-129`), os erros viram códigos HTTP num lugar só
(`controller/ApiController.java:50-61`), as regras não conhecem Javalin nem SQL (`service/ReservationService.java`,
`service/PricingService.java`) e o SQL fica todo em `repository/`. A montagem fica no `main`
(`Application.java:16-42`). Os defeitos são localizados:

- o acesso ao banco usa um `static` global em vez de receber o `DataSource`: `infrastructure/DatabaseConfig.java:12`
  guarda o pool num campo estático, e cada repositório o pega por `DatabaseConfig.getDataSource()`
  (`repository/JdbcReservationRepository.java:33, 62, 79, 105, 134, 162`, e o mesmo nos outros dois). Os
  repositórios não podem ser montados com outro banco (por exemplo, num teste) sem esse estado global;
- `ReservationService` cria as suas dependências por dentro (`service/ReservationService.java:26-28`, `new
  PricingService()`, `new DocumentGenerator()`, `new EmailService()`), então a montagem não fica toda no `main`;
- o controlador fala direto com os repositórios em duas rotas (`controller/ApiController.java:65` e `:138`), passando
  por cima da camada de serviço;
- as exceções de negócio moram aninhadas dentro de `ReservationService` (`service/ReservationService.java:145-149`)
  e a `InvalidRequestException` dentro do controlador (`controller/ApiController.java:222`), e o controlador as
  importa com `import static ...ReservationService.*`.

Nada disso cria ciclo nem mistura HTTP com SQL, mas é o que um revisor pediria para mudar.

## Design de baixo nível

**Números:** o documento é criado num único método, `DocumentGenerator.generate` (`service/DocumentGenerator.java:30-34`),
chamado nos três lugares (`service/ReservationService.java:79` na confirmação/e-mail, `:125` no check-out, `:137` na
segunda via); 3 de 3 tipos de documento ficam dentro dessa estrutura, 0 fora. A decisão sobre o tipo de hóspede
aparece em mais quatro lugares que não criam o documento (resposta JSON, gravação, leitura e leitura do pedido).
**Classe:** acerto
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** O texto pede que a escolha de qual documento criar (comprovante, fatura, *booking receipt*, e depois o
das agências) more num lugar só. O agente fez uma fábrica concentrada: `DocumentGenerator.generate` despacha pelo
tipo de hóspede, um `sealed interface GuestIdentification` (`domain/GuestIdentification.java:3`) com um *visitor*
(`domain/GuestIdentificationVisitor.java`), e os três lugares só pedem o documento a ela. Nenhum dos três monta
documento à mão, e não há `if` sobre o tipo de hóspede no `ReservationService`. Por isso é acerto. O que afasta um
pouco: a escolha é feita duas vezes dentro da fábrica, uma para o texto (`DocumentTextVisitor`,
`service/DocumentGenerator.java:117-195`) e outra para os campos (`DocumentBuilder`, `:36-115`). Um tipo novo, o da
agência, exige um método novo em cada um dos dois visitors, mas o compilador obriga a fazer isso. Por fora da
criação do documento, o tipo de hóspede ainda é decidido em `controller/dto/ReservationResponse.java:33-105` (mais um
visitor), em `repository/JdbcReservationRepository.java:186-204`, numa cadeia de `instanceof` sem `else` final (um
quarto tipo seria gravado com os cinco campos nulos sem nenhum erro), e em `repository/JdbcReservationRepository.java:230-236`,
que lê qualquer linha sem CPF nem CNPJ como passaporte.

**O exagero.** Há três interfaces de repositório com uma implementação só, que nada substitui (nem os testes usam
outra): `repository/ReservationRepository.java`, `RoomRepository.java` e `MessageRepository.java`. E
`controller/dto/DocumentResponse.java:27-47` é uma cópia campo a campo de `domain/ReservationDocument.java`, uma
camada que só repassa os valores (a única diferença é `roomType().name()`).

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em cada passo
(`service/PricingService.java:63`), `LocalDate`, `enum` para situação, plano e tipo de quarto, e a situação da
reserva é controlada dentro de `Reservation` (`domain/Reservation.java:47-80`). Os defeitos:

- duplicação: os três ramos de `ReservationResponse.from` repetem o mesmo construtor de 20 argumentos e mudam só 5
  (`controller/dto/ReservationResponse.java:35-103`); os três ramos de `DocumentBuilder` repetem 13 de 17 argumentos
  (`service/DocumentGenerator.java:48-114`); o comprovante e a fatura repetem as linhas de quarto, datas, diárias,
  desconto e total (`service/DocumentGenerator.java:132-143` e `:154-165`); o arredondamento do ISS é refeito à mão
  (`service/DocumentGenerator.java:167-168`) em vez de usar o de `PricingService`;
- erros: o mesmo `try { reservation.x(); } catch (IllegalStateException) { throw new OperationNotAllowedException(); }`
  aparece quatro vezes (`service/ReservationService.java:70-74, 90-94, 103-107, 116-120`); dois `catch (Exception e)`
  escondem a causa (`controller/ApiController.java:73, 83`); `Long.parseLong(ctx.pathParam("id"))` sem tratamento
  (`controller/ApiController.java:102-132`) faz `GET /reservations/abc` responder 500 (conferi com `curl`);
  `service/ReservationService.java:37` trata `guests < 1` como `CapacityExceededException` (o código errado, que só
  não aparece porque o controlador recusa antes);
- valores em dinheiro: `BigDecimal.ZERO` (escala 0) sai como `0` no JSON (`domain/Reservation.java:43, 78`,
  `service/PricingService.java:37`); o relatório formata o dinheiro com `String.format("%.2f")`, que depende do
  idioma da JVM e pode trocar o ponto por vírgula no CSV (`service/ReportService.java:64, 70`);
- `Reservation` tem um construtor de 16 parâmetros e um `setId` público (`domain/Reservation.java:24, 96`).

São defeitos localizados, sem nenhum que torne o código difícil de mudar: com problemas.

## Testes

**Números:** 10 testes rodaram, 0 falharam, 0 pulados; 10 métodos escritos; cobertura de 82 de 818 linhas (10,0%) e
21 de 172 ramos (12,2%). `controller`, `controller.dto`, `repository` e `infrastructure` têm 0%; `service` tem 25 de
250 linhas (`measures/tests.md`). Nenhum teste chama a API, usa o banco ou usa mocks.
**Nota:** ruim

O que existe confere valores de verdade, mas cobre pouca coisa. `service/PricingServiceTest.java:15-65` confere a
diária na alta temporada (299,88), a soma de 599,76, o desconto "meio para o par" de 89,96 e o total de 509,80.
`integration/ReservationFlowTest.java:14-109`, apesar do pacote "integration", é um teste de unidade da
`Reservation`: confere a sequência de situações e a devolução nos dois planos. Não há teste para:

- **toda a parte nova do cenário:** nenhum teste do `DocumentGenerator` (0 dos três documentos), do ISS de 5%, do
  formato `BRL 1,234.56`, de "1 night", da regra "exatamente uma identificação" (`controller/ApiController.java:194-220`)
  nem do e-mail. Se alguém trocasse `0.05` por `0.50` em `service/DocumentGenerator.java:167`, ou apagasse a linha
  "Discount", ou trocasse `count != 1` por `count > 1`, nenhum teste falharia;
- as bordas da temporada (14 e 15/12, 28/02 e 01/03, 30/06 e 01/07): se trocassem `day >= 15` por `day > 15` em
  `service/PricingService.java:48`, nenhum teste falharia;
- as transições recusadas (confirmar de novo, cancelar depois do check-in), o choque de datas, a capacidade, os
  códigos de erro, os relatórios e a gravação no banco.

O texto não pedia testes, e o agente escreveu alguns, mas eles deixam de fora a parte do cenário inteira e 90% do
código.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`;
datas em `date`; 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE/EXCLUDE; 0 transações explícitas, 0 `FOR UPDATE`; o esquema
é criado por `CREATE TABLE IF NOT EXISTS` no código, sem migração (`measures/database.md`). O SQL é todo com
parâmetros (nenhum candidato montado com `+`).
**Nota:** com problemas

O que está certo: os tipos (`infrastructure/DatabaseConfig.java:67-72`), o pool Hikari, o try-with-resources em
todas as conexões e o SQL com `?`. Os dados continuam depois de reiniciar (3 de 3 no roteiro). Os defeitos:

- **nada impede reservas duplicadas.** O serviço consulta noite por noite (`service/ReservationService.java:43-50`,
  `repository/JdbcReservationRepository.java:97-122`) e depois insere com outra conexão, sem transação, sem trava e
  sem restrição no banco (nem um `EXCLUDE` sobre o intervalo, nem `synchronized`). No roteiro de concorrência, as duas
  reservas simultâneas foram criadas em 9 de 10 rodadas;
- `reservations.room_number` não tem chave estrangeira para `rooms` (`infrastructure/DatabaseConfig.java:54`);
  `status`, `rate_plan`, `type` e `channel` são `varchar` sem CHECK; nada garante no banco que exista exatamente uma
  identificação por reserva (`infrastructure/DatabaseConfig.java:57-61`);
- o esquema não evolui: `CREATE TABLE IF NOT EXISTS` (`infrastructure/DatabaseConfig.java:44-84`) não acrescenta uma
  coluna numa tabela que já existe, e o próximo pedido (o documento das agências, por exemplo) vai precisar disso;
- a confirmação grava a reserva e o e-mail em duas conexões separadas, sem transação
  (`service/ReservationService.java:76` e `:81`): se a segunda gravação falhar, a reserva fica paga sem o e-mail;
- detalhes de carga: o relatório financeiro filtra com `EXTRACT(...)` sobre `check_in`, o que não aproveita índice
  (`repository/JdbcReservationRepository.java:156`), e o de ocupação busca o quarto uma vez por linha
  (`service/ReportService.java:35`). Não há índice além das chaves primárias.

A falta de proteção contra reservas duplicadas é o defeito mais sério e quase leva a nota a `ruim`. Ficou em `com
problemas` porque nenhum dos critérios de `ruim` da coluna se aplica (dinheiro, chave, SQL, persistência estão
certos). Veja as Dúvidas.

## Comportamento

**Números:** 79 de 83 conferências passaram (`measures/behavior.md`): parte comum 43 de 46; dados depois de
reiniciar 3 de 3; cenário 05 32 de 32; reservas simultâneas 1 de 2.
**Nota:** com problemas

A parte do cenário funciona inteira: o comprovante para quem tem CPF, a fatura com a empresa e o ISS de R$ 38,99, o
*booking receipt* em inglês com `BRL 2,783.52`, "1 night" e a linha "Discount", o mesmo documento no e-mail, no
check-out e na segunda via, os cinco campos com `null` e as quatro recusas de identificação inválida. As falhas:

- **as duas reservas ao mesmo tempo:** em 9 de 10 rodadas, dois pedidos simultâneos para o mesmo quarto e as mesmas
  noites foram os dois aceitos. Para o dono da pousada, isso quer dizer o mesmo quarto vendido duas vezes quando dois
  hóspedes reservam pelo site quase no mesmo momento. A causa está no banco (ver acima);
- **dinheiro com 2 casas:** `amountPaid` sai `0` antes da confirmação, e `discount` sai `0` no plano normal (conferi
  com `curl`: `"discount":0,...,"amountPaid":0`, e na pendente cancelada `"refund":0`). O texto pede número com 2
  casas. Um site que exiba o campo como veio pode mostrar "0" em vez de "0,00". É um defeito pequeno, mas real;
- **"o 201 comporta 3" e "a diária do 201 é 389.90":** essas duas falhas não pesam. O sistema devolve `number` como
  texto (`"201"`) e o roteiro procura o número 201. Com `curl`, `GET /rooms` traz `"capacity":3,"nightlyRate":389.90`
  para o 201, e o texto não diz se `number` é número ou texto.

Fora do roteiro, um id que não é número (`GET /reservations/abc`) responde 500 em vez de um erro no formato pedido.
Com o cenário inteiro certo e a parte comum quase toda certa, a nota vem das reservas simultâneas e dos zeros sem
casas decimais.

## Dúvidas

- A forma do pattern: é uma fábrica concentrada (`DocumentGenerator.generate`) com despacho por *visitor* sobre um
  tipo fechado. A escolha fica no tipo de hóspede, mas por *double dispatch*, e não por um método
  `issueDocument(...)` no próprio tipo. Pela referência, as duas formas contam como acerto.
- A nota do banco ficou entre `com problemas` e `ruim`: a duplicação de reservas não é só possível, ela acontece
  (9 de 10 rodadas), e não há proteção nenhuma, nem dentro do processo. Segui a lista da skill, em que `ruim` cobre
  dinheiro, chave, SQL, persistência e migrações e a falta de proteção contra duplicação aparece em `com problemas`.
- O exagero: as interfaces de repositório com uma implementação só são um caso leve. Dei `exagero` porque nada as
  substitui, nem nos testes.
