# Revisão: construção, cenário 05

## Visão geral

O projeto (Java 21, Javalin 6.7.0, PostgreSQL via HikariCP e JDBC puro) tem seis pacotes: `web` (rotas, leitura do pedido, montagem do JSON), `service` (reservas, relatórios, e-mail de confirmação), `domain` (reserva com as transições, preço, plano, dinheiro), `identity` (os jeitos de identificar o hóspede e o documento de cada um), `persistence` (o pool, o esquema e os repositórios) e a raiz (`Main` e `App`, que monta tudo). Uma requisição entra em `Routes` (`web/Routes.java:39-55`), o corpo da reserva é validado em `ReservationRequestParser`, o `ReservationService` abre uma transação (`persistence/Database.java:81-95`), trava a linha do quarto (`persistence/RoomRepository.java:49-51`), calcula o preço (`domain/Quote.java:11-20`) e grava por um repositório com SQL parametrizado; o JSON sai de `web/Views.java`. Os dados ficam em três tabelas (`rooms`, `reservations`, `messages`) criadas no código. A variação pedida (CPF, CNPJ, passaporte, e as agências no futuro) virou uma interface `IdentityKind` com uma implementação por tipo, escolhida num lugar só e usada por um renderizador comum. O roteiro passou inteiro (83 de 83) e os 10 testes do agente passam. Não há nada grave. O que há são defeitos menores: um ciclo pequeno entre `domain` e `identity`, um esquema sem migrações e sem `CHECK`, e a identificação gravada como JSON num `TEXT`.

## Arquitetura

**Números:** 6 pacotes, 14 dependências entre eles, 1 ciclo (`domain` ↔ `identity`) (de `measures/dependencies.md`). Javalin só em `web` e na raiz; JDBC em `persistence` e, só como tipo (`Connection`/`SQLException`), em `service`. A maior classe tem 159 linhas (`ReservationRepository`).
**Nota:** com problemas

- A borda HTTP é fina: cada rota lê a requisição, chama o serviço e escreve a resposta (`web/Routes.java:39-55`). A validação do corpo fica separada em `web/ReservationRequestParser.java:23-36`, e a forma do JSON em `web/Views.java`.
- As regras ficam fora do Javalin e do SQL: as transições e a devolução estão em `domain/Reservation.java:42-79`, o preço em `domain/Quote.java:11-31` e o desconto em `domain/RatePlan.java:17-19`. O serviço só orquestra (`service/ReservationService.java:56-125`). Ele recebe a `Connection` do `Database.inTransaction` e a repassa aos repositórios (`service/ReservationService.java:17-18,127-134`), mas não monta SQL. É uma fronteira de transação aceitável.
- O acesso ao banco está isolado em `persistence/*Repository.java`, e a montagem acontece num lugar só (`App.java:16-26`, `Main.java:9-12`), sem `static` global.
- O ciclo `domain` ↔ `identity` nasce de `domain/Reservation.java:3` (a reserva guarda um `GuestIdentity`) contra `identity/DocumentData.java:3`, `identity/DocumentRenderer.java:3` e `identity/CnpjIdentity.java:3` (que usam `Quote` e `Money`). É um ciclo pequeno entre vizinhos, mas sinaliza que `identity` mistura duas coisas: o tipo de hóspede (que é do domínio) e a renderização do documento (que depende do domínio).
- Dois vazamentos menores. `web` depende de `persistence` porque o serviço devolve o `MessageRepository.Message` do repositório (`service/ReservationService.java:52`, `web/Views.java:8`). E a exceção do domínio carrega o código HTTP (`domain/AppException.java:7-23`, `service/ReservationService.java:59,61,129`).

## Design de baixo nível

**Números:** 3 tipos de identificação, os 3 na estrutura (`CpfIdentity`, `CnpjIdentity`, `PassportIdentity`) e 0 fora dela. Nenhum `if`/`switch` sobre o tipo de hóspede no código. Os 3 lugares do documento (e-mail de confirmação, check-out e segunda via) passam pelo mesmo `DocumentRenderer.render`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A criação está concentrada. Escolher o tipo de hóspede acontece num ponto só:
- na entrada, pelos campos informados (`identity/IdentityKinds.java:26-28`, chamado em `web/ReservationRequestParser.java:74-84`);
- na leitura do banco, pelo código gravado (`identity/IdentityKinds.java:30-33`, chamado em `persistence/ReservationRepository.java:133`).

Daí em diante, o objeto `IdentityKind` carrega o comportamento de cada tipo: o título, o idioma, as linhas de identificação e as linhas de fechamento (`identity/IdentityKind.java:11-29`). A fatura acrescenta o ISS sobrescrevendo `closingLines` (`identity/CnpjIdentity.java:26-29`), e o *booking receipt* usa `Language.EN` (`identity/PassportIdentity.java:13`, `identity/Language.java:18-19`). Os três lugares pedem o documento ao mesmo ponto:
- o e-mail chama `DocumentRenderer.render` (`service/ConfirmationEmail.java:10`);
- o check-out e a segunda via passam por `Views.document`, que chama o mesmo render (`web/Routes.java:46-49`, `web/Views.java:61`).

Ninguém pergunta qual é o tipo. Uma agência entraria com uma classe nova e uma linha no catálogo (`identity/IdentityKinds.java:11-14`), e os campos do JSON com `null` saem do catálogo (`identity/IdentityKinds.java:36-40`, `web/Views.java:32,52`).

O que afasta da forma clássica: não existe um objeto "documento" por tipo. `DocumentRenderer.render` (`identity/DocumentRenderer.java:13-31`) é um esqueleto fixo de linhas preenchido pelos ganchos do tipo, o que se parece com um método polimórfico no tipo de hóspede, que a referência aceita. Um documento de agência com estrutura muito diferente pediria ganchos novos.

**O exagero.** A estrutura tem o tamanho certo:
- a interface tem três implementações em uso;
- `Language` tem duas instâncias e serve aos dois idiomas pedidos;
- não há fábrica de fábrica, builder nem camada que só repassa.

O `Stay` (`service/ReservationService.java:27-33`), que junta reserva e quarto para montar o `DocumentData`, é um intermediário leve, mas tem uso.

**O resto do código.** A qualidade é boa:
- dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:11-13`, `domain/Quote.java:22-25`, `domain/RatePlan.java:17-19`);
- `LocalDate` nas datas e `enum` no plano e na situação;
- transições com guarda num método só (`domain/Reservation.java:75-79`) e estado mutável só pelos métodos da reserva;
- erros inesperados registrados no log, e não engolidos (`web/Routes.java:34-37`).

Pontos pequenos:
- os valores da identificação são um `Map<String, String>` com as chaves repetidas como texto solto (`identity/CpfIdentity.java:9,16`, `identity/CnpjIdentity.java:14,22`);
- o tipo do quarto é `String` (`domain/Room.java:5`);
- há nomes totalmente qualificados no meio do código (`service/ReservationService.java:118`, `web/Routes.java:58`).

Nada disso pede mudança antes de aceitar.

## Testes

**Números:** 10 testes rodaram, 0 falhas, 0 erros, 0 pulados. São 10 métodos escritos em 2 classes (7 de unidade e 3 pela API contra o PostgreSQL real). Cobertura: 95,2% das linhas (497 de 522) e 78,7% dos ramos (74 de 94); `domain` tem 86 de 86 linhas e `identity` 59 de 59 (de `measures/tests.md`).
**Nota:** bom

Os testes conferem valores, e não só status.

O preço:
- alta temporada, R$ 599,76 (`QuoteAndDocumentTest.java:20-24`);
- a fronteira de 14 e 15 de dezembro (`QuoteAndDocumentTest.java:27-31`);
- o desconto do não reembolsável com o arredondamento (`QuoteAndDocumentTest.java:34-39`).

A parte nova do cenário, com o texto inteiro de cada documento comparado:
- o comprovante (`QuoteAndDocumentTest.java:49-58`);
- a fatura, com o ISS de R$ 38,99 (`QuoteAndDocumentTest.java:61-75`);
- o *booking receipt* de uma noite, com desconto e em BRL (`QuoteAndDocumentTest.java:78-92`);
- os separadores de milhar (`QuoteAndDocumentTest.java:95-98`);
- pela API, o ISS no e-mail (`ApiTest.java:236`).

O fluxo pela API:
- o fluxo completo com CPF, com o conflito de quarto e os 409 de transição (`ApiTest.java:179-222`);
- o CSV exato da ocupação e a linha do financeiro (`ApiTest.java:210-212,221`);
- o cancelamento do não reembolsável, com a devolução 0.00 e a receita mantida (`ApiTest.java:225-237`);
- as recusas, incluindo identificação dupla e incompleta (`ApiTest.java:240-258`).

Pela pergunta da mutação: trocar 0.05 por 0.10 no ISS, `Language.EN` por `PT_BR` ou o singular "night" faz algum teste falhar.

Ficam sem rede:
- a devolução do STANDARD confirmado: se `domain/Reservation.java:65` devolvesse zero, nenhum teste do agente falharia;
- o cancelamento de reserva pendente;
- o check-in de reserva não confirmada;
- a exclusão da reserva com devolução total no financeiro (`service/ReportService.java:55`);
- os documentos de CNPJ e passaporte no check-out e na segunda via pela API (só o renderizador é testado);
- o reinício e a concorrência.

Um detalhe: sem `DB_URL`, os testes da API são pulados em silêncio (`ApiTest.java:134`).

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(12,2)` nas 6 colunas, as datas em `date`, e a chave estrangeira `reservations.room_number` tem índice. As conferências acusaram 0 restrições que protegem regra (nenhum `CHECK`, `UNIQUE` ou `EXCLUDE`), 5 colunas de situação ou tipo sem `CHECK`, e o esquema criado por `CREATE TABLE` no código, sem migrações (de `measures/database.md`).
**Nota:** com problemas

O que está certo:
- os tipos estão certos e quase tudo é `NOT NULL` (`persistence/Database.java:17-52`);
- o SQL usa sempre parâmetros: os trechos montados com `+` só concatenam constantes (`persistence/ReservationRepository.java:26-28,73,77,97,104`);
- as conexões vêm do pool e são devolvidas por try-with-resources (`persistence/Database.java:57-65,82`);
- cada operação roda numa transação, inclusive a confirmação com o e-mail (`service/ReservationService.java:81-90`);
- a regra "um quarto, uma reserva por noite" é protegida por `SELECT ... FOR UPDATE` na linha do quarto antes da busca por sobreposição (`service/ReservationService.java:58,64`, `persistence/RoomRepository.java:49-51`, `persistence/ReservationRepository.java:81-93`). Isso vale entre processos, e o roteiro confirmou 10 de 10 rodadas simultâneas.
- a inicialização usa uma trava consultiva para dois serviços subindo juntos (`persistence/Database.java:71`).

Os defeitos:
- **O esquema não evolui.** É um `CREATE TABLE IF NOT EXISTS` em código (`persistence/Database.java:16-53`), que não sabe acrescentar uma coluna numa tabela já existente. O próximo pedido que mudar o banco (a agência, por exemplo) não tem caminho.
- **O banco não protege nenhuma regra por conta própria.** Faltam `CHECK` em `status`, `rate_plan`, `identity_kind`, `guests >= 1` e `check_out > check_in`, e não há restrição de exclusão contra a sobreposição (`persistence/Database.java:30-42`).
- **A identificação vira um JSON num `TEXT`** (`persistence/Database.java:31`, `persistence/ReservationRepository.java:42,134,144-158`). O banco não sabe se a reserva de empresa tem CNPJ, e não dá para consultar por CPF ou CNPJ sem ler o texto.

Nada disso perde dado hoje, mas são defeitos que alguém vai precisar corrigir.

## Comportamento

**Números:** 83 de 83 conferências passaram, nenhuma falhou (de `measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 05: 32 de 32;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que o texto descreve funciona pela API. Os quartos, o preço com a alta temporada e o "meio para o par" (R$ 599,76; desconto de R$ 37,48) estão certos. Também estão certos os códigos de erro, a confirmação com o e-mail, o check-in e o check-out, as devoluções (devolve o pago no STANDARD, nada no NON_REFUNDABLE nem na pendente) e os dois relatórios em CSV. As reservas e as mensagens continuam lá depois de reiniciar, e duas reservas simultâneas do mesmo quarto nunca passam juntas.

Na parte nova, os três documentos saem certos nos três lugares:
- a fatura tem a empresa no lugar do CPF e o ISS de R$ 38,99;
- o *booking receipt* sai em inglês, com BRL 2,783.52, datas 2027-03-10 e "1 night";
- o e-mail continua em português;
- a reserva e o documento trazem os cinco campos, com `null` nos que não se aplicam;
- nenhuma identificação, duas juntas, CNPJ sem razão social e passaporte sem país são recusados com 400.

## Dúvidas

- A forma do pattern é o método polimórfico no tipo de hóspede (`IdentityKind`, escolhido em `IdentityKinds.resolve`/`byCode`), e não um criador clássico com um objeto documento por tipo. O documento é um `DocumentData` único, renderizado por um esqueleto fixo com ganchos de cada tipo (`identity/DocumentRenderer.java:13-31`). Classifiquei como acerto porque a escolha acontece num lugar só, nenhum dos três lugares pergunta o tipo, e a referência aceita o método polimórfico no tipo. Alguém poderia ler o esqueleto fixo como "o mesmo documento com a variação depois" e puxar para malfeito. A evidência (nenhum `if` sobre o tipo, os três tipos na estrutura) sustenta melhor o acerto.
- A nota dos testes ficou no limite entre `bom` e `com problemas`: a devolução do plano STANDARD e algumas transições recusadas não têm teste do agente, embora o roteiro confirme que funcionam.
