# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve a estrutura da base: `hotel.web` (Javalin: `HttpApi`, os records de requisição e resposta) chama
`hotel.reservations` (`ReservationService`, `PriceCalculator`, `ReservationStore` com JDBC), que monta o documento em
`hotel.documents` e guarda os dados no PostgreSQL por migrações do Flyway. A mudança foi pequena e localizada: o
`Guest` ganhou quatro campos que podem ser nulos (`final/src/main/java/hotel/reservations/Guest.java:3-10`), uma
migração nova `V2` acrescenta as colunas e um CHECK de "um jeito só de identificar", o `HttpApi` valida os três jeitos
(`final/src/main/java/hotel/web/HttpApi.java:127-136`), e o `ReservationReceipt` passou a escolher, no seu próprio
construtor, o título e o texto do documento pelo tipo de hóspede
(`final/src/main/java/hotel/documents/ReservationReceipt.java:31-107`). Os três lugares que emitem o documento
(`ReservationService.java:64,109,118`) não mudaram e continuam chamando esse construtor. O roteiro de comportamento
passou inteiro (83 de 83) e o `mvn verify` passa (31 testes). Os pontos mais sérios: o documento continua um record
só, com a escolha do tipo feita em dois `if` diferentes e um texto copiado quase igual para cada caso; uma requisição
com CPF e um CNPJ sem razão social é aceita, e o CNPJ some sem aviso; e os testes novos não conferem os valores do ISS
nem o formato do dinheiro em inglês.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base: os
mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo (`measures/dependencies-base.md`). As linhas cresceram em
`hotel.documents` (de 79 para 227), `hotel.reservations` (de 508 para 544) e `hotel.web` (de 236 para 265).
**Nota:** bom

O agente não criou nenhuma dependência nova entre pacotes, e o ciclo entre `documents` e `reservations` já existia na
base (o recibo lê `Reservation`, e o serviço cria o recibo). Cada camada continua com o seu papel. O HTTP só lê o
corpo, valida e converte para `NewReservation` (`HttpApi.java:109-148`). O SQL das colunas novas fica no
`ReservationStore` (`ReservationStore.java:32-35`, `130-137`). O texto do documento fica em `hotel.documents`, que
não usa nem Javalin nem JDBC (tabela de bibliotecas em `measures/dependencies.md`). A regra "um jeito só de
identificar" ficou na borda HTTP (`HttpApi.java:127-136`), e o serviço não a confere. Mas a base já validava os campos
obrigatórios ali, e o banco também a protege com um CHECK (`V2__add_guest_identification_types.sql:11-15`), então
isso é coerente com o que existia. A maior classe agora é o `ReservationReceipt` (203 linhas, era 55). Ele cresceu
por causa da duplicação descrita no design, não por misturar camadas.

## Design de baixo nível

**Números:** 3 tipos de documento (comprovante, fatura, booking receipt), todos decididos dentro de
`ReservationReceipt`. Os 3 lugares que emitem o documento (`ReservationService.java:64,109,118`) passam pelo mesmo
construtor, e nenhum deles pergunta o tipo de hóspede. A escolha do tipo aparece em 2 `if` separados, um para o
título (`ReservationReceipt.java:68-76`) e outro para o texto (`ReservationReceipt.java:95-107`). Há 3 métodos de
texto quase copiados (`:110`, `:138`, `:170`). Nenhum tipo novo de documento ou de hóspede foi criado.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A referência aceita como acerto uma fábrica concentrada, isto é, um método que escolhe o documento
pelo tipo de hóspede e que os três lugares chamam. É o que existe aqui, na forma mais simples. O construtor
`ReservationReceipt(Reservation, Room)` (`ReservationReceipt.java:31-66`) decide o título e o texto, e o e-mail de
confirmação, o check-out e a segunda via continuam fazendo só `new ReservationReceipt(reservation, roomOf(reservation))`
(`ReservationService.java:64,109,118`), sem `if` sobre o hóspede. O comprovante da base, para quem tem CPF, ficou igual
(`receiptText`, `:110-136`, é o antigo `text()`). Três coisas afastam o código da forma limpa:

- **A escolha está em dois lugares, com condições diferentes.** O título testa `isCpf()` e depois `isCompany()`
  (`:69-74`). O texto testa `companyCnpj != null` e depois `guestPassport != null` (`:95-107`). Hoje os dois `if`
  concordam porque a validação e o CHECK garantem um jeito só. Mas a agência de turismo, que o dono já anunciou, vai
  pedir um ramo novo em cada um, além de campos novos no `Guest` e de outro método de texto.
- **Não existe um tipo para o documento nem para o hóspede.** O documento é sempre o mesmo record, e o que muda é uma
  string. O tipo de hóspede é deduzido de qual campo não é nulo (`Guest.java:16-26`; o `isPassport()` da linha 24
  nem é usado).
- **Cópia por caso.** `receiptText` e `invoiceText` repetem as linhas de quarto, datas, diárias, desconto e total
  (`:121-134` e `:155-163`).

**O exagero.** Não há estrutura sem necessidade: nada de interface, fábrica ou camada a mais. O construtor de três
argumentos em `Guest.java:12-14` mantém os testes da base compilando.

**O resto do código.** O construtor do recibo repete as mesmas 16 expressões duas vezes: uma vez para os campos do
record e outra para o `generateText`, que recebe 16 parâmetros soltos (`ReservationReceipt.java:31-65`, `78-94`).
Bastaria passar a `Reservation` e o `Room`. Os métodos de texto têm 12 e 13 parâmetros cada (`:110-122`, `:138-151`,
`:170-183`). O `Guest` virou um saco de campos que podem ser nulos (`Guest.java:3-10`), sem um tipo que diga qual
identificação vale. O ISS usa um `0.05` solto dentro do texto (`:165`), em vez de uma constante com nome como as do
`PriceCalculator`. O arredondamento meio para o par segue o "como sempre" da base (`PriceCalculator.java:51`). Os
tipos do domínio continuam certos (`BigDecimal`, `LocalDate`). São defeitos localizados, todos na parte nova, mas são
justamente eles que vão tornar o próximo documento caro de fazer.

## Testes

**Números:** 31 testes rodaram, 0 falhas, 0 erros, 0 pulados; 31 métodos escritos (a base tinha 26, e os 26
continuam lá, sem mudança). Cobertura de linhas: 94,5% (562 de 595; na base, 93,8%). Cobertura de ramos: 87,2% (143 de
164; na base, 87,1%). Em `hotel.documents`: 89 de 91 linhas e 16 de 20 ramos.
**Nota:** com problemas

O agente acrescentou 5 testes pela API em `final/src/test/java/hotel/web/HttpApiTest.java`. A fatura de empresa
(`:138-157`) e o booking receipt (`:160-181`) conferem o título, a primeira linha, a linha da empresa e a do hóspede,
as datas ISO e "2 nights". As recusas também estão cobertas: dois jeitos de identificar (`:184-194`), nenhum
(`:197-206`) e CNPJ sem razão social (`:209-218`).

As asserções do que é mais fácil de errar são fracas. Para o ISS, o teste confere só que o rótulo aparece
(`assertTrue(text.contains("ISS retido (5%):"))`, `:156`), sem o valor R$ 38,99. Para o dinheiro em inglês, confere
só o prefixo `"BRL "` (`:180`), sem `BRL 779.80` e sem o separador de milhar. Pela pergunta da mutação: trocar `0.05`
por `0.50` em `ReservationReceipt.java:165`, tirar a vírgula de milhar do `"BRL %,.2f"` (`:201`), trocar "night" por
"nights" quando há uma noite só (`:191`) ou apagar a linha "Discount" (`:193-195`) não faria nenhum teste falhar.
Também faltam testes para o passaporte sem país e para a combinação CPF + CNPJ sem razão social (veja
Comportamento). Nenhum teste confere que o e-mail de confirmação e o check-out levam a fatura ou o booking receipt:
os testes novos só leem a segunda via (`:148`, `:170`).

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. O dinheiro está em
`numeric(10,2)`. Nenhuma chave estrangeira sem índice, nenhuma data guardada como texto. 2 migrações:
`V1__create_tables.sql` igual à da base e `V2__add_guest_identification_types.sql` nova. O que as conferências
acusaram: as colunas de situação e de tipo sem CHECK (`status`, `rate_plan`, `type`, `channel`), herança da base que o
agente não mexeu.
**Nota:** bom

O agente evoluiu o esquema do jeito certo. Fez uma migração nova, sem editar a V1
(`final/src/main/resources/db/migration/V2__add_guest_identification_types.sql:3-8`), tirou o `NOT NULL` do
`guest_cpf` e acrescentou as quatro colunas. As reservas que já existem continuam válidas, porque todas têm CPF. O
CHECK `exactly_one_identification` (`:11-15`) põe no banco a regra de um jeito só, com o par completo. Ele é um
pouco frouxo: no caso do CPF não exige `company_name` e `guest_country` nulos. O SQL novo usa parâmetros `?`
(`ReservationStore.java:32-54`), e as partes montadas com `+` são só constantes (`COLUMNS`, `:18-20`). A leitura
preenche os campos novos (`:130-137`). A restrição EXCLUDE contra duas reservas na mesma noite veio da base e continua
lá.

## Comportamento

**Números:** 83 de 83 conferências passaram. Parte comum: 46 de 46. Depois de reiniciar: 3 de 3. Cenário 05: 32 de
32. Reservas simultâneas: 2 de 2.
**Nota:** bom

O dono da pousada recebe o que pediu. A pessoa com CPF continua com o comprovante de sempre. A empresa recebe a fatura
com a razão social, o CNPJ e o ISS de R$ 38,99. O estrangeiro recebe o booking receipt em inglês, com `BRL 2,783.52`,
as datas `2027-03-10`, "1 night" e a linha "Discount". O documento sai igual no e-mail (que continua em português), no
check-out e na segunda via, e o JSON traz os campos novos e o `text`. Os pedidos sem identificação, com CPF e CNPJ
juntos, com CNPJ sem razão social e com passaporte sem país voltam com 400 INVALID_REQUEST.

Fora do roteiro, conferi à mão com `curl` contra o serviço da `final/`. Um pedido com `guestCpf` mais um
`companyCnpj` sem `companyName` foi aceito (201), e a resposta trouxe `companyCnpj: null`. O mesmo aconteceu com
`guestCpf` mais `guestCountry` sem passaporte. A causa é que um par incompleto deixa de contar como identificação
(`HttpApi.java:128-131`) e depois é jogado fora em silêncio (`:141-145`). O pedido diz "tem que ser um desses jeitos,
e só um; se não for, 400". O site que mandar o CNPJ sem a razão social, junto com um CPF, recebe um comprovante de
pessoa física em vez da fatura, sem nenhum aviso. É uma brecha pequena, num caso que o texto não descreveu com todas as
letras, e por isso não muda a nota. Ela está registrada nos testes, que não a cobrem.

## Dúvidas

- Entre as formas de acerto que a referência aceita, a usada foi a fábrica concentrada, e na versão mínima: o próprio
  construtor de `ReservationReceipt` escolhe o título e o texto, sem classe nem tipo por documento. A escolha está
  concentrada para quem emite o documento, e por isso a classe ficou em `acerto`. Mas dentro da classe ela aparece em
  dois `if`, com um texto copiado para cada caso, e um leitor mais rígido poderia ver nisso a "variação num `if`" da
  omissão. A evidência sustenta melhor o acerto, porque os três lugares não decidem nada. A fragilidade pesou na nota.
- Aceitar CPF junto com um par incompleto (CNPJ sem razão social, país sem passaporte) pode ser lido como uma
  interpretação do "só um" que o texto não fechou. Contei como brecha pequena, não como falha do cenário.
