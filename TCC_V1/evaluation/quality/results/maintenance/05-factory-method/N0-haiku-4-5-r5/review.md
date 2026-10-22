# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em sete pacotes. O `hotel.web.HttpApi` lê a requisição e chama o
`hotel.reservations.ReservationService`, que usa o `ReservationStore` (JDBC, PostgreSQL com migrações do Flyway). O
documento (`hotel.documents.ReservationReceipt`) é criado com `new` nos três lugares de sempre: `ReservationService.java:64`
(e-mail de confirmação), `:109` (check-out) e `:118` (segunda via). O agente fez a mudança sem criar nenhuma classe.
Acrescentou quatro campos que podem ser nulos em `Guest`, no pedido, na resposta, no documento e numa migração nova
(`V2__add_identification_fields.sql`). Pôs a regra "um jeito de identificar e só um" no `HttpApi`
(`HttpApi.java:127-146`). Passou a escolha do documento para dentro do construtor do próprio `ReservationReceipt`, com
dois `if` iguais sobre os campos nulos (`ReservationReceipt.java:51-69`) e três funções que montam o texto, quase cópias
uma da outra. Os 83 pontos do roteiro passam e os 34 testes também. O problema mais sério está fora do roteiro: a
validação considera que um campo em branco não foi informado (`isBlank`), mas a escolha do documento só olha se o campo
é `!= null`. Por isso, um hóspede com CPF cujo site mande `"companyCnpj": ""` recebe uma "Fatura de hospedagem" com
"Empresa:  (CNPJ )", no e-mail e no check-out. Confirmei isso chamando a API.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base (`measures/dependencies.md` e
`measures/dependencies-base.md` mostram as mesmas arestas). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que
já existia na base. Tamanhos: `hotel.web` passou de 236 para 275 linhas, `hotel.documents` de 79 para 166 e
`hotel.reservations` de 508 para 532. Maior classe: `HttpApi` (198 linhas).
**Nota:** bom

O agente respeitou as camadas da base. O `HttpApi` continua só lendo a requisição e montando o `Guest`
(`HttpApi.java:109-158`). A validação "exatamente uma identificação" (`HttpApi.java:127-146`) fica junto da conferência
de campos obrigatórios que a base já fazia ali (`HttpApi.java:116-125`), o que é coerente com o resto do código. O
acesso ao banco continua só no `ReservationStore` (`ReservationStore.java:31-60` e `:129-137`). O documento continua
sem Javalin e sem JDBC (`ReservationReceipt.java:1-8`). Nenhuma dependência nova entre pacotes. O ciclo
documents↔reservations vem da base: o `ReservationService` cria o `ReservationReceipt`, e este lê `Reservation`. Um
ponto pequeno: a regra que decide o tipo de hóspede não existe como conceito do domínio. Ela aparece uma vez na borda
HTTP e outra no documento (ver Design). Isso pesa no design, não nas camadas.

## Design de baixo nível

**Números:** os 3 lugares que emitem o documento (`ReservationService.java:64`, `:109`, `:118`) pedem todos ao mesmo
construtor, e nenhum pergunta qual é o tipo de hóspede. Os 3 casos (comprovante, fatura, booking receipt), inclusive o
que já existia, saem do mesmo lugar. A decisão sobre o tipo aparece em 2 `if`/`else if` iguais dentro do documento
(`ReservationReceipt.java:51-59` e `:61-69`) e numa terceira forma, diferente, na validação (`HttpApi.java:128-130`).
As 3 funções de texto (`:71`, `:89`, `:113`) repetem quase linha por linha o quarto, as datas, as diárias, o desconto
e o total. Nenhuma classe ou interface nova.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Na base, o comprovante era montado com `new` em três lugares do serviço. O agente concentrou a escolha
num lugar só: o construtor `ReservationReceipt(Reservation, Room)` (`ReservationReceipt.java:30-49`) decide pelo tipo
de hóspede qual título e qual texto produzir. Os três lugares continuam chamando só esse construtor, sem nenhum `if`
sobre o hóspede. É a forma de "fábrica concentrada" que a referência aceita como acerto (um método que escolhe o
documento pelo tipo de hóspede). Também não é o caso de malfeito descrito na referência, em que a fábrica devolve
sempre o mesmo documento e a escolha acontece fora dela. Uma agência entraria como um ramo a mais em `generateText` e
uma função de texto, sem mexer no serviço.

O que afasta do ideal:
- o tipo de hóspede é implícito, deduzido de qual campo é nulo (`ReservationReceipt.java:52,54,62,64`);
- a decisão está escrita duas vezes (`determinateTitle` e `generateText`) e pode divergir;
- o título calculado em `determinateTitle` é escrito de novo, à mão, dentro de cada texto
  (`ReservationReceipt.java:73`, `:91`, `:115`);
- o "documento" é um único record com um `text` pronto, e não um tipo por documento.

**Exagero.** Não há estrutura sem necessidade: nenhuma interface, fábrica ou camada nova.

**O resto do código.**
- **Regra duplicada e incoerente.** A validação trata o branco como ausência (`HttpApi.java:128-130`, `isBlank`), mas
  guarda o valor como veio (`HttpApi.java:148-155`). O documento decide por `!= null` (`ReservationReceipt.java:52`,
  `:62`). Resultado, conferido pela API: um pedido com `guestCpf` e `"companyCnpj": ""` é aceito, a consulta mostra
  `"companyCnpj": ""` em vez de null, e a segunda via (e o e-mail) vira "Fatura de hospedagem nº 1 / Empresa:  (CNPJ )
  … ISS retido". Com `"guestPassport": " "`, sai um "Booking receipt" com "(passport  , null)". É um dado errado no
  documento do hóspede, nascido de a regra "qual é o tipo de hóspede" estar em dois lugares com critérios diferentes.
- **Duplicação.** `generateReceipt` e `generateInvoice` (`ReservationReceipt.java:71-87` e `:89-111`) são iguais da
  linha do quarto ao total. `generateBookingReceipt` (`:113-131`) repete a mesma estrutura em inglês.
- **Arredondamento.** O ISS faz o próprio arredondamento (`ReservationReceipt.java:106-107`, `HALF_EVEN`) em vez de
  reutilizar `PriceCalculator.cents` (`PriceCalculator.java:50-52`). O modo é o mesmo, então o valor está certo, mas a
  regra "como sempre" fica copiada.
- **Formatação.** Os arquivos tocados passaram a misturar tabulação e espaços: 112 linhas com tab em
  `ReservationReceipt.java`, 43 em `ReservationResponse.java` e 10 em `Guest.java`, contra espaços no resto do projeto.
  Além disso, o Javadoc de `CreateReservationRequest` foi apagado sem substituto.
- **O que está bem.** `BigDecimal` e `LocalDate` continuam nos tipos certos. As funções novas de `TextFormat`
  (`TextFormat.java:27-33`) são pequenas e bem nomeadas. O construtor de compatibilidade `Guest(name, email, cpf)`
  (`Guest.java:12-14`) evita mexer nos testes da base.

## Testes

**Números:** 34 testes rodaram, 0 falhas, 0 erros, 0 pulados, com 34 métodos escritos (a base tinha 26: o agente
acrescentou 8, todos em `HttpApiTest`). Cobertura de linhas 93,9% (555/591; base 93,8%) e de ramos 85,0% (136/160;
base 87,1%). `hotel.documents`: 83/86 linhas e 16/20 ramos. `hotel.web`: 41/56 ramos (de `measures/tests.md` e
`measures/tests-base.md`).
**Nota:** com problemas

O agente manteve os 26 testes da base passando, sem apagar nem enfraquecer nenhum, e testou a parte nova pela API:
- os três jeitos de criar com os campos que não se aplicam vindo null (`HttpApiTest.java:124`, `:134`, `:150`);
- as recusas de duas identificações e de nenhuma, com o código INVALID_REQUEST (`:166`, `:178`);
- a primeira linha e a linha de identificação da fatura e do booking receipt (`:204`, `:224`).

As asserções do que é novo são fracas onde está a regra:
- o ISS só confere que a linha "ISS retido (5%)" existe (`HttpApiTest.java:220`), não o valor R$ 38,99;
- o booking receipt só confere que o texto contém "BRL" (`:241`), não "BRL 779.80" nem o milhar com vírgula;
- não há teste de "1 night", da linha "Discount:", da recusa de CNPJ sem razão social ou de passaporte sem país, nem do
  documento novo no e-mail ou na segunda via;
- não há teste de unidade do `ReservationReceipt`.

Pela pergunta da mutação, nenhum teste falharia se alguém:
- trocasse `0.05` por `0.50` (`ReservationReceipt.java:106`);
- trocasse `Locale.US` por `pt-BR` em `TextFormat.internationalMoney` (`TextFormat.java:28`);
- trocasse `" night"` por `" nights"` (`ReservationReceipt.java:123`);
- apagasse as conferências de par incompleto (`HttpApi.java:139-146`).

Também não há teste com campo em branco, que pegaria o defeito descrito em Design.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` (6 colunas, todas ok), nenhuma chave
estrangeira sem índice, nenhuma data em texto. EXCLUDE contra reserva dupla, que veio da base. Status, plano e tipo em
`text` sem CHECK, também da base. Migrações: `V1__create_tables.sql` igual à da base e `V2__add_identification_fields.sql`
nova (de `measures/database.md`).
**Nota:** com problemas

O agente evoluiu o esquema do jeito certo: uma migração nova, sem editar a V1, com quatro colunas `text` que podem ser
nulas (`V2__add_identification_fields.sql:1-5`). O SQL novo usa só parâmetros (`ReservationStore.java:32-55`), e a
leitura traz os campos novos (`ReservationStore.java:130-137`). A V2, porém, tira o `NOT NULL` de `guest_cpf`
(`V2__add_identification_fields.sql:7-8`; na base, `V1__create_tables.sql:24`) e não põe nada no lugar. Antes, o banco
garantia que toda reserva tinha identificação. Agora ele aceita uma reserva sem nenhuma, ou com CPF e CNPJ juntos, ou
com CNPJ sem razão social. A regra fica só no `HttpApi`. Um
`CHECK (num_nonnulls(guest_cpf, company_cnpj, guest_passport) = 1)` com as combinações dos pares manteria a proteção
que a base tinha. Também não há normalização de branco para NULL, e por isso `""` vai para o banco (ver Design). É um
afrouxamento localizado: nada se perde e os dados existentes migram sem problema.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam (`measures/behavior.md`):
- a parte comum: 46/46;
- reiniciar o serviço: 3/3;
- o cenário 05: 32/32;
- duas reservas ao mesmo tempo: 2/2.
**Nota:** bom

O que o dono pediu funciona pela API:
- o hóspede com CPF continua recebendo o mesmo comprovante;
- a fatura mostra a empresa e o ISS de R$ 38,99 sobre R$ 779,80;
- o booking receipt sai em inglês, com "BRL 2,783.52", datas 2027-03-10, "1 night" e a linha "Discount:";
- o e-mail continua em português e leva o documento certo, e o check-out e a segunda via devolvem o mesmo documento,
  com o campo `text` e os quatro campos novos;
- nenhuma identificação, duas juntas ou um par incompleto dão 400 INVALID_REQUEST.

Os dados sobrevivem ao reinício e a reserva dupla simultânea é recusada.

Fora do roteiro, chamando a API com o serviço de pé, achei um defeito: se o site mandar os campos que não se aplicam
como texto vazio (`"companyCnpj": ""`) em vez de omiti-los, a reserva com CPF é aceita, mas o hóspede recebe uma fatura
com "Empresa:  (CNPJ )" e ISS retido. A causa é de design (a escolha do documento por `!= null` contra a validação por
`isBlank`) e foi contada lá, sem mudar esta nota. Para o dono da pousada, vale combinar com o pessoal do site que os
campos que não se aplicam sejam omitidos ou enviados como null.

## Dúvidas

- **A classe do pattern.** Dei `acerto` porque a escolha do documento está num lugar só, o construtor de
  `ReservationReceipt` (`generateText`), que os três lugares chamam sem perguntar o tipo. Essa é a forma de "fábrica
  concentrada num método", e não o Factory Method clássico nem um método polimórfico no hóspede. É a forma mais fraca
  das aceitas: um único record com o texto pronto, o tipo de hóspede deduzido de campos nulos e a decisão escrita duas
  vezes no documento e de outro jeito na validação. Um revisor mais rígido poderia chamar isso de `malfeito`.
- **A nota do banco.** Ficou entre `bom` e `com problemas`. O peso está todo em ter tirado o `NOT NULL` de
  `guest_cpf` sem uma restrição que o substitua.
