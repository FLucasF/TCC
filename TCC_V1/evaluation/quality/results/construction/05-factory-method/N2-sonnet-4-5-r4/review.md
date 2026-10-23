# Revisão: construção, cenário 05

## Visão geral

O agente montou um serviço Javalin em seis pacotes: `web` (um `ApiController` com todas as rotas e o mapeamento das
exceções), `service` (`ReservationService` com as transições e o e-mail; `ReportService` com os dois CSV), `domain`
(a `Reservation`, que calcula o preço e guarda a situação, e a `GuestIdentification` selada com três records),
`document` (a escolha e a montagem do documento), `repository` (JDBC puro com HikariCP, que também cria as tabelas
com `CREATE TABLE IF NOT EXISTS`) e o `App`, que liga tudo. Uma requisição entra no `ApiController`, que lê o JSON e
monta a identificação (`web/ApiController.java:187-208`), chama o `ReservationService`, que valida, consulta
conflitos e grava pelo `ReservationRepository`. A escolha do documento está num lugar só
(`document/DocumentService.java:17-25`) e os três lugares (e-mail, check-out, segunda via) passam por ela. O mais
sério: nada impede duas reservas do mesmo quarto feitas ao mesmo tempo. A consulta de conflito e o `INSERT` não estão
numa transação e não há restrição no banco, e o roteiro criou as duas reservas nas 10 rodadas. Também há defeitos
menores: o campo `title` do documento sai sempre `null` (`document/Document.java:43`), o "R$" é seguido de um espaço
não separável (U+00A0) em vez de um espaço comum, e um corpo que não é JSON dá 500.

## Arquitetura

**Números:** 6 pacotes, 13 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece
em `web` e no `App`; o JDBC, só em `repository` e no `App`. A maior classe é `ApiController` (301 linhas).
**Nota:** bom

As dependências seguem a direção certa: `web → service → repository → domain`, e `document` depende só de `domain`.
As regras (preço, temporada, transições, devolução) estão em `domain/Reservation.java:70-137`, sem Javalin e sem SQL.
O SQL fica todo nos três repositórios, com `try-with-resources` e o pool. Tudo é montado num lugar só, em
`App.java:20-48`, com injeção pelo construtor e sem `static` mutável. Os handlers são finos: leem, chamam o serviço e
escrevem a resposta (`web/ApiController.java:108-142`).

Há duas misturas pequenas, que não mudam a nota. O controller fala direto com os repositórios em `GET /rooms` e
`GET /messages` (`web/ApiController.java:24-25,74,145`), sem passar pelo serviço. E a regra "exatamente uma
identificação" mora na borda HTTP (`web/ApiController.java:187-208`), não no serviço. As exceções de negócio são
classes aninhadas no `ReservationService` (`service/ReservationService.java:143-152`), e há uma segunda
`InvalidRequestException` no controller (`web/ApiController.java:296`).

## Design de baixo nível

**Números:** os três tipos de documento estão na estrutura: a escolha está num único método
(`DocumentService.selectGenerator`, `document/DocumentService.java:17-25`), e 3 de 3 lugares de emissão passam por
ela, via `ReservationService.generateDocument` (`service/ReservationService.java:124-127`), chamado na confirmação
(`:130`), no check-out (`:110`) e na segunda via (`:121`). Nenhum lugar cria o documento à mão. O código tem mais 4
cadeias de `instanceof` sobre o tipo de hóspede, que não escolhem documento: copiam os cinco campos.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É a forma de "fábrica concentrada" que a referência aceita: o `DocumentService` escolhe uma subclasse
de `DocumentGenerator` pelo tipo de hóspede, e quem precisa do documento só pede `generateDocument`
(`document/DocumentService.java:12-15`). Cada documento é uma subclasse que sobrescreve os ganchos de um método
`final` (`document/DocumentGenerator.java:10-44,46-53`). O comprovante, a fatura com o ISS
(`document/CorporateGuestDocumentGenerator.java:61-65`) e o booking receipt
(`document/ForeignGuestDocumentGenerator.java`) ficam cada um no seu lugar. Uma agência de turismo seria uma nova
subclasse e um novo ramo em `selectGenerator`. Duas coisas afastam do ideal:
- a escolha é uma cadeia de `instanceof` com um `throw` no fim (`document/DocumentService.java:18-25`), e não um
  `switch` exaustivo sobre a interface selada, que faria o compilador avisar quando a agência chegar;
- cada gerador faz um cast cego da identificação (`document/CorporateGuestDocumentGenerator.java:26-27`,
  `IndividualGuestDocumentGenerator.java:24-25`, `ForeignGuestDocumentGenerator.java:31-32`).

**O exagero.** Não há estrutura sem necessidade fora do pattern: nenhuma interface de uma implementação só e nenhuma
camada que só repassa. `Document.from` é uma função auxiliar, não uma fábrica a mais.

**O resto do código.** Os tipos estão bem escolhidos: `BigDecimal` com `HALF_EVEN` a cada passo
(`domain/Reservation.java:70-83,96-102`), `LocalDate`, `enum` para a situação, o plano e o tipo de quarto, e as
transições protegidas dentro da `Reservation` (`domain/Reservation.java:104-137`). Os defeitos:
- **Duplicação.** `formatRoom`, `formatPeriod`, `formatSubtotal`, `formatDiscount` e `formatTotal` são idênticos
  entre o comprovante e a fatura (`IndividualGuestDocumentGenerator.java:30-55` e
  `CorporateGuestDocumentGenerator.java:32-58`). A cópia dos cinco campos de identificação aparece em quatro lugares
  (`web/ApiController.java:217-235`, `document/Document.java:33-41`,
  `repository/ReservationRepository.java:108-126` e `:204-216`).
- **Um formatador compartilhado entre threads.** `NumberFormat` e `DecimalFormat` não são seguros para várias threads
  e estão em campos `static` compartilhados por todas as requisições do Javalin
  (`IndividualGuestDocumentGenerator.java:15`, `CorporateGuestDocumentGenerator.java:16`,
  `ForeignGuestDocumentGenerator.java:15-22`). Sob carga, um documento pode sair com o valor embaralhado.
- **Um bug de construção.** `Document.from` passa `null` como título (`document/Document.java:43`), e nenhum gerador
  o preenche.
- **Entrada aceita em silêncio.** `getRequiredInt` usa `asInt()` (`web/ApiController.java:285`), então
  `"roomNumber": "abc"` vira 0. Ao ler uma linha do banco, `mapReservation` trata como empresa qualquer linha sem CPF
  nem passaporte (`repository/ReservationRepository.java:213-215`).
- **Escala desigual do dinheiro.** O zero do dinheiro é `BigDecimal.ZERO`, de escala 0
  (`domain/Reservation.java:43,98`), e por isso o JSON mostra `0` em vez de `0.00`.

## Testes

**Números:** 16 testes rodaram, 0 falhas, 0 erros, 0 pulados; 16 métodos escritos em 2 arquivos. Cobertura de 23,9%
das linhas (196 de 819) e 23,3% dos ramos (41 de 176). Os pacotes `document` (102/105) e `domain` (94/131) estão
cobertos; `service`, `repository`, `web` e o `App` têm 0%. Nenhum teste pela API nem contra o banco
(`measures/tests.md`).
**Nota:** com problemas

O que está testado, com valores:
- o preço na baixa e na alta temporada (`src/test/java/com/pousadamarealta/domain/ReservationTest.java:22-25,39-42`:
  499.80 e 599.76) e o desconto com o arredondamento (`:56-58`: 116.97 e 662.83);
- as transições e as devoluções (`ReservationTest.java:62-202`);
- os três documentos (`DocumentGenerationTest.java:13-91`), incluindo o ISS de 38,99 (`:61`). Trocar 0.05 por 0.06
  faria um teste falhar.

Mas as asserções dos documentos são todas `contains` em pedaços do texto (`DocumentGenerationTest.java:28-38`), e
isso deixa passar dois defeitos que o roteiro achou: o título `null` (nenhum teste olha `document.title()`) e o
espaço não separável depois de "R$" (`contains("779,80")` e `contains("R$")` passam com ou sem ele).

Ficaram sem rede:
- a regra "exatamente uma identificação" (`web/ApiController.java:187-208`): apagar a checagem de `count != 1` não
  derruba nenhum teste;
- o separador de milhar do booking receipt (nenhum valor acima de 1.000) e a linha "Discount" em inglês;
- o conflito de quartos, a capacidade e os códigos de erro;
- os dois relatórios (`ReportService`, 0%);
- o e-mail de confirmação e a persistência.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE/EXCLUDE. O esquema é criado com
`CREATE TABLE` no Java, sem migrações. 0 transações explícitas e 0 `FOR UPDATE`. Nenhum SQL montado com dado de fora
(`measures/database.md`).
**Nota:** com problemas

O que está certo: os tipos (dinheiro com 2 casas e `date`), o SQL todo com `?`, as conexões devolvidas por
`try-with-resources` e o HikariCP (`App.java:53-75`). Os dados sobrevivem ao reinício (3 de 3 no roteiro).

Os defeitos:
- **Nada protege a regra principal.** "Um quarto não pode ter duas reservas na mesma noite" é uma consulta seguida de
  um `INSERT` (`service/ReservationService.java:46-54`), em conexões separadas, sem transação, sem trava e sem
  restrição de exclusão no banco. No roteiro, os dois pedidos simultâneos foram criados nas 10 de 10 rodadas: é
  reserva dupla de verdade, não um risco teórico.
- **A reserva não está ligada ao quarto.** `reservations.room_number` não tem chave estrangeira para `rooms`
  (`repository/ReservationRepository.java:24`) e não tem índice.
- **Nenhum CHECK.** Não há CHECK em `status`, `rate_plan` nem `rooms.type`, nem um CHECK de que uma única
  identificação está preenchida.
- **O esquema não sabe evoluir.** `CREATE TABLE IF NOT EXISTS` em cada repositório
  (`repository/ReservationRepository.java:19-47`, `RoomRepository.java:21-39`, `MessageRepository.java:16-29`) não
  acrescenta uma coluna numa tabela que já existe: a coluna da agência de turismo vai exigir mexer à mão no banco.
- **Pequenos vazamentos e consultas a mais.** Os `Statement` de `initialize` não são fechados
  (`RoomRepository.java:23`, `MessageRepository.java:18`, `ReservationRepository.java:21`), e o relatório de
  ocupação faz uma consulta por quarto (`service/ReportService.java:31-33`).

## Comportamento

**Números:** 71 de 83 conferências passaram (`measures/behavior.md`):
- a parte comum: 41 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 26 de 32;
- as duas reservas ao mesmo tempo: 1 de 2.

**Nota:** com problemas

As falhas, por causa:
- **O título do documento vem `null`** (6 falhas: a segunda via e o check-out na parte comum; o documento do CPF, da
  empresa, do check-out da empresa e do estrangeiro no cenário). Vem de `document/Document.java:43`. O texto do
  documento está certo, mas o campo `title` que o site usaria para saber se é comprovante, fatura ou booking receipt
  vem vazio.
- **O "R$" vem seguido de um espaço não separável** (3 falhas: o "Total" do e-mail; o ISS e o total da fatura).
  Conferi à mão: `Total: R$ 779,80`. A causa é o `NumberFormat.getCurrencyInstance(pt-BR)` do Java
  (`IndividualGuestDocumentGenerator.java:15`, `CorporateGuestDocumentGenerator.java:16`). Na tela parece igual, mas
  é outro caractere: quem copiar o texto, buscar por "R$ 38,99" ou comparar com o modelo do dono não acha. Os valores
  estão certos (779,80, 38,99).
- **As reservas simultâneas** (1 falha): as duas reservas foram criadas em todas as 10 rodadas. Para o dono, dois
  hóspedes que reservam o mesmo quarto pelo site no mesmo instante ficam ambos com reserva. O defeito pesa na coluna
  do banco e aparece aqui.
- **Um corpo que não é JSON dá 500** em vez de 400 `INVALID_REQUEST` (1 falha). O handler só converte
  `IllegalArgumentException` (`web/ApiController.java:87,103`), e o erro do Jackson escapa. Também conferi à mão que
  `GET /reservations/abc` devolve o erro padrão do Javalin (`TYPE_CONVERSION_FAILED`), fora do formato pedido.
- **O dinheiro com escala 0** (1 falha): `amountPaid` vem `0` em vez de `0.00`, e o `discount` do plano STANDARD
  também vem `0` na criação (`domain/Reservation.java:43,98`).

O resto funciona: o preço com a temporada e o arredondamento, os códigos de erro, as transições, as devoluções, os dois
relatórios, a persistência e, no cenário, a validação das três formas de identificação, os cinco campos com `null`, o
booking receipt (BRL com milhar, datas ISO, "1 night", "Discount") e o e-mail em português com a fatura.

## Dúvidas

- A forma do pattern é uma fábrica concentrada (`DocumentService.selectGenerator`) que escolhe subclasses de um
  gerador com Template Method, e não o Factory Method clássico (um criador com o método de fábrica sobrescrito) nem
  um método polimórfico no tipo de hóspede. A referência aceita essa forma como acerto. Pesa contra ela a escolha por
  `instanceof` em vez de um `switch` exaustivo sobre a interface selada, mas é uma escolha só e num lugar só.
- No banco, fiquei entre `com problemas` e `ruim`. A skill põe em `com problemas` "a duplicação protegida só dentro
  do processo", e aqui não há proteção nenhuma: o roteiro mostrou reserva dupla em 10 de 10 rodadas. Dei
  `com problemas` porque o resto (tipos, chaves primárias, SQL com parâmetros, persistência) está certo e a skill não
  lista esse caso entre os `ruim`; um revisor mais rígido daria `ruim` pelo risco de dado errado.
