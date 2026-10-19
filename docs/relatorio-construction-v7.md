# Relatório da construction-v7: cada cenário, cada modelo, cada braço

19 de outubro de 2026. Este relatório traz os resultados das 45 execuções da `construction-v7` e das 45 correções
delas.

**Como o experimento foi feito.**
- **A tarefa:** o agente recebe uma pasta vazia e o enunciado de **um** cenário, e constrói do zero o sistema de
  reservas da Pousada Maré Alta. A pilha é Java 21 e Javalin 6.7.0, sem Spring, com os dados em memória.
- **O enunciado:** descreve o sistema inteiro na voz do dono. No fim vem uma parte que pede um pattern, sem dizer o
  nome:
  - 01 pede o Strategy;
  - 03 pede o Observer;
  - 05 pede o Factory Method;
  - 07 pede o Decorator;
  - 09 pede o Template Method.
- **Os modelos:** Claude Haiku 4.5, Claude Sonnet 4.5 e Claude Sonnet 5.5, com effort `medium` e uma execução por
  combinação.
- **A correção:** feita pelo Claude Opus 5.5, effort `high`, às cegas: a IA não sabe qual modelo nem qual braço fez o
  projeto. Ela lê o código, sobe o serviço e testa pela API.

**Os braços.**
- **N0:** só o enunciado.
- **N1:** um `CLAUDE.md` com dois princípios:
  - "para acrescentar o próximo caso parecido, quantos lugares do código teriam de mudar? (…) organize para que o
    próximo caso entre num lugar só";
  - "faça essa organização só onde o enunciado mostra que a variação existe e vai continuar".
- **N2:** o mesmo `CLAUDE.md`, com uma linha a mais ("Antes de decidir a estrutura, consulte a skill
  `gof-patterns`"), e a skill na pasta.

## Como ler

Cada execução recebe três conclusões.

**O desenho.** Diz como a parte do fim do enunciado foi construída:
- **acerto:** o pattern foi aplicado direito, em todos os casos;
- **alternativa equivalente:** outra estrutura que as instruções do pattern aceitam;
- **malfeito:** a estrutura existe, mas convive com o jeito sem ela;
- **omissão:** a parte foi construída sem o pattern (`if`, `switch` ou cópia).

**O exagero colateral.** Registra estrutura sem necessidade fora dessa parte:
- **contenção:** não há;
- **exagero:** há.

**O comportamento.** Diz se o que o enunciado pede funciona quando a IA chama a API:
- **segue;**
- **em parte;**
- **não segue;**
- **não se aplica.**

**De onde vem a influência do harness.**
- **N1:** comparo com o N0 do mesmo modelo e procuro no que o agente escreveu ao trabalhar se ele cita o
  `CLAUDE.md`.
- **N2:** comparo com o N1 e confiro no registro da execução se a skill foi aberta, com que pergunta e o que foi lido
  dela.

Com uma execução só por combinação, a diferença entre dois braços de um mesmo modelo pode ser acaso. Por isso, onde
digo que o `CLAUDE.md` ou a skill "influenciou", estou descrevendo o que mudou e o que o agente disse. Isso não prova a
causa.

A correção completa de cada execução está em
`construction-v7/evaluation/evaluator/<pattern>/runs/<cenário>-<braço>/opus-5-5-high/<código>/answer.md`. O código
(`run-01`, `run-02` ou `run-03`) vem entre parênteses em cada título abaixo.

## O quadro geral

| cenário | modelo | N0 | N1 | N2 |
|---|---|---|---|---|
| 01 Strategy | Haiku 4.5 | acerto | acerto | acerto |
| | Sonnet 4.5 | malfeito | acerto | omissão (pasta vazia) |
| | Sonnet 5.5 | acerto | acerto | acerto |
| 03 Observer | Haiku 4.5 | omissão | malfeito | omissão |
| | Sonnet 4.5 | acerto | acerto (+ exagero) | acerto |
| | Sonnet 5.5 | acerto | acerto | acerto |
| 05 Factory Method | Haiku 4.5 | omissão | acerto | acerto |
| | Sonnet 4.5 | acerto | acerto | acerto |
| | Sonnet 5.5 | acerto | acerto | acerto |
| 07 Decorator | Haiku 4.5 | omissão | omissão | omissão |
| | Sonnet 4.5 | omissão | omissão | omissão (pasta vazia) |
| | Sonnet 5.5 | malfeito | malfeito | alternativa equivalente |
| 09 Template Method | Haiku 4.5 | omissão | omissão | omissão |
| | Sonnet 4.5 | omissão | omissão | omissão |
| | Sonnet 5.5 | malfeito | acerto | acerto (+ exagero) |

**Por braço:**

| braço | acerto | alternativa | malfeito | omissão |
|---|---|---|---|---|
| N0 | 6 | 0 | 3 | 6 |
| N1 | 9 | 0 | 2 | 4 |
| N2 | 8 | 1 | 0 | 6 (2 delas, pastas vazias) |

**Por modelo:**

| modelo | acerto | alternativa | malfeito | omissão | comportamento `segue` |
|---|---|---|---|---|---|
| Haiku 4.5 | 5 | 0 | 1 | 9 | 0 de 15 |
| Sonnet 4.5 | 7 | 0 | 1 | 7 | 1 de 15 |
| Sonnet 5.5 | 11 | 1 | 3 | 0 | 15 de 15 |

O exagero colateral apareceu duas vezes em 45: no Sonnet 4.5 N1 do cenário 03 e no Sonnet 5.5 N2 do 09.

---

## Cenário 01: os planos de tarifa (Strategy)

O enunciado traz cinco planos: o normal, o não reembolsável, o convênio com empresas, o pacote de fim de semana e a
estadia longa. Cada um tem o seu preço, a sua recusa e a sua devolução, e o comercial "vive inventando" planos novos.
O acerto é uma interface de plano com uma implementação por plano, os dois da parte comum inclusive, e um serviço que
só pede ao plano, sem perguntar qual ele é.

### Haiku 4.5

**N0: acerto, contenção, comportamento em parte** (`run-02`).
- **O desenho:** uma interface `PricingStrategy`, uma classe por plano, e um `switch` que só escolhe a classe. O
  serviço não cita nenhum plano.
- **As ressalvas, que não mudaram a classe:**
  - a devolução não é um método do plano: é um campo que volta junto com o preço, e o cancelamento refaz o cálculo
    inteiro para chegar nele;
  - o laço que soma as noites está copiado em quatro classes.
- **Por que o comportamento falhou:** as regras dos planos conferem todas. As falhas são da parte comum:
  - o dinheiro sai `R$ 820.00` e `R$ 1749.30` no comprovante, porque o `String.format` não tem a vírgula nem o ponto
    do milhar;
  - o relatório financeiro lista reservas que devolveram tudo, com valor 0.00;
  - o JSON sai com uma casa só (`779.8`);
  - a segunda via de reserva pendente dá 400 em vez de 409.

**N1: acerto, contenção, em parte** (`run-01`).
- **O desenho:** uma classe abstrata `RatePlan` com uma subclasse por plano, e um `switch` que só instancia a classe.
- **As ressalvas:** a cópia piorou um pouco:
  - o laço das noites continua repetido em quatro planos;
  - a checagem de capacidade, que vale para todos, foi copiada para dentro do `validate` de cada plano.
- **Por que o comportamento falhou:**
  - o dinheiro sai `R$ 1.159.80`;
  - o check-out e a segunda via devolvem texto puro, e não o JSON combinado;
  - um corpo que não é JSON, um campo faltando e `guests` 0 dão 500 em vez de 400;
  - o financeiro lista reservas canceladas com 0.00;
  - o mês `2027-13` é aceito.
- **Como o `CLAUDE.md` influenciou:** o agente o citou logo no começo: "desenhar o código para que novos planos de
  tarifa entrem 'num lugar só' (como o CLAUDE.md sugere)". O resultado é o mesmo desenho do N0, que já estava certo.
  O princípio guiou a intenção, mas não mudou a classe, e a cópia dentro dos planos continuou.

**N2: acerto, contenção, em parte** (`run-01`).
- **O desenho:** uma interface `RatePlan` com uma classe por plano, registradas num mapa.
- **A ressalva:** o laço das noites ainda está copiado em quatro classes.
- **Por que o comportamento falhou:**
  - a confirmação exige no corpo um campo `amountPaid` que o enunciado não prevê: chamada sem corpo, como o site
    chama, ela dá 400, e com corpo ela aceita qualquer valor como pago;
  - o dinheiro sai `R$ 779.80`;
  - o financeiro lista reservas que não deixaram dinheiro;
  - a ocupação conta reservas pendentes;
  - a segunda via sai para reserva cancelada;
  - o agente não escreveu nenhum teste.
- **Como o `CLAUDE.md` e a skill influenciaram:**
  - **a skill:** não foi aberta, apesar da linha que manda consultá-la;
  - **o `CLAUDE.md`:** aparece na fala do agente ("para que novos planos entrem facilmente em um único lugar").

  O desenho é igual ao do N0 e do N1.

### Sonnet 4.5

**N0: malfeito, contenção, em parte** (`run-03`).
- **O desenho:** a estrutura existe: a interface `RatePlanStrategy`, uma classe por plano e um `EnumMap` que escolhe.
- **Por que é malfeito:** a interface não recebe o código da empresa. Então, na criação da reserva, o serviço pergunta
  "o plano é `CORPORATE`?", faz um cast para a classe do convênio e chama outro método, que recebe o código. O
  `calculatePrice` da própria interface, no convênio, calcula sem o desconto. O preço só sai certo porque o serviço
  desvia da interface. As instruções do pattern dão este caso como exemplo de malfeito: "quem usa a estrutura ainda
  pergunta qual é o caso concreto".
- **Por que o comportamento falhou:** as regras dos planos funcionam. Na parte comum:
  - o dinheiro sai `R$ 779.80` e `R$ 2.087.64`;
  - um corpo que não é JSON, um plano desconhecido ou uma data mal formada dão 500;
  - a segunda via de uma reserva pendente dá 400, e a de uma cancelada depois de paga sai com 200.

**N1: acerto, contenção, em parte** (`run-03`).
- **O desenho:** uma interface `RatePlan` com `validate`, `calculatePrice` e `calculateRefund`, uma classe por plano e
  um `EnumMap` único. O serviço só delega, sem nenhum `if` sobre o plano.
- **A ressalva:** o código da empresa entrou na assinatura de todos os planos, embora só o convênio o use.
- **Por que o comportamento falhou:**
  - o comprovante usa o idioma padrão da máquina, que é o inglês, e o dinheiro sai `R$ 1,749.30`;
  - um corpo que não é JSON, um plano desconhecido, um `ratePlan` ausente e uma data fora do formato dão 500;
  - uma reserva sem `guestName` é criada com `null`;
  - o financeiro lista reservas que não deixaram dinheiro.
- **Como o `CLAUDE.md` influenciou:** o furo do N0 sumiu. O código da empresa entrou na interface, e o serviço parou
  de perguntar qual é o plano. O agente não citou o `CLAUDE.md`: no fim, chamou o desenho de "Strategy Pattern". A
  mudança vai na direção do primeiro princípio (nenhuma classe conhecendo o que não é dela), mas, com uma execução
  só, não dá para separar isso do acaso.

**N2: omissão, contenção, não segue (pasta vazia)** (`run-03`).
- **O que aconteceu:** o agente não deixou nenhum arquivo. Ele abriu a skill passando como argumento uma descrição
  do problema ("Preciso modelar planos de tarifa onde cada um tem: 1) regras de validação diferentes…"). A resposta
  que veio virou a resposta final da execução: uma explicação do Strategy com código de exemplo, dirigida ao dono
  ("Para seu caso de planos de tarifa, o padrão ideal é Strategy"). Depois disso, ele parou. Foram 3 turnos, 51
  segundos e US$ 0,10.
- **Por que não segue:** não há sistema para testar. A própria IA da correção desconfiou de uma falha na cópia da
  pasta, mas eu conferi o registro da execução: a pasta veio vazia mesmo. Não foi falha da infraestrutura, e
  também não foi só o modelo (veja "O erro da skill aberta com uma pergunta", no fim).
- **Como o `CLAUDE.md` e a skill influenciaram:** a linha do N2 ("consulte a skill") levou a uma consulta. A
  pergunta que o modelo passou para a skill voltou como se fosse uma mensagem do dono, e ele a respondeu e parou. O
  mesmo aconteceu no cenário 07, abaixo.

### Sonnet 5.5

**N0: acerto, contenção, segue** (`run-01`).
- **O desenho:** uma interface `RatePlan` e cinco planos registrados por código. Três deles (o normal, o não
  reembolsável e a estadia longa) são instâncias de uma mesma classe com parâmetros, e os outros dois têm classe
  própria. O serviço não nomeia nenhum plano.
- **A ressalva:** o código da empresa está na assinatura geral `quote(...)`, e um plano futuro que precise de outro
  dado vai obrigar a mudar a interface.
- **O comportamento:** tudo o que foi testado confere.

**N1: acerto, contenção, segue** (`run-02`).
- **O desenho:** uma classe por plano, inclusive o normal e o não reembolsável, e um catálogo único.
- **A ressalva:** um campo `bookedOn`, com o relógio injetado, foi preparado para um plano futuro de antecedência, e
  nenhum plano o usa. É um campo, e não uma estrutura, então não contou como exagero.
- **O comportamento:** tudo confere.
- **Como o `CLAUDE.md` influenciou:** o modelo já acertava no N0. A mudança foi de forma: em vez de uma classe com
  parâmetros, uma classe por plano. Apareceu também um pequeno preparo para o que ninguém pediu, o `bookedOn`, que o
  segundo princípio desaconselha. O agente não citou o `CLAUDE.md`.

**N2: acerto, contenção, segue** (`run-02`).
- **O desenho:** é o mesmo do N1, com uma classe por plano, num catálogo.
- **A ressalva:** a soma das noites é feita no serviço antes de chamar o plano, e o pacote recebe essa soma e a
  ignora.
- **O comportamento:** tudo confere.
- **Como o `CLAUDE.md` e a skill influenciaram:** abriu a skill sem argumento e seguiu construindo. Foram 8 turnos e
  2,4 minutos. Não há diferença visível em relação ao N1.

---

## Cenário 03: avisar a equipe (Observer)

Quando uma reserva é confirmada, além do e-mail ao hóspede, saem avisos à governança e ao financeiro. No cancelamento
de uma reserva confirmada, a governança é avisada, e o financeiro também, se houver o que devolver. O restaurante e o
transfer "também vão querer saber". O acerto é o serviço anunciar o evento sem saber quem é avisado: uma interface de
ouvinte, uma implementação por interessado (o hóspede inclusive) e a lista registrada na montagem do sistema.

### Haiku 4.5

**N0: omissão, contenção, em parte** (`run-01`).
- **O desenho:** os e-mails da confirmação e do cancelamento são montados e enviados um depois do outro, em dois
  métodos privados do `ReservationService`. Não há ouvinte nem lista, e para avisar o restaurante seria preciso
  editar o serviço.
- **O pior ponto do desenho:** a regra do reembolso (o valor e o `setRefund`) ficou dentro do método que manda o
  e-mail do financeiro.
- **Por que o comportamento falhou:** os avisos saem certos, mas o `refund` fica `null` quando não há o que devolver.
  Na parte comum:
  - dezembro inteiro é cobrado como alta temporada, e não a partir do dia 15;
  - o dinheiro sai sem o ponto do milhar (`R$ 2319,60`);
  - a ocupação conta reservas pendentes;
  - o financeiro lista canceladas com 0.0.

**N1: malfeito, contenção, em parte** (`run-03`).
- **O desenho:** a estrutura do Observer apareceu:
  - uma interface `NotificationHandler`;
  - uma classe por interessado e por evento;
  - o hóspede também como ouvinte;
  - o serviço só anunciando.
- **Por que é malfeito:** quem distribui os eventos (o `NotificationService`) cria e registra os cinco ouvintes no
  próprio construtor. Além disso, a interface não é pública, então nem o `main` conseguiria registrar um ouvinte
  novo. Para o restaurante entrar, seria preciso editar o distribuidor, que funciona como um notificador central que
  conhece todos os destinos.
- **Por que o comportamento falhou:**
  - o valor pago vem do corpo da requisição: sem corpo, a confirmação dá 500, e com `{}` ela registra pagamento de
    R$ 0,00;
  - o dinheiro sai sem o ponto do milhar;
  - as datas saem no JSON como `[2027,7,10]`;
  - um corpo que não é JSON dá 500;
  - o financeiro lista uma reserva reembolsada com 0.00.
- **Como o `CLAUDE.md` influenciou:** o desenho passou da omissão para o malfeito. A estrutura de ouvintes apareceu,
  que é a direção do primeiro princípio, mas a lista ficou presa num lugar central. O agente não citou o `CLAUDE.md`.
  No fim, disse ao dono que a "arquitetura [está] pronta para adicionar restaurante, transfer", o que o próprio
  desenho não cumpre.

**N2: omissão, contenção, em parte** (`run-01`).
- **O desenho:** os avisos voltaram a ser métodos privados chamados em sequência dentro do `ReservationService`, como
  no N0.
- **Por que o comportamento falhou:** o mais grave é que o cancelamento de uma reserva confirmada nunca avisa
  ninguém. O serviço muda o status para `CANCELLED` e só depois pergunta se a reserva estava `CONFIRMED`. Além disso:
  - dezembro inteiro é alta temporada;
  - o dinheiro sai `R$ 779.80`;
  - o financeiro lista uma reembolsada com 0.00.
- **Como o `CLAUDE.md` e a skill influenciaram:**
  - **a skill:** não foi aberta;
  - **o `CLAUDE.md`:** o agente não o citou, e o resultado voltou ao do N0.

### Sonnet 4.5

**N0: acerto, contenção, em parte** (`run-02`).
- **O desenho:** o serviço publica dois eventos, e há três ouvintes separados: o hóspede, a governança e o
  financeiro. O cancelamento passa pela mesma estrutura.
- **A ressalva:** é a mesma do Haiku N1. Os ouvintes são criados no construtor do `NotificationService`, e não na
  montagem. A IA ficou entre acerto e malfeito e escolheu acerto, porque nenhum caso escapa da estrutura e a
  interface é pública (veja "Observações sobre a correção", no fim).
- **Por que o comportamento falhou:** a parte do fim confere inteira. Na parte comum:
  - um corpo que não é JSON e um `ratePlan` inválido dão 500;
  - os zeros saem `0` em vez de `0.00`.

**N1: acerto, exagero, em parte** (`run-01`).
- **O desenho:** o serviço recebe o `EventBus` pelo construtor, e os três ouvintes são registrados no `main`. É o
  Observer completo.
- **O exagero:** um `RoomService` que só repassa chamadas ao repositório.
- **Por que o comportamento falhou:**
  - o dinheiro no JSON sai com uma casa (`779.8`);
  - o "R$" do e-mail vem seguido de um espaço não separável (o caractere U+00A0), e não do espaço comum.
- **Como o `CLAUDE.md` influenciou:** a ressalva do N0 sumiu. A lista saiu do construtor e foi para a montagem,
  recebida de fora, o que é exatamente "o próximo caso entra num lugar só, sem mexer no que já funciona". Ao mesmo
  tempo apareceu a camada que só repassa, contra o segundo princípio. O agente não citou o `CLAUDE.md`. Disse que
  organizou "para que novos avisos (restaurante, transfer, etc.) entrem sem mexer no código que já funciona", quase
  com as palavras do arquivo.

**N2: acerto, contenção, em parte** (`run-03`).
- **O desenho:** um publicador injetado no serviço e três observadores registrados no `main`.
- **Por que o comportamento falhou:** a parte do fim confere toda. Na parte comum:
  - falta a linha em branco antes de "A entrada é a partir das 14h" no e-mail;
  - o financeiro escreve `509.8` em vez de `509.80`;
  - um corpo que não é JSON dá 500;
  - a segunda via sai para reserva cancelada.
- **Como o `CLAUDE.md` e a skill influenciaram:** este é o único caso em que um agente abriu a skill e leu o arquivo
  do pattern, `observer.md`. Depois disse "Vou usar o padrão Observer" e montou o Observer de manual. Sem o exagero
  do N1. O resultado do desenho é o mesmo do N1, agora com o nome do pattern.

### Sonnet 5.5

**N0: acerto, contenção, segue** (`run-03`).
- **O desenho:** o serviço publica num `EventBus` que recebe pelo construtor. Os cinco destinos são inscrições
  separadas, feitas na montagem: três na confirmação e dois no cancelamento.
- **A ressalva:** as cinco inscrições são métodos de uma mesma classe, `Notifications`.
- **O comportamento:** tudo confere.

**N1: acerto, contenção, segue** (`run-02`).
- **O desenho:** uma interface `OuvinteReserva`, uma classe por interessado, e a lista montada fora do serviço. É o
  Observer de manual. O agente escreveu o código em português.
- **O comportamento:** tudo confere.
- **Como o `CLAUDE.md` influenciou:** o desenho passou de "métodos de uma mesma classe" para "uma classe por
  interessado", mais próximo do pattern de livro. O agente não citou o `CLAUDE.md`.

**N2: acerto, contenção, segue** (`run-02`).
- **O desenho:** uma interface `ReservationListener`, três notificadores e o registro na montagem.
- **O comportamento:** tudo confere.
- **Como o `CLAUDE.md` e a skill influenciaram:** abriu a skill sem argumento. O desenho é igual ao do N1.

---

## Cenário 05: o documento por tipo de hóspede (Factory Method)

O hóspede se identifica de um jeito só: CPF, CNPJ com razão social ou passaporte com país. Cada jeito recebe um
documento diferente:
- **CPF:** o comprovante;
- **CNPJ:** a fatura, com ISS;
- **passaporte:** o *booking receipt*, em inglês.

O documento sai em três lugares (no e-mail, no check-out e na segunda via), e as agências vão ter o seu. O acerto é a
escolha do documento num lugar só, que os três pontos usam. Pode ser o Factory Method clássico, uma fábrica
concentrada ou um método polimórfico no tipo de hóspede.

### Haiku 4.5

**N0: omissão, contenção, em parte** (`run-02`).
- **O desenho:** a escolha é uma cadeia de `if/else` sobre os campos nulos, copiada em dois lugares: no e-mail e no
  objeto de resposta do check-out e da segunda via. Os textos ficam em funções separadas, mas nada concentra a
  decisão, e a agência exigiria mexer nas duas cópias.
- **Por que o comportamento falhou:**
  - os campos que deviam vir `null` somem do JSON;
  - o `title` é sempre "Comprovante de reserva";
  - a alta temporada conta dezembro inteiro e usa o preço da noite de entrada para todas as noites;
  - uma reserva que termina no dia em que outra começa é recusada;
  - um corpo que não é JSON dá 500;
  - a segunda via sai para reserva cancelada;
  - o financeiro lista uma reembolsada com 0.00.

**N1: acerto, contenção, em parte** (`run-03`).
- **O desenho:** um método estático, `DocumentGenerator.generateDocument`, olha o tipo de identificação e monta o
  documento. Os três lugares o chamam. É uma fábrica concentrada.
- **A ressalva:** os objetos de resposta ainda perguntam o tipo para preencher os campos.
- **Por que o comportamento falhou:**
  - a confirmação exige `amountPaid` no corpo: sem corpo dá 500, e com corpo aceita qualquer valor;
  - não há ponto do milhar;
  - os campos `null` somem do JSON;
  - um CPF acompanhado de um CNPJ incompleto é aceito;
  - um corpo que não é JSON dá 500;
  - a ocupação conta reservas pendentes;
  - o financeiro lista reservas com 0.00;
  - o agente não escreveu nenhum teste.
- **Como o `CLAUDE.md` influenciou:** é o caso mais claro da pesquisa. A decisão saiu das duas cópias do N0 e foi
  para um lugar só, que é a pergunta do primeiro princípio ("quantos lugares do código teriam de mudar?"). O agente
  não citou o `CLAUDE.md`.

**N2: acerto, contenção, em parte** (`run-02`).
- **O desenho:** `DocumentService.selectGenerator` escolhe uma de três implementações de `DocumentGenerator`. Os
  três lugares pedem o documento a ela.
- **Por que o comportamento falhou:**
  - os campos `null` somem do JSON;
  - dezembro inteiro é alta temporada;
  - a segunda via sai para reserva cancelada;
  - o financeiro lista reservas que não deixaram dinheiro;
  - um `guestCpf` vazio, junto com um CNPJ válido, gera um comprovante "CPF " em branco.
- **Como o `CLAUDE.md` e a skill influenciaram:**
  - **a skill:** foi o único Haiku que a abriu (1 de 5);
  - **o `CLAUDE.md`:** o agente o citou ("seguindo as orientações do CLAUDE.md, consultando padrões de design antes de
    decidir a arquitetura, e garantindo que novas variações (…) entrem em um lugar só");
  - **o nome que ele deu:** "Strategy Pattern para os documentos", mas o que construiu é a fábrica concentrada.

  A classe é a mesma do N1. A diferença é uma classe por documento, em vez de três funções.

### Sonnet 4.5

**N0: acerto, contenção, em parte** (`run-03`).
- **O desenho:** um método privado, `generateDocument`, com `if/else` sobre o tipo, chamado pelos três lugares.
- **Por que o comportamento falhou:**
  - os reais saem sem o ponto do milhar (`R$ 1159,80`);
  - um CPF com um CNPJ incompleto é aceito;
  - a segunda via sai para reserva cancelada;
  - um corpo que não é JSON dá 500;
  - o financeiro lista uma reembolsada com 0.00.

**N1: acerto, contenção, em parte** (`run-01`).
- **O desenho:** um mapa que liga o tipo de identificação ao gerador, com uma classe por documento, consultado por um
  único método.
- **Por que o comportamento falhou:**
  - o "R$" vem seguido do espaço não separável;
  - um corpo sem `ratePlan` dá 500, mas a reserva fica gravada, quebrada, e bloqueia o quarto;
  - sem `checkIn`, dá 500;
  - sem `guestName`, a reserva é aceita.
- **Como o `CLAUDE.md` influenciou:** o desenho passou de `if/else` para um registro com uma classe por documento, e
  a classe ficou a mesma. O agente citou o arquivo no fim: "novos tipos de hóspede/documento possam ser adicionados
  em um único lugar, seguindo as orientações do CLAUDE.md".

**N2: acerto, contenção, em parte** (`run-03`).
- **O desenho:** `determineDocumentGenerator` escolhe, com `if`, um de três geradores, que fica guardado na
  reserva.
- **Por que o comportamento falhou:**
  - o espaço não separável depois do "R$";
  - o `discount` vem `null` no plano normal;
  - as datas do comprovante vêm no formato `10/03/2027`, e não no ISO.
- **Como o `CLAUDE.md` e a skill influenciaram:**
  - **a skill:** foi aberta sem argumento;
  - **o `CLAUDE.md`:** o agente o citou;
  - **o nome:** chamou o desenho de "Strategy Pattern".

  O resultado é da mesma classe do N0 e do N1. Foi a execução mais longa do modelo: 14 minutos.

### Sonnet 5.5

**N0: acerto, contenção, segue** (`run-01`).
- **O desenho:** a identificação é uma `sealed interface` com três `record`s. Um único `Documents.render`, com um
  `switch` exaustivo, cria o documento, e o compilador obriga a tratar um tipo novo.
- **O comportamento:** tudo confere.

**N1: acerto, contenção, segue** (`run-02`).
- **O desenho:** cada tipo de identificação é uma classe que sabe emitir o seu documento, num método polimórfico. Um
  registro de tipos decide pelo pedido, sem nenhum `if`.
- **O comportamento:** tudo confere.
- **Como o `CLAUDE.md` influenciou:** o desenho passou de um `switch` central para o polimorfismo no próprio tipo.
  Na resposta final, o agente explicou ao dono que, "para a agência de turismo, basta criar uma classe nova e
  registrá-la". Não citou o `CLAUDE.md`.

**N2: acerto, contenção, segue** (`run-01`).
- **O desenho:** é o mesmo do N1, com `Identity.render` e um registro `IdentityTypes`.
- **O comportamento:** tudo confere.
- **Como o `CLAUDE.md` e a skill influenciaram:** abriu a skill e não houve mudança visível.

---

## Cenário 07: as regras de envio (Decorator)

O envio de e-mail ganha três regras:
- o endereço inválido é bloqueado;
- no modo de teste (`HOTEL_TEST_MODE`), a mensagem vai para a caixa de testes;
- fora do teste, sai uma cópia para o arquivo.

O dono diz que vão vir outras regras (madrugada, limite por dia), cada uma valendo em modos diferentes. O acerto é
cada regra embrulhar o envio, com a montagem decidindo quais entram em cada modo. A corrente de regras e a lista de
filtros também são aceitas, desde que a combinação por modo seja decidida na montagem, e não dentro das regras.

Este foi o cenário mais difícil: nenhum acerto literal.

### Haiku 4.5

**N0: omissão, contenção, em parte** (`run-03`).
- **O desenho:** as três regras são três `if` dentro de `MessageService.sendMessage`, e o modo é um `boolean` do
  próprio serviço.
- **Por que o comportamento falhou:**
  - a confirmação, chamada sem corpo, dá 500, e nenhuma mensagem sai;
  - o valor pago vem do cliente;
  - `ana@.com` é aceito;
  - um corpo que não é JSON dá 500;
  - uma reserva com entrada igual à saída é aceita com 0 noites;
  - a segunda via sai para reserva cancelada.

**N1: omissão, contenção, em parte** (`run-03`).
- **O desenho:** o mesmo: três `if` em `MessageService.sendEmail`, com o modo num `boolean` do serviço.
- **Por que o comportamento falhou:** as regras do fim conferem. Na parte comum:
  - a ocupação conta reservas pendentes e sai fora de ordem;
  - o check-out e a segunda via devolvem texto puro, e não JSON;
  - `guests` 0 dá 422 em vez de 400;
  - os zeros saem `0`.
- **Como o `CLAUDE.md` influenciou:** nada mudou no desenho, e o agente não o citou.

**N2: omissão, contenção, em parte** (`run-02`).
- **O desenho:** os mesmos `if`, e o serviço lê `HOTEL_TEST_MODE` sozinho.
- **Por que o comportamento falhou:**
  - a confirmação exige `amountPaid` e grava o valor que veio;
  - o dinheiro sai `R$ 599.76`;
  - há uma linha em branco a mais depois de "Destinatário original".
- **Como o `CLAUDE.md` e a skill influenciaram:** a skill não foi aberta, e nada mudou.

### Sonnet 4.5

**N0: omissão, contenção, em parte** (`run-01`).
- **O desenho:** três `if` dentro de `MessageService.sendEmail`, e o serviço lê a variável de ambiente.
- **Por que o comportamento falhou:**
  - um corpo que não é JSON dá 500;
  - o financeiro lista uma reembolsada com 0.00;
  - o espaço não separável depois do "R$";
  - os zeros saem `0`;
  - `h@.gmail.com` é aceito, o que o enunciado não decide sozinho.

**N1: omissão, contenção, em parte** (`run-01`).
- **O desenho:** o agente escreveu uma interface `MessageRule` e três classes de regra, mas nada as usa. O envio de
  verdade passa por `MessageProcessor.process`, que resolve as três regras com `if/else` num método só. As classes
  esquecidas também não seriam embrulhos, porque cada uma recebe o modo e decide por dentro se vale.
- **Por que o comportamento falhou:**
  - `ana@.com` e `ana@gmail.` passam como válidos, porque o código só confere se há um ponto no domínio;
  - o espaço não separável depois do "R$".
- **Como o `CLAUDE.md` influenciou:** este é o caso que mostra o limite do primeiro princípio. O agente escreveu:
  "O sistema foi organizado seguindo os princípios do CLAUDE.md: MessageProcessor centraliza a lógica de mensagens,
  novas regras entram em um único lugar". Ele leu "num lugar só" como "num método só". O princípio ficou cumprido na
  letra (um lugar) e não na intenção ("sem mexer no que já funciona"). A estrutura começada e abandonada também
  sugere que ele chegou a tentar.

**N2: omissão, contenção, não se aplica (pasta vazia)** (`run-03`).
- **O que aconteceu:** foi igual ao cenário 01. O agente abriu a skill descrevendo o problema ("Preciso implementar
  um sistema de envio de mensagens com várias regras…"). A resposta que veio, uma explicação do Chain of
  Responsibility, com o Decorator como alternativa, virou a resposta final. Ele parou em 3 turnos e 47 segundos, sem
  nenhum arquivo.
- **Por que não se aplica:** não há o que testar. Para a mesma situação, a correção do cenário 01 escolheu `não
  segue`. Para contar, as duas valem como "nada entregue".
- **Como o `CLAUDE.md` e a skill influenciaram:** a skill, aberta com uma pergunta, tomou o lugar da tarefa.

### Sonnet 5.5

**N0: malfeito, contenção, segue** (`run-02`).
- **O desenho:** as regras viraram uma lista de três peças (classes anônimas com uma interface `Rule`), aplicadas em
  ordem. É perto da lista de filtros aceita.
- **Por que é malfeito:**
  - cada regra declara dentro de si em que modo vale (`scope()`);
  - o envio faz um `switch` sobre o modo de teste para decidir quais regras rodam;
  - a lista é fixa dentro do serviço, e o `main` só repassa um booleano.

  As instruções dão as duas coisas como malfeito: o modo escrito nas regras e o envio que ainda pergunta pelo modo.
- **O comportamento:** tudo confere.

**N1: malfeito, contenção, segue** (`run-02`).
- **O desenho:** um pacote próprio de regras, com uma classe por regra e um `MessagePipeline` que aplica a lista. O
  problema é o mesmo do N0:
  - cada regra traz fixo o modo em que vale (`LIVE_ONLY`, `TEST_ONLY`);
  - a montagem põe as três regras nos dois modos;
  - o pipeline recebe o modo e pergunta a cada regra se ela vale agora.
- **O comportamento:** tudo confere.
- **Como o `CLAUDE.md` influenciou:** as regras viraram classes de verdade, em vez de classes anônimas, e ganharam um
  pacote próprio. O ponto que decide a classe, onde mora a escolha do modo, não mudou. O agente não citou o
  `CLAUDE.md`.

**N2: alternativa equivalente, contenção, segue** (`run-01`).
- **O desenho:** uma corrente de regras. Cada regra recebe a mensagem e o "próximo", e nenhuma pergunta pelo modo. Um
  único ponto da montagem, `DispatchRules.forMode`, diz quais regras valem em cada modo (sempre, só no teste ou só
  fora dele). Para incluir "não mandar de madrugada", basta uma classe nova e uma linha nesse ponto.
- **A ressalva:** a cópia para o arquivo é criada depois do retorno da corrente, então uma regra futura posta depois
  dela não a alcançaria.
- **O comportamento:** tudo confere. Há um campo `sent` a mais nas mensagens.
- **Como o `CLAUDE.md` e a skill influenciaram:** o agente disse "Vou consultar a skill `gof-patterns`, como o
  CLAUDE.md pede, antes de decidir a estrutura". É a única execução do cenário que chegou a uma estrutura aceita. A
  diferença em relação ao N1 é justamente o ponto que faltava: a escolha do modo saiu das regras e foi para a
  montagem. Entre todos os casos da pesquisa, é o mais claro de uma melhora com a skill.

---

## Cenário 09: os relatórios (Template Method)

O enunciado traz quatro relatórios em CSV, que seguem os mesmos passos: o título, o cabeçalho, uma linha por reserva
e o total. Dois vêm da parte comum (a ocupação e o financeiro) e dois do cenário (a taxa de turismo e o relatório por
plano), e o contador vai pedir mais. O acerto é o esqueleto num lugar só, num método-modelo, com cada relatório
preenchendo só o que muda. Também é aceito um escritor genérico que receba as partes que variam.

### Haiku 4.5

**N0: omissão, contenção, em parte** (`run-01`).
- **O desenho:** quatro métodos do `ReportService`, cada um com a sequência inteira. A linha de título está copiada
  quatro vezes, e a seleção das reservas pagas do mês, três.
- **Por que o comportamento falhou:**
  - o financeiro e o relatório por plano deixam de fora a reserva não reembolsável cancelada, que o enunciado manda
    contar: o total sai 3482.80, e não 4675.89;
  - a ocupação conta uma reserva pendente;
  - o dinheiro sai `R$ 779.80`;
  - a temporada é decidida só pela data de entrada;
  - o `refund` fica `null`;
  - uma reserva inexistente dá 400 em vez de 404;
  - a segunda via sai para reserva cancelada.

**N1: omissão, contenção, em parte** (`run-01`).
- **O desenho:** quatro métodos inteiros dentro do `ReservationService`.
- **Por que o comportamento falhou:**
  - a confirmação exige um corpo: sem corpo dá 400, e com `{}` grava `amountPaid` nulo, o que depois faz o financeiro
    responder 400;
  - os zeros saem `0` nos relatórios;
  - uma reserva não reembolsável cancelada ainda pendente entra no financeiro.
- **Como o `CLAUDE.md` influenciou:** nada mudou, e o agente não o citou.

**N2: omissão, contenção, em parte** (`run-03`).
- **O desenho:** quatro métodos no `ReportService`, com o título copiado quatro vezes.
- **Por que o comportamento falhou:**
  - a confirmação sem corpo dá 500 e nunca confirma nada;
  - a ocupação e o relatório por plano contam reservas pendentes;
  - o financeiro lista linhas com 0.00;
  - o dinheiro sai `R$ 1403.64`;
  - um campo faltando ou um corpo que não é JSON dá 500;
  - `guests` 0 dá 422;
  - o agente não escreveu nenhum teste.
- **Como o `CLAUDE.md` e a skill influenciaram:** a skill não foi aberta, e nada mudou.

### Sonnet 4.5

**N0: omissão, contenção, em parte** (`run-03`).
- **O desenho:** quatro métodos com a sequência inteira e o título copiado quatro vezes.
- **Por que o comportamento falhou:** os quatro relatórios estão 100% certos. As falhas são da parte comum:
  - um corpo que não é JSON dá 500;
  - os zeros saem `0`;
  - o espaço não separável depois do "R$".

**N1: omissão, contenção, segue** (`run-02`).
- **O desenho:** uma interface `ReportGenerator`, com quatro implementações guardadas num mapa pelo nome. A interface
  só dá a mesma assinatura aos relatórios: cada classe escreve de novo o título, o cabeçalho, o laço e o total. A
  linha "Pousada Maré Alta,…" está copiada nas quatro, e a formatação de números, em três.
- **O comportamento:** tudo confere, menos os zeros como `0`.
- **Como o `CLAUDE.md` influenciou:** o agente o citou: "Organizado seguindo o CLAUDE.md para facilitar extensões:
  Novos relatórios: criar classe que implementa `ReportGenerator` e registrar no `ReportService`". O princípio levou
  a uma estrutura que escolhe o relatório num lugar só, mas não a um esqueleto comum. O próximo relatório entra
  "num lugar só", como uma classe nova, e a sequência dos passos continua copiada nele. O comportamento melhorou em
  relação ao N0, mas isso não tem ligação visível com o `CLAUDE.md`.

**N2: omissão, contenção, em parte** (`run-01`).
- **O desenho:** é o mesmo do N1: uma interface de um método e quatro classes que escrevem tudo.
- **Por que o comportamento falhou:**
  - os zeros saem `0` nos relatórios (`NON_REFUNDABLE,0,0`);
  - um corpo que não é JSON e uma data fora do formato dão 500;
  - o espaço não separável depois do "R$".
- **Como o `CLAUDE.md` e a skill influenciaram:** o agente abriu a skill com uma pergunta ("Preciso organizar um
  sistema de reservas onde: 1. Vários relatórios em CSV serão adicionados…") e saiu dela decidido: "vou usar o
  Strategy Pattern para os relatórios". A skill confirmou o pattern errado. O resultado é o mesmo do N1, agora com o
  nome Strategy.

### Sonnet 5.5

**N0: malfeito, contenção, segue** (`run-02`).
- **O desenho:** os quatro relatórios implementam uma interface `Report` e usam a mesma função `Csv.build`, que
  escreve, em ordem, o título, o cabeçalho, as linhas e o total. Por isso a linha de título está num lugar só.
- **Por que é malfeito:** a `Csv.build` recebe valores já prontos. Cada relatório ainda conduz sozinho a sequência
  inteira: ler o período, escolher e ordenar as reservas, montar as linhas, somar o total, e só então chamar o
  formatador. As instruções dão esse caso como malfeito: o "esqueleto" é uma função utilitária, e cada relatório
  repete a sequência dos passos.
- **O comportamento:** tudo confere.

**N1: acerto, contenção, segue** (`run-03`).
- **O desenho:** uma classe abstrata `CsvReport` com o esqueleto. Entre ela e os três relatórios mensais há uma
  intermediária, `MonthReport`, com a leitura do mês e a seleção. A rota é registrada por um laço sobre a lista de
  relatórios, e a taxa vem da configuração.
- **As ressalvas:** o método-modelo não é `final`, e o passo variável é largo: cada subclasse monta numa só chamada o
  cabeçalho, as linhas e o total.
- **O comportamento:** tudo confere.
- **Como o `CLAUDE.md` influenciou:** o desenho passou de uma função utilitária para um esqueleto de verdade. Junto
  com o N2, é um dos dois Template Method do estudo. O agente não citou o `CLAUDE.md`.

**N2: acerto, exagero, segue** (`run-02`).
- **O desenho:** `CsvReport` com o método-modelo `final generate`, a base intermediária `MonthlyCsvReport` e a taxa
  de turismo num objeto próprio.
- **O exagero:** duas interfaces com uma implementação só, `ReservationRepository` e `MessageSender`, que nada
  substitui. O próprio `App` lê as mensagens pela classe concreta, sem passar por `MessageSender`.
- **O comportamento:** tudo confere.
- **Como o `CLAUDE.md` e a skill influenciaram:** abriu a skill sem argumento. O desenho ficou um pouco mais fiel ao
  livro, com o método-modelo `final`. Ao mesmo tempo apareceu o único exagero do Sonnet 5.5 na pesquisa. Pode ter
  vindo da skill, mas o registro da execução não mostra de onde veio.

---

## O que se repete

**1. O modelo pesa mais que o braço.**
- **O Sonnet 5.5:**
  - não teve nenhuma omissão;
  - teve 11 acertos e 1 alternativa em 15;
  - todos os 15 comportamentos `segue`;
  - de 2 a 5 minutos por execução.
- **O Haiku 4.5 e o Sonnet 4.5:**
  - acertam onde o pattern é mais "natural" (01 e 05) e erram onde a estrutura é menos óbvia (07 e 09);
  - quase sempre têm o comportamento `em parte`;
  - levam de 6 a 14 minutos.

**2. O cenário pesa.**

| cenário | acertos (contando a alternativa) |
|---|---|
| 05 (Factory Method) | 8 de 9 |
| 01 (Strategy) | 7 de 9 |
| 03 (Observer) | 6 de 9 |
| 09 (Template Method) | 2 de 9 |
| 07 (Decorator) | 1 de 9 |

Nos dois últimos, só o Sonnet 5.5 chegou lá, e só com harness.

**3. O `CLAUDE.md` (N1) ajudou onde a resposta é "juntar a escolha num lugar".**
- **A favor:** o N1 subiu a classe em quatro casos:
  - Haiku 05, de omissão para acerto;
  - Sonnet 4.5 01, de malfeito para acerto;
  - Haiku 03, de omissão para malfeito;
  - Sonnet 5.5 09, de malfeito para acerto.
- **Contra:** nenhum caso caiu do N0 para o N1.
- **O limite:** onde o pattern pede mais que juntar a escolha, o princípio foi lido de outro jeito:
  - **no Decorator,** "num lugar só" virou "num método só" (Sonnet 4.5 07, que citou o `CLAUDE.md` para justificar o
    `if/else`);
  - **no Template Method,** virou "uma classe nova por relatório", com os passos copiados (Sonnet 4.5 09).

  Nos dois casos, o próximo caso entra mesmo num lugar só. O princípio não distingue "a escolha num lugar" de "os
  passos comuns num lugar".

**4. A skill (N2) dividiu os modelos.**
- **O Haiku 4.5:** abriu a skill uma vez em cinco, apesar da linha no `CLAUDE.md`. Nas outras quatro, o N2 foi, na
  prática, um N1. Foi pior em 03 (de malfeito para omissão) e igual nos demais.
- **O Sonnet 4.5:** abriu as cinco vezes. Em três, passou como argumento uma descrição do problema e recebeu uma
  "consultoria". Em duas delas (01 e 07), tratou essa resposta como a tarefa e entregou a pasta vazia. Na terceira
  (09), saiu com o pattern errado. Quando abriu sem argumento (03 e 05), construiu normalmente, e no 03 leu o
  `observer.md`. Para esse modelo, o N2 foi pior que o N1.
- **O Sonnet 5.5:** abriu as cinco vezes, sem argumento, e continuou trabalhando. O N2 trouxe a única estrutura
  aceita do Decorator e o método-modelo `final` no 09, e trouxe também o seu único exagero.

**5. As falhas de comportamento são do modelo, e não do harness.** As mesmas falhas aparecem nos três braços:
- **o dinheiro mal formatado:** sem vírgula, sem o ponto do milhar, no formato do inglês ou com o espaço não
  separável;
- **o financeiro:** lista reservas reembolsadas com 0.00;
- **o 500 em vez de 400:** para um corpo que não é JSON;
- **a segunda via:** sai para reserva cancelada;
- **a alta temporada:** conta dezembro inteiro;
- **os campos `null`:** somem do JSON;
- **a confirmação que exige `amountPaid` no corpo:** foi inventada pelo Haiku 4.5 em 7 das 15 execuções dele, nos
  três braços. Isso quebra a confirmação do jeito que o site chama.

  O harness fala de estrutura, então era esperado que não mexesse aqui.

**6. Os limites do enunciado aparecem nos resultados.** Ficaram como limitações declaradas, por decisão do Lucas, e
contam contra os modelos de forma igual:
- **o "ponto no meio" do e-mail:** `ana@.com`, `h@.gmail.com` e `ana@gmail.` foram aceitos ou recusados conforme o
  modelo leu essa regra;
- **o espaço depois do "R$":** o `NumberFormat` do Java põe um espaço não separável, e o enunciado mostra o espaço
  comum;
- **as linhas de total dos CSV:** saíram com ou sem vírgulas de preenchimento, e a correção não contou isso como
  erro.

## O erro da skill aberta com uma pergunta

Este é o achado mais importante sobre o N2. Ele afeta a construção e a manutenção.

**Como a skill é aberta no experimento.** O agente roda sozinho, com `claude -p`, e ninguém digita `/gof-patterns`.
A skill só abre se o próprio modelo chamar a ferramenta `Skill`. Até lá, ele vê só o nome e a descrição dela. Ao
chamar, o corpo do `SKILL.md` entra na conversa. A ferramenta aceita um texto opcional, os `args`.

**O que dá errado.** Quando o modelo passa uma pergunta nos `args`, o Claude Code entrega o texto da skill com
`ARGUMENTS: <a pergunta>` no fim, numa mensagem com o papel de **usuário**. O modelo lê aquilo como se o dono tivesse
acabado de perguntar "Qual padrão usar?". Ele responde como consultor, com a explicação do pattern e um código de
exemplo, e encerra o turno. No modo `-p`, encerrar o turno encerra a execução: não há ninguém para dizer "agora
construa".

**Onde aconteceu.**

| pesquisa | modelo e braço | cenário | o que ele passou para a skill | o resultado |
|---|---|---|---|---|
| construção (v7) | Sonnet 4.5 N2 | 01 | "(…) Qual padrão usar?" | parou; pasta vazia |
| construção (v7) | Sonnet 4.5 N2 | 07 | "(…) Como organizar isso?" | parou; pasta vazia |
| construção (v7) | Sonnet 4.5 N2 | 09 | a lista dos relatórios e das variações | seguiu, mas com o pattern errado (Strategy) |
| manutenção (v1) | Sonnet 4.5 N2 | 02 | "Como determinar se uma data está em alta temporada? (…)" | parou; nenhuma linha mudada |
| manutenção (v1) | Sonnet 4.5 N2 | 09 | "Preciso adicionar dois novos relatórios (…)" | parou; nenhuma linha mudada |

- **O Sonnet 4.5 parou em 4 das 5 vezes** em que passou uma pergunta para a skill.
- **Quem abriu sem pergunta seguiu trabalhando:** o Sonnet 5.5, todas as vezes, e o Sonnet 4.5 no 03 e no 05.
- **Outros modelos também passaram uma pergunta e não pararam:** o Haiku 4.5 (manutenção 01 e 03) e o Opus 5
  (manutenção 01).

**De quem é o erro.**
- **Não é da infraestrutura:** o enunciado chegou inteiro, a pasta estava certa e a skill carregou.
- **Não é só do modelo:** três coisas do nosso N2 armam a situação:
  - o verbo "consulte", que convida a fazer uma pergunta;
  - a skill, que é um texto de referência e não diz como deve ser aberta;
  - a execução de uma vez só, sem segunda chance.

  O Sonnet 4.5 é o modelo que cai nela. É um efeito do harness com esse modelo.

**Como isso pesa nos números.**
- **Na construção,** as duas pastas vazias (01 e 07) entram como `omissão`, mas o certo é lê-las como "nada
  entregue".
- **Na manutenção,** as duas execuções sem mudança foram corrigidas como se o agente tivesse decidido não mexer:
  - o 02 recebeu `contenção`, como se fosse um acerto do controle negativo; os controles já saíram da análise;
  - o 09 recebeu `omissão`.

  Os dois são "nada entregue".

**O teste.** O braço N2u troca a linha por "use a skill `gof-patterns` e aplique no código o que ela indicar para
este projeto". Ele roda o Sonnet 4.5 nos três cenários da construção em que o N2 dele deu errado (01, 07 e 09), à
parte das 45 execuções, com o mesmo avaliador às cegas.

| cenário | N0 | N1 | N2 ("consulte") | N2u ("use… e aplique") |
|---|---|---|---|---|
| 01 Strategy | malfeito | acerto | pasta vazia | malfeito, contenção, em parte |
| 07 Decorator | omissão | omissão | pasta vazia | malfeito, contenção, em parte |
| 09 Template Method | omissão | omissão | omissão, contenção | omissão, **exagero**, em parte |

- **A parada sumiu.** As três execuções entregaram o projeto inteiro, com o build ok, em 9 a 12 minutos. No 01, o
  agente abriu a skill sem pergunta e leu o `strategy.md`. No 07 e no 09, ainda passou texto nos `args`, mas desta
  vez não tratou a resposta como o fim do trabalho: leu os arquivos dos patterns e seguiu construindo.
- **01:** o Strategy existe, mas a interface não leva o código da empresa, e o serviço pergunta "é `CORPORATE`?" e
  faz `instanceof` para chamar um método que só o convênio tem. É o mesmo furo do N0 do Sonnet 4.5.
- **07:** escolheu uma corrente de regras, que as instruções aceitam, mas a escolha do modo ficou dentro das regras
  (a cópia e o desvio perguntam pelo `testMode`), e a montagem instala sempre as três. Ficou melhor que o N0 e o N1
  (omissão), no mesmo ponto em que o Sonnet 5.5 também tropeçou no N0 e no N1.
- **09:** escolheu de novo o Strategy para os relatórios, como no N2. Cada relatório escreve tudo sozinho, o título
  inclusive, e por isso a classe continua `omissão`. E, seguindo o "aplique o que ela indicar", espalhou patterns que
  o enunciado não pede: um State de cinco classes para a situação da reserva e uma Strategy de plano de tarifa
  paralela ao enum. É o primeiro exagero do Sonnet 4.5 na construção.

**O que o teste mostra.** O verbo resolve a parada: com "use", o Sonnet 4.5 entregou nas três vezes. Não resolve a
escolha do pattern: no 09 a skill continuou levando ao Strategy. E "aplique o que ela indicar" tem um custo: no 09,
virou licença para pôr patterns onde ninguém pediu, contra o segundo princípio do `CLAUDE.md`. Com uma execução por
cenário, isso é um indício, e não uma medida.

## Observações sobre a correção

- **O registro dos ouvintes no 03.** O mesmo problema, a lista criada no construtor de quem distribui, recebeu
  `acerto` no Sonnet 4.5 N0 e `malfeito` no Haiku N1. O Haiku tinha dois agravantes: a interface não é pública, e os
  ouvintes de cancelamento conferem o estado por conta própria. As duas correções registraram a dúvida. É o caso de
  fronteira mais sensível do estudo.
- **As fronteiras do 07 e do 09.** No 07 (Sonnet 5.5 N0 e N1) e no 09 (Sonnet 5.5 N0), a IA ficou entre `malfeito` e
  `alternativa equivalente`. Escolheu `malfeito` pela regra escrita nas instruções: no 07, a combinação por modo
  decidida na montagem; no 09, as partes que variam entregues como funções, e não como valores prontos.
- **As pastas vazias.** O 01 recebeu `não segue` e o 07 recebeu `não se aplica`, para a mesma situação. As duas
  valem como "nada entregue" (veja "O erro da skill aberta com uma pergunta").
- **O título das correções.** Quase todas começam com "# Cenário 01", porque o exemplo de formato do `common.md`
  traz esse número fixo. É só o título: a pasta de cada correção diz o cenário, e o conteúdo está certo. Uma correção
  veio dentro de um bloco de código. Nada disso muda as conclusões.

## Custo e tempo

- **Os agentes:** US$ 37,33 em equivalente de API, nas 45 execuções. Por execução:
  - Sonnet 5.5: cerca de US$ 0,47 e 3,6 minutos em média;
  - Haiku 4.5: US$ 0,73 e 8,3 minutos;
  - Sonnet 4.5: US$ 1,29 e 8,7 minutos (contando as duas execuções vazias).
- **A correção:** cerca de US$ 28, em equivalente de API, pelas 45.
