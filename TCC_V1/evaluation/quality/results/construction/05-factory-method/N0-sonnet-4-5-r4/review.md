# Revisão: construção, cenário 05

## Visão geral

O projeto (`projeto/`) separa o código em seis pacotes: `handler` (um `ApiHandler` com todas as rotas do Javalin),
`service` (preço, reservas, documentos e relatórios), `repository` (JDBC puro com HikariCP), `model`, `dto` e o `App`,
que monta tudo. Uma requisição entra pelo `ApiHandler`, que converte o corpo num DTO e chama o `ReservationService`.
O serviço usa o `PricingService` para o preço e os repositórios para gravar no PostgreSQL. As tabelas são criadas com
`CREATE TABLE IF NOT EXISTS` em `repository/Database.java`. O documento por tipo de hóspede é decidido num lugar só
(`DocumentService.generateDocument`), mas o título do documento é escolhido de novo em `DocumentResponse`. O mais
sério é a reserva duplicada: nada no banco nem no código impede duas reservas do mesmo quarto nas mesmas noites quando
os pedidos chegam juntos, e em 9 de 10 rodadas as duas foram criadas. Há também três defeitos de comportamento:
- todo valor em reais nos textos sai com um espaço não separável depois de "R$";
- a segunda via sai para reserva cancelada;
- o `amountPaid` da reserva recém-criada sai como `0`.
Os testes são poucos e só de unidade (18,8% das linhas).

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só
em `handler` e no `App`, e o JDBC só em `repository`. A maior classe é `ReservationRepository` (254 linhas), seguida de
`ApiHandler` (216).
**Nota:** com problemas

As camadas existem e apontam na direção certa: borda HTTP → serviço → repositório → modelo. O `service` não conhece
Javalin nem SQL, e a montagem é feita no `App.java:42-64`. Os defeitos são localizados:

- **Regra de negócio dentro do handler.** A regra "a segunda via só existe para reserva paga" está no handler
  (`handler/ApiHandler.java:155`), e de forma incompleta (veja Comportamento). O check-out e a segunda via também
  montam o documento no handler: buscam o quarto, chamam a fábrica e montam a resposta, com o mesmo trecho copiado em
  `handler/ApiHandler.java:136-138` e `:161-163`. Isso devia estar no serviço.
- **Handler chamando repositórios direto.** O handler fala direto com `RoomRepository` e `MessageRepository`
  (`ApiHandler.java:22-23`, `:42`, `:174`), sem passar por um serviço.
- **DTO dependendo do serviço.** O pacote `dto` depende de `service`: o `CreateReservationRequest` lança as exceções
  de dentro do `ReservationService` (`dto/CreateReservationRequest.java:54`, `:61`, `:65`, `:74`). Assim, a validação
  do pedido fica entre duas camadas.
- **Conexão como singleton estático.** O acesso ao banco é um singleton estático (`repository/Database.java:12`,
  `:30`), e os repositórios pegam a conexão por `Database.getConnection()` (11 chamadas). Os repositórios não recebem
  o `DataSource`, e isso amarra o código a um estado global.

## Design de baixo nível

**Números:** a escolha do documento pelo tipo de hóspede está concentrada num método
(`service/DocumentService.java:29-41`), e os três lugares que emitem o documento passam por ele:
- o e-mail, em `DocumentService.java:118`, chamado por `ReservationService.java:150`;
- o check-out, em `ApiHandler.java:137`;
- a segunda via, em `ApiHandler.java:162`.

Fora dela há uma segunda decisão por tipo de hóspede que faz parte do documento: o título, em
`dto/DocumentResponse.java:39-52`.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que aproxima do acerto é uma fábrica concentrada: o `generateDocument` escolhe entre comprovante,
fatura e *booking receipt* (`DocumentService.java:32-37`) a partir de um tipo selado de identificação
(`model/GuestIdentification.java:3-13`). Nenhum dos três lugares monta o texto à mão.

O que afasta: a fábrica devolve só uma `String`. O título do documento (`"Comprovante de reserva"`,
`"Fatura de hospedagem"`, `"Booking receipt"`) é escolhido depois e fora dela, numa nova cadeia de `instanceof`
(`DocumentResponse.java:39-52`), usada pelo check-out e pela segunda via. Para o documento das agências, alguém terá
de mexer em dois lugares que precisam andar juntos (`DocumentService` e `DocumentResponse`). Se esquecer o segundo, o
erro só aparece em tempo de execução: cai no `IllegalStateException` (`DocumentResponse.java:51`). É o caso "quem usa
a estrutura ainda pergunta qual é o caso".

**O exagero:** não há. Não há interfaces com uma implementação só, nem builders ou camadas que só repassam.

**O resto do código:**

- **Duplicação nos geradores de documento.** As linhas de quarto, datas, diárias, desconto e total são copiadas
  quase iguais nos três geradores (`DocumentService.java:50-61`, `:74-85`, `:101-112`).
- **Duplicação no handler.** O mesmo bloco de `try/catch` com os mapeamentos de exceção para código HTTP se repete
  em seis handlers (`ApiHandler.java:73-84`, `:86-99`, `:101-114`, `:116-129`, `:131-148`, `:150-171`). Não há um `app.exception(...)` central.
- **Erro escondido na criação.** `createReservation` termina num `catch (Exception e)` que transforma qualquer falha
  em 400 e descarta a causa (`ApiHandler.java:68-70`). Uma falha de banco vira "Invalid request".
- **O dinheiro.** O tipo está certo: `BigDecimal` com `HALF_EVEN` em cada passo (`PricingService.java:40`, `:53`,
  `:58`, `:64`). Mas:
  - o formatador de reais é o `NumberFormat.getCurrencyInstance(new Locale("pt","BR"))` (`DocumentService.java:21`),
    que coloca um espaço não separável (U+00A0) depois de "R$". O texto pedido usa um espaço comum;
  - o `BigDecimal.ZERO` sem escala em `ReservationService.java:69` faz o JSON sair como `0` em vez de `0.00`.
- **O estado.** `Reservation` é um saco de *setters* (`model/Reservation.java`), e qualquer camada muda a situação
  livremente (`ReservationService.java:82`, `:101`, `:123`, `:137`). A máquina de estados fica espalhada em `if`s no
  serviço, e o `cancelReservation` lista os estados proibidos em vez dos permitidos (`ReservationService.java:95-97`).
  `Database.dataSource` é `static` mutável (`Database.java:12`).
- **A ordem das validações.** A capacidade é conferida antes da data e da quantidade de hóspedes
  (`ReservationService.java:37-47`). Um pedido com saída antes da entrada e hóspedes demais recebe 422 em vez de 400.

## Testes

**Números:** 12 testes rodaram, 0 falhas, 0 erros, 0 pulados; 12 métodos escritos em 2 classes. Cobertura de linhas
18,8% (166 de 881) e de ramos 15,2% (28 de 184). Os pacotes `handler`, `repository`, `dto` e o `App` estão com 0%, e
o `service` com 107 de 240 linhas (`measures/tests.md`). Nenhum teste passa pela API ou pelo banco.
**Nota:** com problemas

**O que está bem testado.** O preço tem testes com valores:
- os limites da alta temporada (`PricingServiceTest.java:25-36`);
- 299,88 na alta temporada e 599,76 de subtotal (`:46-58`);
- o desconto de 89,96 (`:61-65`);
- o total de 509,80 (`:76-78`);
- o ISS de 38,99 (`:82-84`).

Trocar 0.05 por 0.06 ou 1.20 por 1.25 derruba um teste.

**O que ficou sem teste.**
- **O ciclo da reserva.** Nada testa confirmar, check-in, check-out, cancelar, a devolução por plano, os conflitos de
  noite, a capacidade, os códigos de erro e os dois relatórios (`ReservationService` e `ReportService` sem nenhum
  teste).
- **A parte nova, só pela metade.** A recusa de "exatamente um jeito de identificar" (`CreateReservationRequest.java:41-69`)
  não tem teste: apagar o `count != 1` passa despercebido.
- **Asserções fracas nos documentos.** Os testes conferem pedaços soltos com `contains`
  (`DocumentServiceTest.java:39-47`, `:67-72`, `:92-99`). Por isso não pegaram o espaço não separável depois de "R$":
  conferem `"R$"` e `"779,80"` separadamente (`:46-47`). Também não testam a linha de desconto nem "1 diária"/"1 night".
- **O e-mail.** O teste do e-mail confere só trechos e não o texto inteiro (`DocumentServiceTest.java:119-123`).

O texto não pedia testes. Os que existem passam e conferem valores, mas deixam a maior parte das regras sem rede.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. As conferências acusaram:
- uma chave estrangeira sem índice (`reservations.room_number`);
- as colunas de situação e de tipo sem `CHECK` (`status`, `rate_plan`, `type`, `channel`);
- nenhuma restrição que proteja uma regra (nem `UNIQUE`, nem `EXCLUDE`, nem `CHECK`);
- 0 transações explícitas e 0 `FOR UPDATE` (`measures/database.md`).

No comportamento, 9 de 10 rodadas de dois pedidos simultâneos criaram as duas reservas.
**Nota:** ruim

- **Reserva duplicada (o defeito sério).** A regra "um quarto não pode ter duas reservas na mesma noite" é conferida
  com um `SELECT` (`ReservationRepository.java:129-157`, chamado em `ReservationService.java:49`) e só depois gravada
  com um `INSERT` em outra conexão (`ReservationService.java:71`). Não há transação, trava nem restrição de exclusão.
  Com dois pedidos ao mesmo tempo, os dois passam pela conferência, e o mesmo quarto fica vendido duas vezes na mesma
  noite: dado errado, gravado no banco.
- **Esquema sem migrações.** O esquema é criado no código com `CREATE TABLE IF NOT EXISTS` (`Database.java:38-81`),
  sem migrações. A primeira coluna nova que o próximo pedido precisar não chega a um banco que já existe.
- **Regras sem proteção no banco.** Nada no banco garante que só um dos três jeitos de identificação esteja preenchido
  (`Database.java:53-57`), nem os valores válidos de situação e plano.
- **Escritas que deviam andar juntas.** A confirmação grava a reserva (`ReservationService.java:84`) e depois a
  mensagem (`:159`) em conexões separadas, sem transação: se a segunda escrita falhar, fica uma reserva confirmada sem
  e-mail. A carga inicial dos quartos confere e insere sem transação (`RoomRepository.java:17-38`).
- **O que está certo.** O SQL é todo com parâmetros (o script não achou SQL montado com `+`), as conexões vêm de um
  pool e voltam com *try-with-resources*, e os dados continuam lá depois de reiniciar.
- **Consultas.** O relatório financeiro filtra o mês com `EXTRACT` sobre `check_in` (`ReservationRepository.java:162-163`),
  o que impede o uso de índice. O de ocupação busca o quarto uma vez por linha (`ReportService.java:33`). Pesam pouco
  neste tamanho.

## Comportamento

**Números:** 76 de 83 conferências passaram (`measures/behavior.md`):
- parte comum: 42 de 46;
- depois de reiniciar: 3 de 3;
- cenário 05: 30 de 32;
- reservas simultâneas: 1 de 2.

**Nota:** com problemas

**As falhas, uma a uma.**

- **"o 201 comporta 3" e "a diária do 201 é 389.90".** Não são erro do agente. O sistema devolve `"number":"201"`
  como texto, e o roteiro procura o número 201. Os valores estão certos (`capacity: 3`, `nightlyRate: 389.90`,
  conferido com `curl`). O texto não diz se o número do quarto é texto ou número, e texto é uma escolha razoável.
- **"o e-mail traz o comprovante", "fatura: o ISS retido" e "fatura: o total".** Erro do agente, e pega todos os
  documentos em português e o e-mail. Conferi os bytes com `curl` e `od`: depois de "R$" vem `302 240` (U+00A0, espaço
  não separável), por causa do formatador de moeda do Java (`DocumentService.java:21`). Na tela parece igual. Mas o
  texto que o dono pediu ("R$ 779,80") não sai igual, e qualquer busca ou comparação feita pelo site ou pela
  contabilidade com o texto falha.
- **"dinheiro no JSON com 2 casas (amountPaid 0.00)".** Erro do agente, pequeno: a resposta da criação traz
  `"amountPaid":0` (`ReservationService.java:69`). Depois de lida do banco, a reserva vem `0.00`.
- **Duas reservas ao mesmo tempo.** 9 de 10 rodadas criaram as duas. Para o dono, o site pode vender o mesmo quarto
  duas vezes na mesma noite. O defeito pesa na coluna Banco.
- **Fora do roteiro.** A segunda via de uma reserva cancelada responde 200 com o comprovante; conferi com `curl`. O
  texto diz que ela só existe para reserva paga (confirmada, com check-in ou com check-out), e o handler só recusa
  `PENDING` (`ApiHandler.java:155`).

**O que funciona.** O preço com temporada e arredondamento, as recusas e os códigos de erro, o ciclo da reserva, as
devoluções e os dois relatórios. Na parte nova:
- os três documentos e os cinco campos com `null`;
- o *booking receipt* em inglês com `BRL 2,783.52`, "1 night" e a linha de desconto;
- as quatro recusas de identificação.

## Dúvidas

- **Classe do pattern: malfeito ou acerto.** Ficou entre `malfeito` e `acerto`. A escolha do texto do documento está
  concentrada numa fábrica por método (`DocumentService.generateDocument`), usada nos três lugares, e isso é a forma
  "fábrica concentrada" que a referência aceita. Dei `malfeito` porque o título do documento é decidido de novo, por
  `instanceof`, em `DocumentResponse.java:39-52`, fora da fábrica. Quem lê esse título como detalhe de apresentação
  daria `acerto`.
- **Nota do Banco.** A nota `ruim` se apoia no resultado medido das reservas simultâneas (9 de 10 duplicadas). Sem
  esse resultado, os outros defeitos do banco dariam `com problemas`.
