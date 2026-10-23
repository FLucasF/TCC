# Revisão: manutenção, cenário 05

## Visão geral

A base já tinha a forma que o agente manteve: `hotel.web` (Javalin, `HttpApi` lê o JSON e chama o serviço),
`hotel.reservations` (as regras no `ReservationService` e o JDBC isolado no `ReservationStore`), `hotel.documents` (o
comprovante), `hotel.messages`, `hotel.reports` e `hotel.rooms`, tudo montado em `hotel.Hotel`. Os dados ficam no
PostgreSQL, e o esquema evolui pelo Flyway. Para os três tipos de hóspede, o agente criou um tipo selado
`Identification` (`Cpf`, `Company`, `Passport`, em `src/main/java/hotel/reservations/Identification.java:4-17`),
acrescentou quatro colunas por uma migração nova (`V2__guest_identification.sql`) com um CHECK de "um jeito e só um",
validou a requisição em `HttpApi.readIdentification` (`HttpApi.java:133-151`) e fez o `ReservationReceipt` escolher o
título e o texto do documento. O comportamento está todo certo (83 de 83 conferências passaram, 32 de 32 do cenário),
e os testes passam (28 de 28). O ponto mais fraco é o design da variação: existe um único record de documento, e o texto
de cada tipo é decidido por `if`s sobre campos nulos dentro de um método `render` de 16 parâmetros. Por isso, o
documento das agências não vai caber sem mexer nesses `if`s.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tinha os
mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo (`measures/dependencies-base.md`). A maior classe é
`HttpApi` (de 170 para 191 linhas), e `ReservationReceipt` foi de 55 para 121 linhas.
**Nota:** bom

As camadas continuam no lugar. A rota de criação só lê, chama e responde (`HttpApi.java:65-68`). A identificação é
lida e validada na borda (`HttpApi.java:133-151`), o que é coerente com o resto da validação da requisição, que a base
já fazia ali (`HttpApi.java:117-126`). O JDBC das colunas novas fica só no `ReservationStore`
(`ReservationStore.java:32-55` e `129-137`), e a resposta só copia os campos (`ReservationResponse.java:35-41`). O
ciclo `documents` ↔ `reservations` veio da base (`ReservationService.java:3` importa o `ReservationReceipt`, que já lia
a `Reservation`). O agente reforçou o ciclo, sem criá-lo: o documento agora importa `Guest`, `Identification` e
`PriceCalculator` (`ReservationReceipt.java:3-5`), e `PriceCalculator.cents` virou público só para isso
(`PriceCalculator.java:50`). O peso disso é pequeno, num ciclo que já existia.

## Design de baixo nível

**Números:** 3 tipos de hóspede (2 novos e o CPF, que já existia), todos modelados no tipo selado `Identification`. A
decisão sobre o documento fica num lugar só (`ReservationReceipt`), e os 3 lugares que o emitem
(`ReservationService.java:64`, `109`, `118`) continuam iguais. Dentro do documento, porém, a escolha acontece 4 vezes:
um `switch` sobre o tipo (`ReservationReceipt.java:71-77`) e três `if`s sobre campos nulos (`:84`, `:100`, `:115`).
Há um único tipo de documento para os três casos.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O que aproxima do pattern:** o agente não espalhou o `if` pelos três lugares que emitem o documento. Confirmação,
check-out e segunda via continuam chamando `new ReservationReceipt(reservation, room)`, e a escolha mora numa classe.
O tipo selado `Identification` é uma boa estrutura de domínio, e o `switch` exaustivo de `titleFor`
(`ReservationReceipt.java:71-77`) faria o compilador apontar um caso novo.

**O que afasta:** não há uma fábrica nem um documento por tipo. O construtor sempre devolve o mesmo
`ReservationReceipt`, e o texto é escolhido depois, em `render` (`ReservationReceipt.java:79-120`). Essa escolha já não
usa a estrutura: ela pergunta se `passport != null` (`:84`) e se `cnpj != null` (`:100`, `:115`). A estrutura e o
jeito sem ela convivem. Uma agência acrescentada como `Identification.Agency` faria o compilador cobrar só o `titleFor`.
O `render` cairia em silêncio no ramo do CPF e imprimiria "CPF null" (`:103-104`). O README do agente diz que "o
compilador aponta onde falta tratar", e isso não vale para o texto. O `Guest` também pergunta o tipo campo a campo, com
`instanceof` (`Guest.java:10-28`). Isso se justifica para achatar as colunas e o JSON, mas reforça o mesmo jeito de
perguntar o tipo.

**Exagero:** nenhum. `Identification` tem três implementações reais, e não há interface, fábrica ou camada sem uso.

**O resto do código:**
- `render` recebe 16 parâmetros (`ReservationReceipt.java:79-82`) e junta dois blocos quase paralelos, um em inglês e
  um em português (`:84-98` e `:99-119`), cada um com a sua cópia das linhas de quarto, datas, diárias, desconto e
  total.
- O `text` é um componente público do record, preenchido no construtor compacto só quando vem `null`
  (`ReservationReceipt.java:63-69`). O construtor canônico público aceita, portanto, um texto que contradiz os outros
  campos.
- O ISS é calculado só dentro do texto (`:115-118`) e não aparece como dado no documento.
- O que está bem: `BigDecimal` com o arredondamento comum (`PriceCalculator.cents`), `LocalDate` e o tipo selado no
  lugar de texto solto.

## Testes

**Números:** 28 testes rodaram, 0 falhas, 0 erros e 0 pulados; 28 métodos escritos. A base tinha 26, e nenhum foi
apagado ou mudado (o patch só acrescenta). A cobertura é de 94,8% das linhas (542 de 572) e 87,7% dos ramos (157 de
179). Na base, era 93,8% e 87,1%. Em `hotel.documents`, são 66 de 66 linhas e 16 de 19 ramos (`measures/tests.md`).
**Nota:** bom

O agente testou a parte nova com valores e em dois níveis:
- **Contra o banco:** `ReservationServiceTest.java:159-196` confere o texto inteiro da fatura, com o exemplo do pedido
  e o "ISS retido (5%): R$ 38,99" (`:165-174`). Confere também o texto inteiro do *booking receipt* com uma noite e
  desconto: "1 night", "BRL 579.90", "Discount: BRL 86.98" (`:182-190`). Por fim, confere os campos nulos (`:192-193`)
  e que o e-mail leva o documento em inglês (`:195`).
- **Pela API:** `HttpApiTest.java:92-122` confere as quatro recusas (nenhuma identificação, CPF com empresa, empresa
  com passaporte, CNPJ sem razão social: `:99-102`), os `null` na consulta e no documento (`:108-114`) e o ISS de
  R$ 46,79 (`:117`).

Pela pergunta da mutação, a taxa de 5% e o arredondamento do ISS, o formato `BRL x.xx` e a data ISO ficam cobertos. As
lacunas:
- trocar `" nights"` por `" night"` (`ReservationReceipt.java:91`) passa, porque o teste em inglês só usa uma noite;
- o separador de milhar em inglês não é conferido, porque nenhum valor passa de mil;
- o check-out dos tipos novos não é conferido;
- o "passaporte sem o país" não é recusado em nenhum teste.

São lacunas pequenas perto do que está conferido.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, nenhuma chave estrangeira está
sem índice e nenhuma data está em texto. 2 migrações: a `V1` igual à da base e a `V2` nova. A conferência acusou só as
colunas de situação e de tipo sem CHECK (`status`, `rate_plan`, `type`, `channel`), que vieram da base
(`measures/database.md`).
**Nota:** bom

O agente mudou o esquema do jeito certo, com uma migração nova (`V2__guest_identification.sql:2-14`), sem tocar na
`V1`. A migração:
- tira o `NOT NULL` do `guest_cpf`;
- acrescenta as quatro colunas;
- protege a regra "um jeito e só um, completo" com um CHECK no próprio banco (`V2__guest_identification.sql:8-14`).

As reservas que já existiam (todas com CPF) continuam válidas sob o CHECK. O SQL continua com parâmetros
(`ReservationStore.java:32-55`), e a leitura reconstrói a identificação a partir das colunas
(`ReservationStore.java:129-137`). As concatenações que o script listou só juntam constantes (`COLUMNS`, `:18-20`). A
duplicação de reserva continua protegida pelo `EXCLUDE` da base. A consulta `all()` traz a tabela inteira
(`ReservationStore.java:114-117`), mas isso veio da base.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam (`measures/behavior.md`):
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- **O hóspede com CPF** recebe o mesmo comprovante de antes.
- **A empresa** recebe a fatura com a razão social e o CNPJ no lugar do CPF e o ISS retido de R$ 38,99 sobre
  R$ 779,80, no e-mail em português, no check-out e na segunda via.
- **O estrangeiro** recebe o *booking receipt* com datas `2027-03-10`, "1 night", a linha "Discount" e
  `BRL 2,783.52`.
- **As recusas:** sem identificação, com duas, ou com uma incompleta, a resposta é 400 `INVALID_REQUEST`.
- **O que já existia:** as reservas, os relatórios, os dados depois de reiniciar e a trava contra duas reservas
  simultâneas continuam funcionando.

## Dúvidas

- **A classe do pattern** ficou entre `malfeito` e `omissão`. A referência chama de omissão o `if` repetido nos três
  lugares que emitem o documento, e isso não acontece aqui: a decisão está concentrada no `ReservationReceipt`. Também
  não há fábrica nem documento por tipo: é um único record cujo texto se decide por `if`s sobre campos nulos, ao lado
  de um tipo selado que só o título usa. Escolhi `malfeito` porque a estrutura existe (`Identification` e o `switch` de
  `titleFor`) e convive com o jeito sem ela (os `if`s de `render`). Isso combina com a descrição de malfeito: "a
  fábrica devolve sempre o mesmo documento e a escolha acontece depois".
- **A forma**, que a referência pede para registrar: não é nenhuma das três que ela aceita como acerto. Não há
  criador com método sobrescrito, nem uma fábrica que escolha entre documentos distintos, nem método polimórfico no
  tipo de hóspede. É um construtor único com ramificação interna.
