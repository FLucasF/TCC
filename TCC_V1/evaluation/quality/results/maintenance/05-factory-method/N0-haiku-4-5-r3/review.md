# Revisão: manutenção, cenário 05

## Visão geral

A base já tinha os pacotes `hotel.web` (Javalin: rotas, leitura do corpo, respostas), `hotel.reservations` (serviço, regras de preço, situação da reserva e o `ReservationStore` com JDBC), `hotel.documents` (o comprovante), `hotel.messages`, `hotel.reports` e `hotel.rooms`, com a montagem em `hotel.Hotel`. Uma requisição vai de `HttpApi` para `ReservationService` e dele para `ReservationStore`, e os dados ficam no PostgreSQL, com o esquema feito por migrações do Flyway. O agente acrescentou quatro campos de identificação a `Guest`, criou uma migração `V2`, criou a `CompanyInvoice` e a `InternationalBookingReceipt` ao lado do `ReservationReceipt`, e criou uma fábrica estática (`DocumentFactory`) que os três lugares (confirmação, check-out e segunda via) chamam. O comportamento está todo certo (83 de 83). Os problemas mais sérios estão no design e nos testes. A fábrica devolve uma interface que não basta para quem a usa, e por isso a `HttpApi` pergunta de novo qual é o documento, com uma cadeia de `instanceof` (`HttpApi.java:208-217`). O agente não escreveu nenhum teste para a parte nova: a fatura e o booking receipt nunca rodam nos testes. E enfraqueceu um teste da base.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base. Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). O pacote `hotel.documents` passou de 2 para 7 classes (de 79 para 330 linhas). A `HttpApi` passou de 170 para 218 linhas e continua sendo a maior classe.
**Nota:** com problemas

As camadas da base continuam de pé: só `hotel.web` usa o Javalin, e o JDBC fica em `reservations`, `rooms` e `messages`. O SQL novo está todo no `ReservationStore` (`ReservationStore.java:31-56`, `:129-137`). A fábrica é chamada do serviço (`ReservationService.java:66`, `:111`, `:120`), não das rotas. O ciclo `documents` ↔ `reservations` vem da base, e o agente só o reforçou: os documentos novos leem `Reservation` e `Guest`, e o serviço chama a `DocumentFactory`.

O que o agente pôs no lugar errado ficou na borda HTTP, em dois pontos:
- **A regra de identificação mora no handler.** A regra "exatamente um jeito de identificar o hóspede: o CPF, ou o CNPJ com a razão social, ou o passaporte com o país" está em `HttpApi.java:132-155`. Na base, a `HttpApi` só conferia se os campos vinham. As regras de domínio (a saída depois da entrada, o número de hóspedes, a capacidade) ficavam no serviço (`ReservationService.java:31-41`). Do jeito que ficou, um `Guest` sem nenhuma identificação, ou com duas, passa pelo `ReservationService.create` sem recusa (`Guest.java:3-14` não confere nada).
- **A `HttpApi` decide o tipo de cada documento.** O método `documentToJson` (`HttpApi.java:208-217`) pergunta pelo tipo concreto para montar o JSON. Por isso a borda importa as três classes de documento (`HttpApi.java:7-11`).

As duas misturas são localizadas, e sem ciclo novo.

## Design de baixo nível

**Números:** a criação do documento está concentrada num lugar só (`DocumentFactory.java:12-20`). Os 3 lugares que emitem o documento usam a fábrica, e nenhum faz `new` à mão (`ReservationService.java:66`, `:111`, `:120`). Fora da fábrica, porém, o tipo do documento é perguntado de novo em 1 lugar de produção (`HttpApi.java:208-217`, com 3 `instanceof` e um `IllegalArgumentException` para o resto) e em 2 lugares de teste (`ReservationServiceTest.java:145`, `:157`). Também há 3 métodos `Document.from` quase iguais (`Document.java:28-88`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Isto aproxima do acerto:
- a escolha de qual documento criar é feita num lugar só, uma fábrica estática que olha o `Guest` (`DocumentFactory.java:13-19`);
- os três lugares que a referência cita pedem o documento a ela;
- o comprovante de quem tem CPF entrou na mesma estrutura.

Isto afasta do acerto:
- **A interface não basta para quem usa o documento.** A interface `DocumentType` só expõe `title`, `reservationId` e `text` (`DocumentType.java:4-8`). Para montar o JSON, a `HttpApi` precisa voltar ao tipo concreto (`HttpApi.java:208-216`). Com isso, quem usa a estrutura ainda pergunta qual é o caso.
- **O documento da agência vai doer.** Ele vai exigir uma classe nova, um ramo na fábrica, um `Document.from` novo e um ramo novo no `instanceof` da `HttpApi`. Se alguém esquecer o último, o código compila e o check-out da agência responde 500 (`HttpApi.java:216`).
- **Os testes repetem o problema.** Eles fazem cast para `ReservationReceipt` (`ReservationServiceTest.java:157`), sinal de que o tipo devolvido não serve sozinho.

Por isso a classe é `malfeito`, e não `acerto`.

**O exagero.** Não há estrutura sem necessidade: a fábrica e a interface fazem parte do pattern, e o `Document` existe porque o JSON pede uma forma única. Por isso, `contenção`.

**O resto do código:**
- **Duplicação.** Os três documentos copiam o mesmo construtor de 13 a 16 campos a partir da `Reservation` (`ReservationReceipt.java:28-46`, `CompanyInvoice.java:25-40`, `InternationalBookingReceipt.java:28-43`). Os três `Document.from` repetem o mesmo mapeamento (`Document.java:28-88`). Há ainda o `text()` quase igual no comprovante e na fatura (`ReservationReceipt.java:48-62`, `CompanyInvoice.java:42-58`).
- **Campos sem sentido no comprovante.** O `ReservationReceipt` ganhou `companyCnpj`, `companyName`, `guestPassport` e `guestCountry` (`ReservationReceipt.java:15-18`), que são sempre nulos para quem tem CPF.
- **Nenhum tipo para a identificação.** O `Guest` virou sete `String` que podem ser nulas, sem um tipo ou `enum` para o jeito de identificar (`Guest.java:3-10`). A fábrica deduz o tipo por `!= null` (`DocumentFactory.java:13-15`).
- **Ramo morto.** A validação tem um ramo que nunca dispara: `hasCpf && (hasCompany || hasPassport)` em `HttpApi.java:145`, que a contagem da linha 138 já exclui.
- **Import que sobrou.** O import de `ReservationReceipt` ficou sem uso em `ReservationService.java:5`.

Os tipos estão certos: `BigDecimal` com `HALF_EVEN` no ISS (`CompanyInvoice.java:43`) e `LocalDate`. São defeitos localizados, mas um revisor exigente pediria para mudar.

## Testes

**Números:** 26 testes rodaram, com 0 falhas, 0 erros e 0 pulados. Há 26 métodos escritos, os mesmos 26 da base: nenhum teste novo. A cobertura caiu de 93,8% para 80,0% nas linhas e de 87,1% para 69,6% nos ramos. Em `hotel.documents`, a cobertura é de 56 de 148 linhas e 6 de 16 ramos (na base, 30 de 30). Em `hotel.web`, são 32 de 68 ramos (na base, 18 de 28).
**Nota:** ruim

O agente não testou nada do que o pedido mudou:
- nenhum teste cria reserva com CNPJ ou com passaporte;
- nenhum confere a fatura nem o ISS de 5% (R$ 38,99);
- nenhum confere o booking receipt (`BRL 1,234.56`, as datas `2027-03-10`, "1 night");
- nenhum confere as recusas da identificação (nenhuma, duas, ou uma incompleta) nem os campos novos no JSON.

A cobertura confirma: a `CompanyInvoice` e a `InternationalBookingReceipt` não são executadas, e os ramos de validação novos da `HttpApi` (`HttpApi.java:132-155`) também não.

A pergunta da mutação deixa sem rede todas as regras novas:
- trocar `0.05` por `0.50` em `CompanyInvoice.java:43`, ou apagar a linha do ISS (`:57`), não quebra nenhum teste;
- trocar `Locale.US` por outro em `InternationalBookingReceipt.java:63` não quebra nenhum teste;
- inverter a ordem dos `if` em `DocumentFactory.java:13-17` também não.

Além disso, um teste da base foi enfraquecido. Antes, `ReservationServiceTest` conferia sem condição que o total do check-out era 779,80. Agora a conferência está dentro de `if (receipt instanceof ReservationReceipt rr)` (`ReservationServiceTest.java:145-147`). Se o check-out passasse a devolver outro tipo, o teste passaria sem conferir o total.

Os 26 testes da base continuam passando, mas a coluna da parte nova está ausente.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, a chave estrangeira tem índice e as datas estão em `date`. O `EXCLUDE` contra duas reservas na mesma noite continua lá. A `V1` ficou igual à da base, e a `V2` é nova. A tabela `reservations` passou a ter 20 colunas, 14 delas `NOT NULL` (`measures/database.md`).
**Nota:** com problemas

A evolução está feita do jeito certo: uma migração nova, `V2__add_company_and_international_support.sql`, só acrescenta colunas e relaxa uma restrição (`V2__...sql:2-8`), sem editar a `V1`. Um banco que já tem dados sobe sem problema. O SQL novo usa só parâmetros (`ReservationStore.java:32-55`), e a leitura traz os campos novos (`ReservationStore.java:130-137`).

O defeito: o agente tirou o `NOT NULL` de `guest_cpf` (`V2__...sql:8`) e não pôs nada no lugar. Antes, o banco garantia que toda reserva tinha uma identificação. Agora ele aceita uma reserva sem nenhuma identificação, com duas, ou com o CNPJ sem a razão social, porque a regra "exatamente uma" vive só na borda HTTP (`HttpApi.java:132-155`). Um `CHECK` (por exemplo, com `num_nonnulls`) protegeria os dados de qualquer outro caminho de escrita.

O resto vem da base, sem piora:
- as colunas de situação e de plano em `text` sem `CHECK`;
- o `SELECT ... ORDER BY id` da tabela inteira (`ReservationStore.java:117`).

## Comportamento

**Números:** 83 de 83 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- as duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo funciona como ele pediu:
- quem tem CPF recebe o mesmo comprovante de antes;
- a empresa recebe a fatura com a razão social e o CNPJ no lugar do CPF e o ISS retido de R$ 38,99 sobre R$ 779,80;
- o estrangeiro recebe o booking receipt em inglês, com `BRL`, as datas no formato ISO, "1 night" e a linha "Discount";
- o documento certo sai no e-mail (que continua em português), no check-out e na segunda via;
- os pedidos sem identificação, com duas, ou com uma incompleta são recusados com 400 INVALID_REQUEST;
- o que já funcionava continua funcionando, inclusive depois de reiniciar e com duas reservas ao mesmo tempo.

## Dúvidas

- **A classe do pattern.** A forma usada é uma fábrica concentrada, estática, que escolhe pelo tipo de hóspede. A referência a aceita como acerto quando os três lugares pedem o documento a ela, o que acontece. Dei `malfeito` porque a `HttpApi` volta a perguntar o tipo concreto com `instanceof` para serializar (`HttpApi.java:208-217`), e é esse o critério de "quem usa a estrutura ainda perguntando qual é o caso". Mas a decisão de qual documento criar está num lugar só. Um revisor que olhe só a criação daria `acerto`.
- **A nota da arquitetura.** Ficou em `com problemas` por causa da regra de identificação e do despacho por tipo na borda HTTP. Os pacotes e as dependências são exatamente os da base, e alguém poderia ler a validação da identificação como simples validação do formato da requisição, o que daria `bom`.
