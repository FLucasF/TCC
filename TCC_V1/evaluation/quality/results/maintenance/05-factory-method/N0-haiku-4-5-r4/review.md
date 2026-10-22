# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em 7 pacotes: `hotel.web` (rotas e JSON), `hotel.reservations` (serviço, regras de preço,
`ReservationStore` com JDBC), `hotel.documents` (o comprovante), `hotel.rooms`, `hotel.messages`, `hotel.reports` e a
montagem em `hotel`. Uma requisição passa por `HttpApi` → `ReservationService` → `ReservationStore` (PostgreSQL, com
migrações do Flyway). O agente pôs os três jeitos de identificar o hóspede como quatro campos a mais, que podem vir
null, em `Guest`, na requisição, na resposta, no documento e numa migração nova (V2 e V3, sem mexer na V1). A escolha
do documento ficou num `if`/`else` dentro do próprio record `ReservationReceipt`, e não foi criado nenhum tipo de
documento. Os textos da fatura e do *booking receipt* estão certos no e-mail. O defeito mais sério é que **o JSON do
documento (check-out e segunda via) não traz o campo `text`**, que o pedido exige: `text()` é um método e não um
componente do record, e o Jackson não o serializa. Por isso 14 das 32 conferências do cenário falharam. Também
preocupa um `Flyway.repair()` que agora roda a cada subida, com a exceção engolida (`Database.java:28-32`).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Há um ciclo, `hotel.documents` ↔
`hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). As linhas cresceram em `hotel.web`
(236 → 296) e em `hotel.documents` (79 → 155). A maior classe é `HttpApi` (170 → 211 linhas).
**Nota:** bom

As camadas da base continuam como estavam. O SQL novo ficou no `ReservationStore`
(`ReservationStore.java:32-35`, `130-137`), e o serviço continua sem Javalin e sem SQL. O documento continua em
`hotel.documents`. O agente não criou dependência nem ciclo novo. A regra "um e só um jeito de identificar"
(`HttpApi.java:141-159`) ficou na borda HTTP, mas a base já validava ali os campos obrigatórios do corpo
(`HttpApi.java:130-139`), e essa regra é um formato de entrada com resposta 400. O agente seguiu o padrão da base.
Há duas pequenas mudanças de mão na borda, que não chegam a misturar camadas:

- os handlers agora buscam o quarto (`rooms.find(...).orElseThrow()`, repetido em `HttpApi.java:67`, `72`, `77`,
  `82` e `87`), coisa que o serviço já sabe fazer (`ReservationService.java:121-123`);
- `ReservationResponse.of` monta um `ReservationReceipt` (`ReservationResponse.java:36-37`).

## Design de baixo nível

**Números:** nenhum tipo de documento foi criado. Os três documentos (comprovante, fatura, *booking receipt*) são o
mesmo record, `ReservationReceipt`, com os campos dos três tipos de hóspede (que podem vir null). A escolha aparece
**duas vezes** dentro dele (`selectTitle`, `ReservationReceipt.java:56-64`; `text()`, `ReservationReceipt.java:66-74`),
e cada caso tem a sua cópia do texto (`76-90`, `92-109`, `111-126`). Fora do pattern, `new ReservationReceipt`
aparece em 4 lugares: os 3 do serviço e um novo em `ReservationResponse.java:37`.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido pede que a escolha de qual documento criar fique num lugar só, de onde os três pontos de
saída o peçam. A agência que vem por aí precisa poder entrar sem espalhar mudança. O agente não criou fábrica, tipo
por documento nem método polimórfico no hóspede. A variação ficou num `if`/`else if`/`else` sobre quais campos do
`Guest` estão preenchidos, dentro do record:

- `selectTitle` decide pelo CPF primeiro (`ReservationReceipt.java:57`);
- `text()` decide pelo CNPJ primeiro (`ReservationReceipt.java:67`). As duas cadeias não estão nem na mesma ordem.

Os três pontos do serviço (`ReservationService.java:64`, `109`, `118`) chamam o mesmo construtor, por isso o `if` não
se repete ali. O que aproxima de uma fábrica concentrada é só isso: a decisão mora numa classe. Mas ela não escolhe
um documento: escolhe ramos de texto num objeto único. Para pôr a agência, seria preciso mexer em `Guest`, na
requisição, na resposta, nos campos do record, em `selectTitle`, em `text()` e acrescentar mais uma cópia do texto.
Por isso a classe é omissão. O tipo de hóspede também não existe como tipo: são sete `String` soltas em `Guest`
(`Guest.java:3-10`), e quem decide o tipo vê qual campo não é null.

**O exagero.** Nenhuma estrutura sem necessidade: contenção.

**O resto do código:**

- `receiptText` e `invoiceText` são quase a mesma coisa linha a linha (`ReservationReceipt.java:76-90` e `92-109`;
  só mudam a linha de identificação e a do ISS). O booking receipt repete a mesma sequência.
- O agente copiou para dentro do record o formatador de data que já existia em `TextFormat`
  (`ReservationReceipt.java:32` contra `TextFormat.java:12` e `21`) e trocou `TextFormat.date` pela cópia no
  comprovante da base (`ReservationReceipt.java:81-82`). A constante `BRAZIL` ficou sem uso
  (`ReservationReceipt.java:31`), e a importação de `Rooms` também (`ReservationResponse.java:8`).
- O ISS é arredondado com `HALF_UP` (`ReservationReceipt.java:106`), mas o resto do sistema arredonda o dinheiro com
  `HALF_EVEN` (`PriceCalculator.java:51`), e o pedido fala em "arredondado para centavos como sempre". Os dois diferem
  quando o terceiro decimal é 5 (um total de R$ 779,70 dá ISS de R$ 38,99 com `HALF_UP` e de R$ 38,98 com
  `HALF_EVEN`). O número de 779,80 bate.
- `Database.java:28-32` engole qualquer `Exception` do `repair()` com um comentário que supõe a causa.
- O Javadoc que dizia o contrato do corpo foi apagado de `CreateReservationRequest.java:6`, e nada o substituiu.

## Testes

**Números:** 30 testes rodaram, nenhum falhou nem foi pulado, e há 30 métodos escritos. A base tinha 26, todos
mantidos. O agente só adaptou o construtor de `Guest` em `ReportsTest.java:31` e `ReservationServiceTest.java:34`. A
cobertura é de 93,3% das linhas (560/600) e 85,9% dos ramos (146/170); na base era 93,8% e 87,1%. `hotel.documents`
ficou com 75/78 linhas e 16/20 ramos.
**Nota:** com problemas

Os 4 testes novos são todos pela API (`HttpApiTest.java:124-189`) e conferem pouco do que o pedido dá com valores:

- **A fatura:** o teste confere só que o texto contém "Empresa: Tecnosul Ltda" e o rótulo "ISS retido (5%)"
  (`HttpApiTest.java:141-142`), nunca o valor R$ 38,99. Trocar 0.05 por 0.06 em `ReservationReceipt.java:106`, ou
  apagar o arredondamento, não derruba nenhum teste.
- **O *booking receipt*:** confere o nome, o passaporte e a data de entrada (`HttpApiTest.java:163-164`). O dinheiro
  em `BRL 1,234.56`, o "1 night" contra "N nights" e a linha "Discount:" ficam sem nenhum teste.
- **As recusas:** testam nenhuma identificação e CPF com CNPJ (`HttpApiTest.java:168-189`). O par incompleto (CNPJ
  sem razão social, passaporte sem país) não tem teste, e os `if` de `HttpApi.java:151-159` ficariam sem rede se
  alguém os apagasse.
- **Os três lugares de saída:** nenhum teste confere o documento novo no check-out, na segunda via ou no e-mail. Os
  testes leem `title` e `text` de `GET /reservations/{id}` (`HttpApiTest.java:139-142`, `161-164`), um campo que o
  agente acrescentou à reserva. Por isso nenhum teste pegou que o JSON do documento não tem `text` (a falha principal
  do comportamento).

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, as datas em `date`, e a chave
estrangeira tem índice. A restrição `EXCLUDE` contra reserva dupla continua. O Flyway aplicou 3 migrações: V1 igual à
base, V2 e V3 novas. As colunas de situação e de plano não têm CHECK, como na base.
**Nota:** com problemas

A evolução do esquema está certa: o agente não editou a V1 e criou migrações novas para as quatro colunas
(`V2__add_company_and_passport.sql:2-5`) e para tirar o `NOT NULL` do CPF (`V3__make_guest_cpf_nullable.sql:2`). O SQL
novo usa parâmetros (`ReservationStore.java:32-55`). Os dois defeitos são do agente:

- **`Flyway.repair()` a cada subida, com qualquer erro engolido** (`Database.java:28-32`). O `repair` realinha os
  checksums das migrações já aplicadas e apaga do histórico as que falharam. Rodar isso sempre, antes do `migrate`,
  desliga a proteção que impede subir o sistema com uma migração aplicada que foi editada depois: a mudança passa
  calada, e o banco de quem já roda o sistema fica diferente do que os arquivos dizem. O `catch (Exception ignored)`
  ainda esconde qualquer falha de conexão ou de permissão nesse passo. Tudo indica que o agente pôs isso para
  contornar um problema do próprio desenvolvimento.
- **O banco não protege mais a identificação.** A V3 tira o `NOT NULL` de `guest_cpf` e não põe nada no lugar (por
  exemplo, um CHECK que exija exatamente um dos três jeitos, com os pares completos). A regra só existe em
  `HttpApi.java:141-159`.

## Comportamento

**Números:** 69 de 83 conferências passaram. A parte comum passou 46 de 46, os dados depois de reiniciar 3 de 3 e as
duas reservas ao mesmo tempo 2 de 2. O cenário 05 passou só 18 de 32.
**Nota:** com problemas

As 14 falhas têm uma causa só: a resposta de `GET /reservations/{id}/document` e a de
`POST /reservations/{id}/check-out` não trazem o campo `text`. Elas serializam o record `ReservationReceipt`, e
`text()` (`ReservationReceipt.java:66`) não é componente dele. Com o serviço de pé, a segunda via da fatura veio com
`title`, os quatro campos novos e os de hoje, mas sem `text`. O texto em si está certo: o e-mail de confirmação trouxe
"Fatura de hospedagem nº 1 … ISS retido (5%): R$ 38,99" e "Booking receipt no. 1 … Nights: BRL 2,783.52 / Discount:
BRL 417.53 / Total: BRL 2,365.99". Uma reserva de uma noite saiu com "1 night". O script também confirma que o e-mail
leva a fatura e continua em português.

Para o dono da pousada, isso quer dizer o seguinte:

- o hóspede recebe o documento certo por e-mail;
- mas o site não consegue mostrar nem imprimir o texto do documento no check-out nem na segunda via, que o pedido
  dizia que viria no campo `text`. Isso vale também para o comprovante de quem tem CPF ("com CPF: o texto do
  documento" falhou);
- o texto só aparece na consulta da reserva, num campo que o pedido não pediu ali e que sai até para reserva pendente
  ou cancelada (`ReservationResponse.java:37`).

As recusas (nenhuma identificação, duas juntas, CNPJ sem razão social, passaporte sem país) passaram, e a parte comum
continua inteira.

## Dúvidas

- A classe ficou entre omissão e malfeito. Escolhi omissão porque não existe estrutura nenhuma: nem fábrica, nem tipo
  de documento, nem tipo de hóspede. A variação é um `if` duplicado (`ReservationReceipt.java:56-74`) e uma cópia do
  texto por caso. Dá para ler o construtor único do record como uma "fábrica" que sempre devolve o mesmo objeto, com a
  escolha feita depois em `text()`. A referência chama isso de malfeito.
- Não ficou claro se a regra de "exatamente um jeito de identificar" em `HttpApi.java:141-159` deveria estar no
  serviço. Considerei validação de entrada, como a base já fazia na mesma função, e não pesou na arquitetura.
- Arredondar o ISS com `HALF_UP` em vez do `HALF_EVEN` da base não muda o exemplo do pedido. Só diverge quando o
  terceiro decimal é 5, e o roteiro não testa esse caso.
