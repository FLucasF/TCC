# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve a estrutura da base: `hotel.web` (Javalin, `HttpApi`) lê o JSON e chama o `ReservationService`
(`hotel.reservations`), que guarda a reserva pelo `ReservationStore` (JDBC sobre PostgreSQL, com o esquema criado por
migrações do Flyway). O documento continua saindo de `new ReservationReceipt(reservation, room)`. Para a mudança, ele
trocou o `cpf` do `Guest` por uma `Identification` com três implementações (`CpfIdentification`,
`CompanyIdentification`, `PassportIdentification`). Cada uma diz qual layout de documento lhe cabe (`layout()`), e o
layout é uma classe-base com o texto montado num lugar só (`DocumentLayout`) e três subclasses. No banco, a migração
nova `V2__guest_identification.sql` substitui `guest_cpf` por três colunas genéricas e copia o CPF das reservas que já
existiam. O roteiro passou em 83 de 83 conferências e os 33 testes passam. Não achei nenhum defeito sério. Há só
detalhes: um campo que ninguém usa em `Wording`, uma validação que lança exceção de dentro de um método que devolve
`boolean`, e uma coluna de tipo sem `CHECK`.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos números da base. Há um ciclo
(`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies-base.md`). As maiores
classes são `HttpApi` (173 linhas; 170 na base), `ReservationStore` (149; 143 na base) e `ReservationService` (138, sem
mudança).
**Nota:** bom

- **A borda HTTP continua fina.** `HttpApi` só passou a montar os campos de identificação e a entregá-los a
  `Identifications.fromRequest` (`src/main/java/hotel/web/HttpApi.java:128-130`). A regra "exatamente um jeito de
  identificar" mora em `hotel.reservations`, sem Javalin (`src/main/java/hotel/reservations/Identifications.java:35-46`).
- **O SQL continua isolado.** Ele ficou no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:31-52`
  e `:126-130`), e a leitura da linha do banco para o tipo de identificação passa por `Identifications.fromDatabase`
  (`Identifications.java:48-50`).
- **O ciclo veio da base.** Na base, `ReservationService` já criava o `ReservationReceipt` e o documento já lia a
  `Reservation`. O agente acrescentou mais um elo no mesmo sentido: o tipo de domínio conhece o layout do documento
  (`src/main/java/hotel/reservations/Identification.java:21`, `CpfIdentification.java:27-28`). O ciclo não piorou de
  tamanho, mas ficou um pouco mais apertado. Não pesa na nota.

## Design de baixo nível

**Números:** os 3 tipos de hóspede (o CPF, que já existia, a empresa e o estrangeiro) estão na estrutura, e nenhum
fica fora dela. Os 3 lugares que emitem o documento (o e-mail de confirmação, o check-out e a segunda via) pedem o
documento pelo mesmo caminho (`ReservationService.java:64`, `:109` e `:118`), e não há nenhum `if` nem `switch` sobre
o tipo de hóspede em todo o `src/main`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

- **O pattern.** A escolha de qual documento criar está num método polimórfico do próprio tipo de hóspede: a interface
  declara `Identification.layout()` (`Identification.java:21`), e cada tipo cria o seu layout (`CpfIdentification.java:27-28`
  com `ReceiptLayout`, `CompanyIdentification.java:27-28` com `InvoiceLayout`, `PassportIdentification.java:27-28` com
  `BookingReceiptLayout`). O único ponto que consulta o método é o construtor do documento
  (`src/main/java/hotel/documents/ReservationReceipt.java:32-37`), e os três lugares do serviço só chamam esse
  construtor. O CPF, que já existia, também entrou na estrutura: o texto antigo de `ReservationReceipt.text()` saiu
  e virou `ReceiptLayout`.
- **A agência.** Uma agência de turismo seria um novo `Identification` com o seu `layout()`, mais uma entrada em
  `Identifications.java:16-29`. O serviço e os três lugares não mudam.
- **O resto da estrutura.** O texto comum fica num lugar só, `DocumentLayout.text` (`DocumentLayout.java:25-46`). As
  subclasses só dão a identificação e o que vem depois do total, como o ISS em `InvoiceLayout.java:24-28`. Isso evita
  copiar o texto três vezes.
- **Sem exagero.** As classes novas (`Wording`, os layouts e `Identifications`) servem à variação pedida: a língua, o
  documento e a leitura do tipo. Não há interface com uma implementação só, nem camada que só repassa.
- **O resto do código.** O dinheiro continua em `BigDecimal`, e o ISS usa o mesmo arredondamento dos centavos
  (`InvoiceLayout.java:26`, com `PriceCalculator.cents` tornado público). As datas são `LocalDate`. Os detalhes que um
  revisor apontaria são pequenos e localizados:
  - O campo `Wording.guest` (`Wording.java:10`) nunca é lido. Os layouts escrevem "Hóspede:" e "Guest:" à mão
    (`ReceiptLayout.java:14`, `BookingReceiptLayout.java:14`).
  - `given` devolve um `boolean`, mas também lança a recusa do pedido incompleto (`Identifications.java:56-67`), o que
    esconde uma validação dentro de um teste de condição.
  - `fromDatabase` dá `NullPointerException` com um tipo desconhecido no banco (`Identifications.java:49`).
  - A interface ganha um `default` nulo para cada campo de cada tipo (`Identification.java:23-41`). É o preço do JSON
    plano que o texto pede.

## Testes

**Números:** 33 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 33 métodos de teste. A base tinha 26, todos
mantidos (só adaptados para `new CpfIdentification(...)`), e o agente acrescentou 7. Cobertura de linhas: 95,0%
(572 de 602; 93,8% na base). Cobertura de ramos: 88,8% (119 de 134; 87,1% na base). `hotel.documents` tem 73 de 73
linhas cobertas.
**Nota:** bom

- **A parte nova é conferida com o texto inteiro do documento,** com os valores do pedido:
  - o comprovante do CPF (`src/test/java/hotel/documents/ReservationDocumentTest.java:46-64`);
  - a fatura com o ISS de R$ 38,99 (`:66-87`);
  - o booking receipt com `BRL 779.80` e as datas ISO (`:89-108`);
  - o "1 night" e a linha `Discount:` (`:110-120`);
  - o e-mail em português em volta do documento (`:122-132`).
- **A API também é conferida:**
  - os campos nulos, a consulta e as recusas, que são nenhuma identificação, CNPJ sem a razão social, passaporte sem o
    país e dois jeitos ao mesmo tempo (`src/test/java/hotel/web/HttpApiTest.java:109-127`);
  - o campo `text` e os campos do documento pela segunda via (`:129-143`).
- **A pergunta da mutação.** Trocar 0.05 por outro valor, trocar o título ou a ordem das linhas, ou apagar o `if` do
  desconto faria algum teste falhar.
- **O que ficou sem rede:**
  - Nenhum teste confere o check-out devolvendo a fatura ou o booking receipt. O caminho é o mesmo da segunda via,
    então o risco é baixo.
  - Nenhum teste confere a cópia do `guest_cpf` antigo na migração V2.
  - O ISS não é testado num valor em que o arredondamento meio para o par faça diferença.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, e nenhuma chave estrangeira
está sem índice. Nenhuma data é guardada como texto. Há 2 migrações: a V1 está igual à da base e a V2 é nova. A única
coluna nova acusada é `reservations.identification_type`, que é `text` sem `CHECK`. As outras colunas sem `CHECK`
(`status`, `rate_plan`, `channel`, `rooms.type`) vêm da base.
**Nota:** bom

- **O esquema evolui por migração, sem editar o que já existia.** A `V2__guest_identification.sql` (linhas 3-13) faz
  quatro passos: acrescenta as colunas, copia o CPF das reservas antigas para `identification_number` com o tipo `CPF`,
  põe `NOT NULL` no tipo e no número, e só então apaga `guest_cpf`. Quem já roda o sistema não perde dado.
- **O SQL continua com parâmetros** (`ReservationStore.java:31-52`). Os `+` acusados pelo script só juntam constantes
  (`COLUMNS`).
- **Os pontos fracos são pequenos:**
  - O modelo genérico (`identification_type`/`_number`/`_detail`) não deixa o banco garantir que a empresa e o
    estrangeiro tenham o `detail` preenchido.
  - O tipo não tem `CHECK`, e um valor inesperado quebraria a leitura (`Identifications.java:49`).
  - A proteção contra reserva dupla é a restrição `EXCLUDE` da base, que o agente manteve.

## Comportamento

**Números:** 83 de 83 conferências passaram, com 0 falhas.
- A parte comum: 46 de 46.
- Os dados depois de reiniciar: 3 de 3.
- O cenário 05: 32 de 32.
- As reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona:
- a pessoa com CPF continua recebendo o mesmo comprovante;
- a empresa recebe a fatura, com a empresa no lugar do CPF e o ISS retido de R$ 38,99 sobre R$ 779,80;
- o estrangeiro recebe o booking receipt em inglês, com `BRL 2,783.52`, as datas `2027-03-10`, "1 night" e a linha
  `Discount:`;
- o documento sai no e-mail, que continua em português, no check-out e na segunda via;
- os pedidos sem identificação, com dois jeitos ou com um jeito incompleto são recusados com 400 `INVALID_REQUEST`;
- o que já funcionava continua funcionando, inclusive depois de reiniciar o serviço e com duas reservas ao mesmo
  tempo.

## Dúvidas

- **A forma do pattern.** É um método polimórfico no tipo de hóspede: `Identification.layout()` cria o documento certo.
  Por isso a classe é acerto, e não uma fábrica separada. O nome do método fala do layout, e não do documento inteiro
  (`ReservationReceipt`), mas a escolha mora nele.
- **A migração V2 num banco com dados.** O script mede um banco vazio. Pela leitura, a V2 trata as reservas antigas
  copiando o CPF antes de apagar a coluna, mas isso não foi executado sobre dados reais.
