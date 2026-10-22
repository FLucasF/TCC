# Revisão: construção, cenário 05

## Visão geral

O projeto tem seis pacotes: `handlers` (as rotas do Javalin), `dao` (o JDBC), `model`, `util` (o preço e o texto dos documentos), `config` (a conexão e o esquema) e a raiz (`Main`). Tudo é `static`. Uma requisição entra por um método estático do handler, por exemplo `ReservationHandler.createReservation`. Esse método lê o corpo com Gson, faz as validações, calcula o preço com `PriceCalculator`, grava pelo `ReservationDAO` e responde com `ctx.json`. Os dados ficam em três tabelas, criadas com `CREATE TABLE IF NOT EXISTS` dentro de `DatabaseConfig`.

**O sistema não funciona.** São dois defeitos, e cada um sozinho já basta para derrubá-lo:

- O `pom.xml` não traz o Jackson, e o projeto não configura um mapeador para o Gson. Por isso toda chamada a `ctx.json` falha, inclusive a resposta de erro, e toda rota JSON devolve 500 com o aviso "you don't have an object mapper configured" (`measures/behavior-service.log`).
- As datas vão ao banco com `setString` em colunas `date` (`dao/ReservationDAO.java:38`, `:107`, `:132`). Chamei o DAO direto e o PostgreSQL recusou: "column check_in is of type date but expression is of type character varying" e "operator does not exist: date < character varying". Portanto nenhuma reserva pode ser gravada, mesmo que o JSON funcionasse.

O roteiro passou em 4 de 83 conferências. O único teste é `assert true`.

## Arquitetura

**Números:** 6 pacotes, 8 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só na raiz e em `handlers`, e o JDBC só em `config` e `dao`. A maior classe é `ReservationHandler`, com 308 linhas.
**Nota:** com problemas

O acesso ao banco está isolado em `dao/`, e as dependências apontam para o lado certo (handlers → dao/util → model). Faltou, porém, a camada de regras: as regras de negócio estão nos handlers, junto com o HTTP.

- **A situação da reserva:** `handlers/ReservationHandler.java:147`, `:180`, `:212`, `:239`, `:269`.
- **A devolução no cancelamento:** `handlers/ReservationHandler.java:186-191`.
- **A disponibilidade e a capacidade:** `handlers/ReservationHandler.java:82-92`.
- **A escolha do documento e o envio do e-mail:** `handlers/ReservationHandler.java:155-160` e `:285`.
- **O "dinheiro que fica com a pousada":** `handlers/ReportHandler.java:98-101`.

O `ReservationHandler` lê a requisição, valida, aplica as regras, chama três DAOs e monta a resposta, e é o lugar onde o próximo pedido vai doer.

Não há montagem: tudo é chamado por métodos estáticos. A conexão é um `DriverManager.getConnection` estático, sem pool (`config/DatabaseConfig.java:11`), e as rotas apontam para referências estáticas (`Main.java:14-27`). Nada pode ser trocado nem testado em separado.

## Design de baixo nível

**Números:** 3 tipos de documento; a escolha entre eles fica num único método (`handlers/ReservationHandler.java:285-305`), chamado pelos 3 lugares (confirmação `:157`, check-out `:249`, segunda via `:276`). Nenhum lugar decide fora dele.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** Os três lugares pedem o documento a `generateDocument`. Esse método escolhe pelo tipo de hóspede, com um `if/else` sobre `guestCpf`/`companyCnpj` (`:289-298`), e chama a função de texto de cada tipo em `util/DocumentFormatter.java:28`, `:50` e `:75`. A referência aceita a fábrica concentrada (um método que escolhe o documento pelo tipo de hóspede) como acerto.

O que afasta:

- A fábrica é um método privado do handler, e o tipo de hóspede não existe no domínio: é deduzido de qual campo não é nulo. O `else` final trata qualquer outro caso como estrangeiro (`:296`).
- Os três documentos são um único `Document` genérico, com `title` e `text` em texto. A agência de turismo pediria mais um ramo no `if` e mais uma função estática.
- A validação da identificação é outro `if` sobre os mesmos campos, em outro lugar (`:33-50`).

**O exagero.** Não há estrutura sem necessidade.

**O resto do código** tem defeitos espalhados, e por isso a nota é `ruim`:

- **Dinheiro em `double`:** em `Reservation`, `Room`, `Document` e `PriceCalculator`. O arredondamento faz `new BigDecimal(double)` (`util/PriceCalculator.java:46`), então o "meio para o par" depende da representação binária: 249,90 × 0,15 vira 37,48499999…, e o 37,48 só sai certo por acaso.
- **A temporada:** a diária de todas as noites é calculada pela data de entrada (`handlers/ReservationHandler.java:98`, `util/PriceCalculator.java:11`). Uma estadia de 13 a 17/12 cobra tudo como baixa temporada.
- **Moeda e locale:** a formatação depende do locale padrão e não tem separador de milhar. `util/DocumentFormatter.java:21` dá "R$ 1234,56" em vez de "R$ 1.234,56", e `:25` dá "BRL 2783.52" em vez de "BRL 2,783.52". O relatório usa `String.format("%.2f")`, também sensível ao locale (`handlers/ReportHandler.java:108`, `:113`).
- **Tipos soltos:** datas e o plano circulam como `String` (`dao/ReservationDAO.java:13-18`, `util/PriceCalculator.java:34`). Um `ratePlan` inválido é gravado e quebra depois em `RatePlan.valueOf`.
- **Estado aberto:** os modelos têm campos públicos mutáveis (`model/Reservation.java:7-…`), e o `createReservation` recebe 16 parâmetros (`dao/ReservationDAO.java:13`).
- **Os erros:** cada handler tem um `catch (Exception)` que devolve 500 com `e.getMessage()` ao cliente (`handlers/ReservationHandler.java:113` e mais seis). Assim, um corpo que não é JSON (`:18`) vira 500, não 400.
- **Validação e cancelamento:** falta checar se a saída é depois da entrada (`:66-73`). O cancelamento zera `amountPaid` (`:191`).
- **Duplicação:** o `record ErrorResponse` está copiado em cinco classes (`Main.java:36`, `handlers/RoomHandler.java:20`, `MessageHandler.java:20`, `ReportHandler.java:123`, `ReservationHandler.java:307`). A busca com resposta 404 se repete em seis métodos, e o cabeçalho do comprovante e o da fatura são cópias (`util/DocumentFormatter.java:28-73`).

## Testes

**Números:** 1 teste rodou, 0 falhas; 1 método de teste escrito; cobertura de 0 de 608 linhas (0,0%) e de 0 de 178 ramos (0,0%) (`measures/tests.md`).
**Nota:** ruim

O único teste é `src/test/java/com/pousadamareal/BasicTest.java:9`, um `assert true` que nem roda com asserções ligadas e não executa nenhuma linha do sistema. Nenhuma regra é conferida: preço, temporada, arredondamento, planos, devolução, situações, recusas, nem nada da parte nova (fatura, ISS de 5%, booking receipt, escolha do documento).

A pergunta da mutação não tem resposta: trocar 0.05 por 0.50 em `util/DocumentFormatter.java:53`, inverter a escolha em `handlers/ReservationHandler.java:289`, ou apagar qualquer linha não derruba teste algum. O `mvn verify` passa justamente porque não há o que verificar; os dois defeitos que impedem o sistema de funcionar passaram despercebidos.

## Banco

**Números:** 3 tabelas, todas com chave primária; o dinheiro em `numeric(10,2)` e as datas em `date`. As conferências acusaram (`measures/database.md`):

- a chave estrangeira `reservations(room_number)` sem índice;
- nenhuma CHECK em `status`, `rate_plan`, `type` e `channel`;
- nenhuma restrição UNIQUE ou EXCLUDE;
- nenhum pool, nenhuma transação, nenhum `FOR UPDATE`;
- 1 SQL montado com `+`.

**Nota:** ruim

O esquema em si está razoável (tipos certos, FK para o quarto). O código que fala com o banco é que não funciona:

- **As datas como texto:** `createReservation` grava as datas com `setString` (`dao/ReservationDAO.java:38-39`), e a consulta de disponibilidade e a de ocupação as comparam com `setString` (`:107-108`, `:132-133`). Testei direto contra o PostgreSQL: o INSERT e as duas consultas falham por tipo. O sistema não consegue gravar uma única reserva; só o relatório financeiro, que faz `?::date` (`:150`), roda.
- **A dupla reserva:** a única proteção é a leitura antes da escrita (`handlers/ReservationHandler.java:88` e `:103`), sem transação, sem trava e sem EXCLUDE. Se o resto funcionasse, duas reservas simultâneas passariam.
- **O cancelamento:** grava `amount_paid = 0` (`handlers/ReservationHandler.java:191`). Com isso some o registro do que foi pago, e uma NON_REFUNDABLE cancelada sairia com 0 no financeiro.
- **O esquema:** é criado por `CREATE TABLE IF NOT EXISTS` num texto dentro do Java, quebrado por `split(";")` (`config/DatabaseConfig.java:23-82`), sem migração. Há ainda uma cópia não usada em `src/main/resources/schema.sql`.
- **O SQL montado com `+`:** `dao/RoomDAO.java:36` concatena um `int`. Não dá para injetar nada, mas destoa do resto, que usa `?`.

## Comportamento

**Números:** 4 de 83 conferências passaram (`measures/behavior.md`):

- parte comum: 2 de 46;
- depois de reiniciar: 1 de 3;
- cenário 05: 0 de 32;
- reservas simultâneas: 1 de 2.

**Nota:** ruim

Toda rota que responde JSON devolve 500, porque falta um mapeador JSON (sem Jackson no `pom.xml`, e `Main.java:12` não configura outro). Isso vale para `GET /rooms`, a criação, as recusas (400, 404, 409, 422) e a consulta. As conferências que "passaram" passaram por acaso: "refund null" e "nenhuma rodada com duas reservas" passam porque nada é criado, e o cabeçalho do financeiro sai porque essa rota escreve texto e sua consulta faz o cast da data.

Para o dono da pousada, o efeito é direto: o site não consegue listar quartos nem criar reserva, a recepção não faz check-in nem check-out, e nenhum hóspede recebe comprovante, fatura ou booking receipt.

Pela leitura, mesmo com os dois defeitos corrigidos haveria mais erros:

- a temporada calculada só pela data de entrada;
- os valores acima de mil sem separador ("R$ 1234,56", "BRL 2783.52"), o que falharia a conferência "BRL 2,783.52";
- 500 em vez de 400 para corpo que não é JSON e para mês como `2027-13`;
- nenhuma recusa para saída antes da entrada (ou igual a ela);
- a segunda via emitida para reserva cancelada (`handlers/ReservationHandler.java:269` só barra PENDING);
- o `refund` null, em vez de 0,00, numa cancelada sem devolução;
- o `amountPaid` zerado no cancelamento, que tiraria a NON_REFUNDABLE cancelada do financeiro;
- nenhuma proteção contra duas reservas simultâneas.

## Dúvidas

- **A forma do Factory Method:** foi usada a fábrica concentrada, um método privado `generateDocument` com `if/else` pelo campo de identificação preenchido. Dei `acerto` porque a referência aceita essa forma, e a escolha mora num lugar só e serve aos três pontos. Mas a fábrica fica dentro do handler, e o tipo de hóspede só é deduzido dos campos nulos; um revisor mais estrito poderia ver aí um caso de fronteira.
- **As falhas que não pude medir:** como nenhuma reserva chega a ser criada, as falhas apontadas pela leitura (temporada, milhar, segunda via de cancelada) não foram confirmadas pela API.
