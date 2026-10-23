# Revisão: construção, cenário 05

## Visão geral

O projeto tem seis pacotes, montados em `App.java:20-60`: `controller` (os handlers do Javalin), `api` (os DTOs de entrada e de saída), `service` (as regras de reserva, preço, relatórios e mensagens), `model` (o domínio, com `Reservation` e uma máquina de estados simples), `repository` (JDBC puro, com `DriverManager`) e o pacote raiz. Uma requisição passa pelo `ReservationController`, que lê o corpo, valida e escolhe o tipo de identificação do hóspede. Depois vem o `ReservationService`, que usa o `PricingService`, o `ReservationRepository` e o `MessageService`. Os dados ficam em três tabelas que `Database.initialize` cria com `CREATE TABLE IF NOT EXISTS`. A variação do cenário (o documento por tipo de hóspede) é um método polimórfico no tipo de identificação, e as 32 conferências do cenário passaram. Há três problemas sérios:
- **Reserva dupla:** nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos. Não há restrição no banco, nem transação, nem trava, e as duas reservas foram criadas em 10 de 10 rodadas.
- **Quase sem testes:** só há 7 testes, todos do preço, que cobrem 3,3% das linhas.
- **Erro 500:** um corpo que não é JSON derruba a requisição com 500.

## Arquitetura

**Números:** 6 pacotes, 11 dependências entre eles, nenhum ciclo. O Javalin aparece só em `controller` e no pacote raiz, o JDBC só em `repository`, e `service` e `model` não usam nenhuma biblioteca de borda. A maior classe é `ReservationRepository`, com 239 linhas. Depois vêm `ReservationService` (176) e `ReservationController` (162).
**Nota:** bom

As dependências apontam da borda para as regras e das regras para os dados (`controller -> service -> repository -> model`), sem ciclo. A montagem fica num lugar só, `App.java:20-37`, e cada peça recebe as suas dependências pelo construtor. As regras não conhecem o Javalin nem o SQL:
- o preço fica em `PricingService.java:12-57`;
- as transições de estado e a devolução ficam em `Reservation.java:86-120`;
- o fluxo fica em `ReservationService.java:28-139`.

Todo o SQL mora no pacote `repository`.

Há pequenas misturas, que não chegam a defeito de camada:
- `RoomController.java:10-17` fala direto com o `RoomRepository`, sem passar por um serviço. É aceitável para uma listagem.
- O handler de criação decide qual identificação montar (`ReservationController.java:26-48`), e a mesma decisão volta a aparecer, a partir das colunas, em `ReservationRepository.java:212-218`.
- O mapeamento de exceção para código HTTP se repete nos seis handlers (`ReservationController.java:70-160`), em vez de um `app.exception(...)`.
- Não há tratador de erro global, e é por isso que o corpo inválido vira 500 (`ReservationController.java:16`).

O tamanho das classes é equilibrado.

## Design de baixo nível

**Números:** 3 de 3 tipos de documento ficam na estrutura (`GuestIdentification.generateDocument`, com 3 implementações). Os 3 lugares que emitem o documento (o e-mail de confirmação, o check-out e a segunda via) pedem o documento ao mesmo método, `Reservation.generateDocument` (`ReservationService.java:86`, `:126`, `:138`). Nenhum `if` sobre o tipo de documento existe fora da estrutura.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A referência aceita como acerto "um método polimórfico no próprio tipo de hóspede". É o que o agente fez:
- `GuestIdentification.java:13-24` declara `generateDocument(...)`;
- `CpfIdentification.java:41-79` monta o comprovante;
- `CnpjIdentification.java:45-87` monta a fatura com o ISS de 5% (`:49`);
- `PassportIdentification.java:45-84` monta o booking receipt;
- `Reservation.java:122-128` delega à identificação.

Os três pontos de emissão chamam `reservation.generateDocument(room.getType())`, sem perguntar qual é o tipo. Uma agência de turismo seria uma quarta implementação de `GuestIdentification`, sem mexer no serviço. O `if` que existe (`ReservationController.java:29-41` e `ReservationRepository.java:212-218`) escolhe qual identificação construir a partir da entrada, e não qual documento emitir. Ele aparece em dois pontos, a borda e a leitura do banco, e uma quarta forma exigiria mexer nos dois. Isso afasta um pouco do ideal, mas não é a decisão que o pattern concentra.

**O exagero.** Não há interface com uma implementação só, nem fábrica ou camada que só repassa. Os DTOs de `api/` separam o JSON do domínio, o que é justificável.

**O resto do código** tem defeitos localizados:
- **Duplicação nos documentos:**
  - `formatMoney` e `DATE_FORMATTER` foram copiados em `CpfIdentification.java:9,81-83` e `CnpjIdentification.java:11,89-91`;
  - o corpo do comprovante e o da fatura são quase iguais (`CpfIdentification.java:45-58` e `CnpjIdentification.java:51-65`);
  - nada extrai a parte comum (quarto, datas, diárias, desconto, total).
- **Formatação frágil:** `String.format("R$ %,.2f", ...)` usa o locale padrão da JVM e depois troca os separadores (`CpfIdentification.java:82`, `CnpjIdentification.java:90`). Numa JVM em `pt_BR` o resultado sairia invertido (`R$ 1,234.56`).
- **A interface vaza os campos:** `GuestIdentification.java:7-11` obriga cada tipo a expor os cinco campos de identificação, e cada implementação devolve `null` em quatro deles (por exemplo, `CnpjIdentification.java:19-42`). Uma quarta forma teria de acrescentar um getter em todas.
- **Construtores enormes:** `Document` tem um construtor de 17 parâmetros posicionais (`Document.java:25-31`, com chamadas como `CnpjIdentification.java:68-86`). `Reservation` tem dois construtores quase iguais (`Reservation.java:24-67`).
- **Erros:** os repositórios embrulham toda `SQLException` num `RuntimeException` genérico (por exemplo, `ReservationRepository.java:76-79`). O dinheiro de "nada pago" nasce como `BigDecimal.ZERO`, sem escala (`Reservation.java:42`), e por isso a resposta de criação traz `"amountPaid":0`.

Os tipos do domínio estão certos: `BigDecimal` com `HALF_EVEN` (`PricingService.java:14,29,33,37`), `LocalDate` e `enum` para status, plano e tipo de quarto.

## Testes

**Números:** 7 testes rodaram, 0 falharam. 7 métodos de teste escritos, num único arquivo (`PricingServiceTest.java`). Cobertura: 30 de 921 linhas (3,3%) e 14 de 142 ramos (9,9%). Não há teste pela API nem contra o banco, e os pacotes `controller`, `repository` e `api` têm 0% de cobertura.
**Nota:** ruim

Os testes que existem são bons e usam valores:
- a noite da alta temporada a 299,88 (`PricingServiceTest.java:21`);
- o exemplo do texto, 599,76 (`:40`);
- o desconto não reembolsável de 89,96 (`:47`);
- a borda de 14/15 de dezembro (`:61-67`).

Mas a coluna está quase ausente. Nada confere a parte nova do cenário:
- o texto dos três documentos;
- o ISS de 5% (`CnpjIdentification.java:49`);
- o formato `BRL 1,234.56`;
- "1 night" contra "N nights";
- a regra de "exatamente uma identificação".

Também ficam sem teste as transições de estado e a devolução (`Reservation.java:86-120`), o conflito de datas, os relatórios e os códigos de erro. A pergunta da mutação não encontra rede em nenhum ponto do cenário. Ninguém notaria se alguém trocasse `0.05` por `0.5` em `CnpjIdentification.java:12`, apagasse a linha `Discount:` (`PassportIdentification.java:59-61`) ou trocasse o `STANDARD` por `NON_REFUNDABLE` na devolução (`Reservation.java:115`).

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram:
- a chave estrangeira `reservations(room_number) -> rooms` sem índice;
- nenhuma restrição `CHECK`, `UNIQUE` ou `EXCLUDE`;
- status, plano, tipo e canal em `varchar` sem `CHECK`;
- 0 pool de conexões, 0 transações explícitas e 0 `FOR UPDATE`.

O esquema é criado com `CREATE TABLE` dentro do código. O único SQL montado com `+` (`ReservationRepository.java:132`) concatena uma constante.
**Nota:** ruim

Os tipos estão certos, o SQL usa parâmetros e as conexões são fechadas com try-with-resources. O problema sério é que a regra central do texto ("um quarto não pode ter duas reservas na mesma noite") não tem proteção nenhuma:
- `ReservationService.java:47` consulta os conflitos (`ReservationRepository.java:125-153`);
- `ReservationService.java:62` grava, em outra conexão e sem transação;
- não há restrição de exclusão no banco, nem trava, nem sequer um `synchronized`.

O roteiro do comportamento confirmou o defeito: duas reservas do mesmo quarto nas mesmas noites foram criadas em 10 de 10 rodadas. É dado errado no banco (quarto vendido duas vezes).

Além disso:
- o esquema não evolui: `CREATE TABLE IF NOT EXISTS` em `Database.java:27-68`, sem migração versionada, não sabe acrescentar uma coluna numa tabela que já existe;
- a chave estrangeira de `Database.java:56` não tem índice;
- cada operação abre uma conexão nova com `DriverManager` (`Database.java:19-21`), sem pool;
- a confirmação grava a reserva (`ReservationService.java:79`) e a mensagem (`MessageService.java:29`) em conexões separadas, sem transação, e uma falha no meio deixa a reserva confirmada sem o e-mail registrado.

## Comportamento

**Números:** 78 de 83 conferências passaram:
- parte comum: 42 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 05: 32 de 32;
- reservas simultâneas: 1 de 2.

**Nota:** com problemas

O cenário funciona por inteiro: os três documentos, com o ISS de R$ 38,99, o booking receipt em BRL com as datas ISO, "1 night" e a linha Discount, e as recusas de identificação ausente, dupla ou incompleta. Os dados sobrevivem ao reinício.

As falhas:
- **Duas reservas ao mesmo tempo:** as duas foram criadas em 10 de 10 rodadas. Para o dono da pousada, o mesmo quarto pode ser vendido duas vezes se dois pedidos chegarem juntos pelo site. A causa está no banco (veja acima).
- **Corpo que não é JSON:** volta 500 ("Server Error") em vez de 400 `INVALID_REQUEST` (conferi com `curl`). `ctx.bodyAsClass` (`ReservationController.java:16`) lança uma exceção que ninguém trata. O site recebe um erro genérico sem o formato combinado.
- **`amountPaid` na criação:** vem como `0`, e não `0.00` como o texto pede ("número com 2 casas"). Ele nasce de `BigDecimal.ZERO` (`Reservation.java:42`). Na consulta posterior, lido do banco, vem `0.00`, então a mesma reserva aparece de dois jeitos.
- **O 201 comporta 3 e a diária do 201 é 389.90:** não pesam. O sistema devolve `number` como texto (`"201"`), e o roteiro procura o número `201`. Pelo `curl`, `GET /rooms` traz `"capacity":3` e `"nightlyRate":389.90` corretos. O texto não fixa o tipo de `number`, então é uma escolha razoável.

Fora do roteiro, notei que um pedido com `guestCpf` e um `companyName` solto é aceito (201), porque a contagem de identificações em `ReservationController.java:43-45` ignora `companyName` e `guestCountry`. É um desvio pequeno da regra "só um jeito".

## Dúvidas

- O pattern foi feito na forma "método polimórfico no tipo de hóspede" (`GuestIdentification.generateDocument`), que a referência conta como acerto. A escolha de qual identificação construir continua num `if` repetido na borda (`ReservationController.java:29-41`) e na leitura do banco (`ReservationRepository.java:212-218`). Não a tratei como malfeito porque não é a escolha do documento, mas um revisor mais rígido poderia ver ali uma segunda decisão por tipo.
- Na coluna do banco, a nota `ruim` vem da regra de reserva dupla sem nenhuma proteção, confirmada em 10 de 10 rodadas. A lista da skill põe "protegida só dentro do processo" em `com problemas`, e aqui não há proteção nem dentro do processo. Se o critério for ler essa falta como a mesma categoria, a nota seria `com problemas`.
