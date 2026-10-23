# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em sete pacotes (`hotel`, `hotel.web`, `hotel.reservations`, `hotel.rooms`,
`hotel.messages`, `hotel.documents`, `hotel.reports`). A requisição entra em `hotel.web.HttpApi`, passa pelo
`ReservationService` e chega às classes de acesso a dados (`ReservationStore`, `Rooms`, `SentMessages`), que gravam no
PostgreSQL. As tabelas são criadas por migrações do Flyway, e a montagem fica em `hotel.Hotel`. O pedido trazia três
regras de envio (bloquear endereço inválido, mandar cópia para o arquivo, desviar para a caixa de testes no modo de
teste) e avisava que outras regras viriam, cada uma valendo sempre, só no teste ou só fora dele, em combinações
diferentes por instalação. O agente mexeu em pouca coisa: o campo `status` em `Message`, uma migração V2 nova, a
gravação do status em `SentMessages`, e as três regras escritas como `if` dentro de `EmailSender.send`
(`src/main/java/hotel/messages/EmailSender.java:20-34`). O modo de teste é lido do ambiente no construtor da própria
classe (`EmailSender.java:12`). O comportamento está todo certo: as 65 conferências passaram. O problema mais sério
está no design. O Decorator que o pedido descreve não foi feito, e a próxima regra (madrugada, limite por dia) ou a
segunda pousada sem cópia vão exigir mudar esse mesmo método.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base. Há um ciclo, `hotel.documents` ↔
`hotel.reservations`, que já existia na base. Só mudou o tamanho de `hotel.messages`, de 75 para 109 linhas.
**Nota:** com problemas

O agente não mudou a forma do sistema. A borda HTTP só expõe a lista (`HttpApi.java:81`), e o SQL continua isolado em
`SentMessages` (`SentMessages.java:20-56`). O ciclo `documents`/`reservations` vem da base. O defeito do agente está
na montagem. A configuração de ambiente era lida num lugar só (`Hotel.java:42-43`, `PORT` e `Database`), mas
`EmailSender` agora lê `HOTEL_TEST_MODE` por conta própria no construtor público (`EmailSender.java:12`). `Hotel.java:25`
continua chamando `new EmailSender(sentMessages)` sem saber que existe um modo de teste. Por isso, a pergunta "quais
regras valem nesta instalação" fica sem lugar na montagem, que é justamente onde o pedido a quer ("cada instalação do
sistema pode querer outra combinação"). A mistura é localizada, e não há ciclo novo.

## Design de baixo nível

**Números:** 3 regras pedidas, 0 delas numa estrutura própria. As 3 são `if` dentro de `EmailSender.send`: a validação
(`EmailSender.java:21`), o modo de teste (`:25`) e a cópia (`:31-32`, que fica no caminho "não é teste"). Nenhuma
interface de envio foi criada, nenhum embrulho e nenhuma combinação na montagem.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

A referência classifica como omissão exatamente este caso: "os três `if` (o endereço, o modo de teste, a cópia) ficam
dentro do envio". Em `EmailSender.java:20-34`:
- o envio real, a validação, o desvio para testes e a cópia moram no mesmo método;
- a combinação por modo está escrita na ordem dos `if`: a cópia só acontece porque o `return` do modo de teste vem
  antes (`:25-29`).

Ligar ou desligar uma regra por instalação (a segunda pousada sem cópia) ou acrescentar "não mandar de madrugada" e
"limite por dia" pede mexer nesse método e criar mais flags. Não há nada que aproxime do pattern: `EmailSender`
continua uma classe concreta `final` (`:4`), e quem chama não depende de nenhuma abstração
(`ReservationService.java:17`).

Não há exagero: nenhuma interface, fábrica ou camada sem uso foi acrescentada.

O resto do código novo tem defeitos localizados:
- o status é texto solto: `"SENT"` e `"BLOCKED"` aparecem como literais (`EmailSender.java:22,28,31,32`) e
  `Message.status` é `String` (`Message.java:3`), quando o domínio tem dois valores fixos e pedia um `enum`;
- o canal `"EMAIL"` se repete quatro vezes (`EmailSender.java:22,28,31,32`);
- a validação aceita domínios com ponto na ponta, como `ana@gmail.` ou `ana@.com`, porque só confere
  `domain.contains(".")` (`EmailSender.java:44-45`). O pedido diz "pelo menos um ponto no meio".

O resto do projeto vem da base, sem alteração.

## Testes

**Números:** com o agente, rodaram 32 testes, sem falha, erro ou teste pulado, para 32 métodos escritos (a base tinha
26). A cobertura de linhas foi de 93.8% para 93.9% (490 de 522). A de ramos foi de 87.1% para 85.9% (110 de 128). No
pacote `hotel.messages`, a cobertura é de 48 de 53 linhas e 11 de 14 ramos.
**Nota:** com problemas

O agente acrescentou `EmailSenderTest`, com 5 testes contra o banco, e um teste pela API
(`HttpApiTest.java:110-124`). Ajustou também os testes da base que contavam mensagens: de 1 para 2, por causa da cópia
(`ReservationServiceTest.java:91,109`, `HttpApiTest.java:81`). Esse ajuste segue o pedido e não enfraquece nada,
porque os ajustados ainda conferem o destinatário e o assunto da cópia (`ReservationServiceTest.java:98-100`).

As regras novas são conferidas com valores:
- o bloqueio de `ana@gmail`, `ana.gmail.com` e `@gmail.com` (`EmailSenderTest.java:29-44`);
- a cópia com o endereço do arquivo e o assunto `[Cópia] ` (`:47-60`);
- a falta de cópia para mensagem bloqueada (`:63-71`);
- o modo de teste com endereço, assunto e corpo exatos (`:74-85`);
- o bloqueio no modo de teste com o endereço original (`:88-97`).

Pela pergunta da mutação, algumas regras ficam sem rede:
- **o corpo da cópia:** nenhum teste confere que a cópia tem o mesmo texto (`EmailSenderTest.java:47-60` não olha
  `body()`), então trocar `body` por `""` em `EmailSender.java:32` não quebra nada;
- **o @ único:** apagar a condição `atIndex != email.lastIndexOf('@')` (`EmailSender.java:41`) não quebra nada, porque
  nenhum teste usa dois @;
- **o ponto "no meio":** não há caso com ponto na ponta, e por isso o defeito de `EmailSender.java:44-45` passa;
- **a leitura de `HOTEL_TEST_MODE`:** a leitura do ambiente (`EmailSender.java:12`) não é testada. Os testes usam o
  construtor de pacote com o booleano.

## Banco

**Números:** 3 tabelas. Migrações: V1 igual à da base, V2 nova (`V2__add_message_status.sql`). Nenhuma tabela sem
chave, nenhum dinheiro fora de `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma data em texto. Sobre a
coluna nova, a conferência acusou só `messages.status: text, sem CHECK`.
**Nota:** bom

A mudança no esquema veio como migração nova, sem editar a V1, e
`ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT'`
(`src/main/resources/db/migration/V2__add_message_status.sql:2`) preenche as mensagens que já existiam no banco de
quem roda o sistema. O SQL novo usa parâmetros (`SentMessages.java:23-28`) e devolve a conexão com try-with-resources
(`:21`).

Há dois detalhes pequenos:
- **sem CHECK:** `status` não tem um CHECK `IN ('SENT','BLOCKED')`, mas isso segue o padrão da base, onde nenhuma
  coluna de situação tem CHECK (`reservations.status`, `rate_plan`, `rooms.type`);
- **sem transação:** a original e a cópia são duas gravações separadas (`EmailSender.java:31-32`). Se a segunda
  falhar, a original fica sem cópia. É um risco pequeno numa lista de mensagens simulada.

## Comportamento

**Números:** 65 de 65 conferências passaram:

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| depois de reiniciar | 3 de 3 |
| cenário, modo normal | 9 de 9 |
| cenário, modo de teste | 5 de 5 |
| reservas simultâneas | 2 de 2 |

**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- endereço inválido (`ana@gmail`, `ana.gmail.com`, `ana@@gmail.com`, `@gmail.com`) fica `BLOCKED`, com o endereço
  original e sem cópia;
- a mensagem válida sai `SENT`, seguida da cópia para o arquivo, com `[Cópia] ` no assunto e o mesmo texto;
- com `HOTEL_TEST_MODE=true`, a mensagem vai para a caixa de testes, com `[TESTE] ` no assunto e a primeira linha
  "Destinatário original: …", sem cópia, e o bloqueio continua igual;
- o resto do sistema continua igual ao da base.

Fora do roteiro, a leitura do código mostra que endereços como `ana@gmail.` e `ana@.com` seriam aceitos e enviados
(`EmailSender.java:44-45`), quando o pedido quer o ponto "no meio" do domínio. Para o dono, isso quer dizer que um
erro de digitação desse tipo ainda chegaria a lugar nenhum sem que a recepção ficasse sabendo. Esse defeito já pesou
em Design e em Testes e não muda a nota desta coluna. Há outro ponto, que o texto deixou em aberto: no modo de teste, o
agente põe uma linha em branco depois de "Destinatário original: …" (`EmailSender.java:27`), e o roteiro aceita.

## Dúvidas

- Não está claro se a leitura do modo de teste dentro de `EmailSender` (`EmailSender.java:12`), e não na montagem,
  deve pesar na arquitetura ou só no design. Contei nas duas: na arquitetura, como montagem fora do lugar; no design,
  como parte da omissão.
