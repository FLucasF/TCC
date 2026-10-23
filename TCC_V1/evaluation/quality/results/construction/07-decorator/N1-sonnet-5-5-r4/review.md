# Revisão: construção, cenário 07

## Visão geral

O projeto tem seis pacotes em camadas: `web` (Javalin, as rotas, o parser do corpo e as views JSON), `service` (as
operações e os relatórios), `domain` (reserva, preço, temporada, planos, comprovante, sem nenhuma biblioteca),
`messaging` (as regras de envio), `persistence` (pool Hikari, transação e dois repositórios JDBC) e a raiz (`Main` e
`Config`, onde tudo é montado). Uma requisição entra em `web/App.java:42-52`, vai ao `ReservationService`, que abre uma
transação (`persistence/Database.java:27-41`), carrega a reserva com `FOR UPDATE`, aplica a mudança de situação no
domínio (`domain/Reservation.java:55-87`), grava e, na confirmação, passa o e-mail pelo `MessagePipeline` e grava as
mensagens na mesma transação (`service/ReservationService.java:44-47`). Os dados ficam em duas tabelas do PostgreSQL
(`reservations` e `messages`); os quartos ficam no código (`domain/Rooms.java:8-13`). As regras de envio são uma lista
de políticas, cada uma numa classe, filtradas por modo na montagem. O roteiro do comportamento passou inteiro
(65 de 65) e os 14 testes passam. O ponto mais fraco é o banco: o esquema é criado por `CREATE TABLE IF NOT EXISTS` no
código, sem migrações e sem nenhuma restrição (nem CHECK, nem chave estrangeira, nem EXCLUDE).

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, nenhum ciclo. Javalin só em `web` e na raiz; JDBC e pool só em
`persistence`; `domain` não depende de nenhum outro pacote, e `messaging` só de `domain`. Maior classe:
`ReservationRepository` com 155 linhas.
**Nota:** bom

- A borda HTTP é fina: cada rota só lê a requisição, chama o serviço e escreve a resposta (`web/App.java:41-52`), e o
  parse do corpo fica numa classe à parte que só converte e valida o formato (`web/ReservationRequestParser.java:21-40`).
  As exceções de domínio viram o JSON de erro num lugar só (`web/App.java:54-58`).
- As regras não conhecem Javalin nem JDBC: preço em `domain/Quote.java:8-18`, temporada em `domain/Season.java:14-20`,
  transições num único `switch` em `domain/ReservationStatus.java:14-21`, devolução em `domain/RatePlan.java:21-23`.
- O SQL fica todo em `persistence/ReservationRepository.java` e `persistence/MessageRepository.java`; os serviços só
  pedem uma transação e usam os repositórios que ela entrega (`persistence/Tx.java:10-21`).
- A montagem é num lugar só, `Main.java:21-25`, e a configuração vem do ambiente em `Config.java:8-16`.
- Detalhes pequenos, que não pedem mudança antes de aceitar: o `ErrorCode` do domínio carrega o status HTTP
  (`domain/ErrorCode.java:4-9`), e `GET /rooms` lê `Rooms.ALL` direto da rota, sem passar por um serviço
  (`web/App.java:41`). Os dois são escolhas aceitáveis para o tamanho do sistema.

## Design de baixo nível

**Números:** as 3 regras pedidas (endereço válido, desvio para testes, cópia para o arquivo) estão na estrutura, uma
classe cada (`ValidAddressPolicy`, `TestRedirectPolicy`, `ArchiveCopyPolicy`). Nenhuma delas pergunta pelo modo; a
combinação por modo fica num único lugar (`messaging/MessageRules.java:22` mais `messaging/MessagePipeline.java:9-14`).
Nenhum `if` de regra de envio fora da estrutura.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** O texto pede um Decorator em volta do envio. O agente fez uma lista de filtros aplicada por um
pipeline, e a referência aceita isso de antemão como alternativa equivalente, desde que cada regra seja uma peça
própria e a combinação por modo seja decidida na montagem:

- a interface comum é `MessagePolicy.apply(Message) -> List<Message>` (`messaging/MessagePolicy.java:9-11`); como cada
  política devolve uma lista, ela pode bloquear (`ValidAddressPolicy.java:10-13`), alterar
  (`TestRedirectPolicy.java:18-21`) ou acrescentar uma mensagem (`ArchiveCopyPolicy.java:18-19`, a cópia logo depois da
  original);
- o pipeline aplica as políticas em ordem, em cadeia (`MessagePipeline.java:16-22`);
- o escopo (sempre, só teste, só fora do teste) é um dado da regra (`messaging/Rule.java:3`, `messaging/Scope.java:9-15`)
  e o filtro por modo acontece uma vez, na construção do pipeline (`MessagePipeline.java:10-13`). Nenhuma política
  conhece o modo de teste;
- a combinação de cada instalação é uma lista lida na montagem (`Main.java:23`, `Config.java:15`,
  `MessageRules.java:28-39`): a segunda pousada sem a cópia é só tirar `archive-copy` da lista, e uma regra nova (a da
  madrugada, o limite por dia) é uma classe e uma linha em `MessageRules.java:16-19`. Um nome desconhecido falha logo
  ao subir (`MessageRules.java:33-35`).

O que afasta do Decorator clássico: não há um "envio" embrulhado. Quem envia de verdade é a gravação no banco
(`MessageRepository.insertAll`, chamada pelo serviço em `ReservationService.java:46`), e as políticas transformam a
mensagem antes dela. Isso basta para o que o texto pede. Ficam dois pontos frágeis, pequenos: a ordem das regras
importa e só é garantida pelo texto da lista padrão e por um comentário (`MessageRules.java:21-22`); se alguém puser
`archive-copy` antes de `valid-address`, uma mensagem bloqueada ganha cópia. E a guarda "só mexe no que ainda vai sair"
se repete em cada política (`TestRedirectPolicy.java:15`, `ArchiveCopyPolicy.java:15`, `ValidAddressPolicy.java:10`).

**O exagero.** Não há estrutura sem necessidade: o pequeno formato de configuração (`HOTEL_MESSAGE_RULES`) responde ao
pedido explícito de "cada instalação pode querer outra combinação"; `Tx` só agrupa os dois repositórios que dividem a
mesma conexão (`persistence/Tx.java:6-22`); `Report` só monta o CSV (`service/Report.java:6-14`). Não há interface com
uma implementação só, fábrica ou camada que só repassa.

**O resto do código.** Dinheiro em `BigDecimal` com `HALF_EVEN` num lugar só (`domain/Money.java:16-18`), arredondado
a cada passo como o texto pede (`domain/Quote.java:12-17`); datas em `LocalDate`; situações, planos, tipos e canal em
`enum`. As transições ficam todas num `switch` (`ReservationStatus.java:14-21`) e a reserva só muda de estado por
métodos que conferem a transição (`Reservation.java:81-87`). O serviço tem um único método que faz "trava, muda,
grava, efeito colateral" (`ReservationService.java:70-78`), sem duplicação entre as operações. Não há exceção engolida;
o `catch (Exception)` do Javalin registra a causa no log (`web/App.java:55-58`). Os detalhes que um revisor
apontaria são pequenos: o nome qualificado inteiro em `web/App.java:62`, e `Receipt.of` público, que deixa gerar
comprovante sem passar pela conferência de "reserva paga" de `Reservation.receipt()` (`Reservation.java:73-79`). Hoje
só é chamado em situações pagas (`ReservationService.java:58`, `ReservationEmails.java:13`).

## Testes

**Números:** 14 testes rodaram, 0 falhas, 0 erros, 0 pulados; 14 métodos escritos. Cobertura de 489 de 538 linhas
(90,9%) e de 100 de 117 ramos (85,5%); `domain` com 147 de 148 linhas, `messaging` com 73 de 76, `service` com 52 de 52.
Três classes: `PricingTest` (unidade), `MessagePipelineTest` (unidade das regras de envio) e `ApiTest` (o sistema
inteiro pela API, contra o PostgreSQL de `DB_URL`, num schema próprio por teste, `TestSystem.java:347-353`).
**Nota:** bom

- Os testes conferem as regras com valores: a fronteira da temporada (`PricingTest.java:13-21`), 599,76 do exemplo do
  texto (`PricingTest.java:24-30`), o desconto de 15% (`PricingTest.java:33-38`), uma estada que cruza a fronteira
  (`PricingTest.java:41-44`), o formato R$ 1.234,56 (`PricingTest.java:47-50`).
- Pela API: o e-mail de confirmação inteiro, texto por texto, e a cópia com "[Cópia] " (`ApiTest.java:65-86`); o ciclo
  de vida e as recusas 409 (`ApiTest.java:58-96`); todos os códigos de erro (`ApiTest.java:125-141`); as devoluções
  dos três casos de cancelamento (`ApiTest.java:156-163`) e os dois relatórios, comparados como CSV inteiro
  (`ApiTest.java:166-183`).
- A parte do cenário está bem coberta: sete endereços inválidos nos dois modos, todos BLOCKED, com o endereço original
  e sem cópia (`MessagePipelineTest.java:33-42`); o desvio no modo de teste com o assunto e a primeira linha exatos e
  sem cópia (`MessagePipelineTest.java:45-52`); uma instalação sem a cópia (`MessagePipelineTest.java:55-57`).
  Mutações como trocar "[Cópia] ", apagar a cópia, trocar o escopo `test`/`live` ou aceitar `ana@.com` fariam algum
  teste falhar.
- Lacunas, nenhuma grave: nenhum teste do agente tem um caso de empate no arredondamento. Os valores testados (116,97,
  89,96, 104,38) não caem no meio, e trocar `HALF_EVEN` por `HALF_UP` em `Money.java:17` passaria nos testes (o
  roteiro do comportamento confere esse caso, 37,48). As duas reservas simultâneas não estão testadas. E os testes pela
  API se desligam sozinhos quando não há banco (`ApiTest.java:20-23`), o que deixaria o `mvn verify` passar sem eles.
  Aqui o banco existia e eles rodaram.

## Banco

**Números:** 2 tabelas (`reservations`, 16 colunas; `messages`, 6 colunas), as duas com chave primária. Dinheiro
todo em `numeric(12,2)`, datas em `date`. 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE/EXCLUDE. O esquema é criado por
`CREATE TABLE` no código, sem migrações. SQL montado com `+`: 8 candidatos, todos com constantes (nenhum dado de fora).
**Nota:** com problemas

- O esquema nasce de `CREATE TABLE IF NOT EXISTS` em `persistence/Database.java:46-73`. Isso funciona no banco vazio,
  mas não evolui: a próxima coluna (para o limite de mensagens por dia que o texto anuncia, por exemplo) não entra numa
  tabela que já existe sem uma migração escrita à mão.
- Não há nenhuma restrição no banco: `status`, `rate_plan`, `channel` são `text` sem CHECK
  (`Database.java:56`, `:61`, `:68`, `:72`), `guests` não tem `CHECK (guests > 0)` e `check_out > check_in` não é
  conferido no banco. Como os quartos ficam no código (`domain/Rooms.java:8-13`), `room_number` não tem chave
  estrangeira nem uma tabela de quartos (`Database.java:49`). Também não há índice para `room_number`/`check_in`, as
  colunas das consultas de disponibilidade e dos relatórios (`ReservationRepository.java:41-42`, `:110-117`). Pesa pouco
  com cinco quartos.
- A regra do "um quarto, uma reserva por noite" está protegida de verdade, entre processos: uma trava consultiva por
  quarto dentro da transação (`ReservationRepository.java:30-37`, chamada em `ReservationService.java:32`) antes da
  consulta de conflito (`ReservationRepository.java:40-53`). O roteiro confirmou nas 10 rodadas simultâneas. Uma
  restrição `EXCLUDE` seria a proteção no próprio banco. As mudanças de situação usam `SELECT ... FOR UPDATE`
  (`ReservationRepository.java:103-105`), e as mensagens de um mesmo envio são gravadas em sequência sob outra trava
  (`MessageRepository.java:22-29`), na mesma transação da confirmação.
- O código está correto: todo valor vindo de fora entra por `?`. O único trecho montado com dado é a lista de
  situações, e ela vem do `enum` (`ReservationRepository.java:109`). As conexões ficam em try-with-resources, com pool
  Hikari (`Database.java:15-24`) e rollback em erro (`Database.java:34-36`).

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam: a parte comum 46 de 46, os dados depois de reiniciar 3 de 3,
o cenário no modo normal 9 de 9, no modo de teste 5 de 5, e as duas reservas ao mesmo tempo 2 de 2.
**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede. Os preços batem com a temporada e com o arredondamento meio
para o par (37,48 no desconto de uma noite), o comprovante e o e-mail saem no formato combinado, os cancelamentos
devolvem o certo e os relatórios contam como o texto diz. Os dados sobrevivem a um reinício, e duas reservas
simultâneas do mesmo quarto nunca passam juntas. Nas regras de envio: o endereço inválido fica BLOCKED com o endereço
original e sem cópia, nos dois modos; a cópia para o arquivo vem logo depois da original; com `HOTEL_TEST_MODE=true`
tudo vai para a caixa de testes, com "[TESTE] " e a linha do destinatário original, sem cópia. O log do serviço
(`measures/behavior-service.log`) não mostra erro.

## Dúvidas

A classe do pattern ficou entre `acerto` e `alternativa equivalente`. As regras têm a mesma interface e se encadeiam,
o que lembra os embrulhos, mas não há uma interface de envio com um envio real embrulhado: o pipeline transforma a
mensagem antes da gravação. Por isso dei a alternativa equivalente (a "lista de filtros aplicada pelo envio" da
referência).
