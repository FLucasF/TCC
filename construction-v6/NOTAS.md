# Notas do rascunho do prompt v2

> Só para o Lucas. O prompt (`experiment/prompt/prompt.md`) é o que o agente leria; estas notas
> explicam o que cada trecho faz no experimento. Nada daqui vai para o prompt.
>
> **Os nomes antigos (até 14/10, quando tudo vivia no Desktop, em `pesquisa-nova/`).** O histórico
> abaixo usa os nomes daquela época: `prompt-v2-rascunho.md` é hoje `experiment/prompt/prompt.md`;
> as versões `prompt-v2-rascunho.antes-*.md` estão em `experiment/prompt/history/prompt.antes-*.md`;
> `piloto/<id>/` é `runs/pilots/<id>/`; `reference/` é `evaluation/reference/`; e o `piloto.sh` virou
> `infra/scripts/pilot.sh` e depois `infra/scripts/run_pilot_without_harness.py` (as ferramentas do V6 passaram para Python).

## Os padrões medidos

Um representante de cada categoria do GoF, sem forçar nenhum:

| papel | padrão | categoria | a pergunta da régua |
|---|---|---|---|
| principal | **Strategy** | comportamental | a do V5: localização (o código de cada caso mora na casa dele?) e seleção (o caminho até a conta nomeia os casos?) |
| principal | **Adapter** | estrutural | o cálculo do frete faz HTTP direto, ou o HTTP está isolado numa peça própria (com ou sem interface)? |
| principal | **Factory** | criacional | a criação dos objetos de cada caso está concentrada (numa fábrica, num registro ou no contêiner do Spring) ou espalhada? |
| secundária | **State** | comportamental | a forma do P4, sobre as situações do pedido |

Observer (os efeitos do pagamento) e Value Object (o dinheiro) continuam no texto, porque são
requisitos reais, mas não viram régua. Sobre a Factory: o que se mede é o **princípio criacional**
(a criação concentrada), não a *Factory Method* estrita do GoF. No TCC, escrever assim.

## O mapa de cenários: óbvios e armadilhas

Quatro tipos de cenário:
- **óbvio**: o requisito claramente pede o padrão (quem aplica?);
- **remendo**: uma regra cruzada que tenta a escrever um `if` no lugar errado;
- **exagero**: parece pedir o padrão, mas não pede (o controle negativo);
- **borda**: um detalhe de correção que pega quem lê com pressa (vira caso da suíte).

### Strategy

| tipo | no prompt | o certo | o que pega |
|---|---|---|---|
| óbvio | entregas ("quase toda semana entra uma opção nova"), cupons ("o marketing adora inventar"), clube ("estudando criar mais níveis"), formas de pagamento | cada caso na sua casa | — |
| remendo | OURO não paga frete, **exceto no motoboy** | a regra mora no nível (ou numa política de frete) | `if (nivel == OURO && entrega != MOTOBOY)` no cálculo do frete |
| remendo | FRETEGRATIS **não vale na retirada** | a regra mora no cupom | `if (cupom == FRETEGRATIS && entrega == RETIRADA)` no serviço |
| remendo | desconto do Pix **só sem cupom** | a regra mora no Pix (que recebe o contexto) | `if (pagamento == PIX && cupom == null)` no serviço |
| remendo | BEMVINDO10 **só na primeira compra** | a regra mora no cupom, que consulta os pedidos | a checagem de histórico no controller |
| exagero | seguro por região, agora com **limite** no Norte e no Nordeste | uma tabela (porcentagem + limite): dados | uma classe por região |
| exagero | **dias a mais** no Norte e no Nordeste (só econômica e expressa) | uma tabela de dias por região | uma estratégia de prazo por região |

### Adapter

| tipo | no prompt | o certo | o que pega |
|---|---|---|---|
| óbvio | a TransLog: site externo, pode cair, pode recusar CEP, "um dia a gente troca" | o HTTP isolado numa peça própria | `RestTemplate`/`WebClient` dentro do cálculo |
| exagero | o motoboy e a retirada: valores fixos, internos | um valor na regra da entrega | uma interface + adaptador para um número fixo |
| borda | a TransLog fora do ar **só importa para quem depende dela** | motoboy e retirada funcionam com a TransLog fora | quem consulta a TransLog para todas as opções devolve 503 no motoboy |
| borda | a TransLog manda o valor com **mais de 2 casas** | arredondar meio-para-o-par | usar o valor cru |
| borda | o tempo limite de **2 segundos** | `TRANSPORTADORA_INDISPONIVEL` | ficar esperando para sempre |
| borda | `404` (não atende o CEP) × outros erros | `MODALIDADE_INDISPONIVEL` × `TRANSPORTADORA_INDISPONIVEL` | tratar tudo igual |

### Factory (criação concentrada)

| tipo | no prompt | o certo | o que pega |
|---|---|---|---|
| óbvio | os objetos dependem de outros: a econômica e a expressa precisam da TransLog; o BEMVINDO10 precisa dos pedidos do cliente | montados num lugar só | `new EntregaExpressa(new TransLogClient(url))` repetido |
| óbvio | "o resumo e o pedido registrado precisam dar os mesmos valores" (o mesmo cálculo em dois fluxos) | o cálculo e as peças reaproveitados | o cálculo copiado em dois serviços, cada um criando as suas peças |
| exagero | o item do carrinho (`sku` + quantidade), o aviso | criados direto | uma fábrica ou um builder para um objeto sem regra |

### State (secundária)

| tipo | no prompt | o certo | o que pega |
|---|---|---|---|
| óbvio | as 5 situações do pedido, as transições permitidas, "estamos pensando em criar 'em separação'" | cada situação sabe o que pode fazer | `if (situacao == PAGO)` espalhado pelos serviços |
| remendo | cancelar um pedido **pago** devolve estoque e crédito; cancelar **não pago** não devolve nada | o comportamento muda conforme a situação, na situação | um `if` de situação dentro do cancelamento |
| exagero | o aviso: só **NOVO → LIDO** | um campo e uma troca simples | uma classe por situação do aviso |
| borda | **48 horas exatas** ainda aceitam | `>` 48 h cancela | `>=` |
| borda | o mesmo aviso de pagamento **repetido** | `200`, sem efeito novo | baixar o estoque duas vezes |
| borda | o pagamento de um pedido **sem estoque** | `409`, pedido cancelado, aviso | pedido pago com estoque negativo |

### Correção geral (vira caso da suíte)

| borda | o que pega |
|---|---|
| motoboy com **5 kg exatos** (aceita) | `>=` em vez de `>` |
| MENOS50 com **300,00 exatos** (aceita) | `>` em vez de `>=` |
| boleto com **1.000,00 exatos** (aceita) | `>=` em vez de `>` |
| brinde com produtos em **500,00 exatos** (não ganha: "passarem de") | `>=` em vez de `>` |
| seguro que dá **exatamente** o limite | arredondar depois do limite |
| LEVE3PAGUE2 com **7 unidades** de um produto (2 grátis) | contar por pedido, e não por produto |
| a **ordem** das 15 recusas (dois problemas no mesmo pedido) | devolver o problema errado |
| a **última peça** paga por dois ao mesmo tempo | vender o que não tem |
| BEMVINDO10 depois de um pedido **cancelado** (ainda vale) | contar o cancelado como compra |

## O que mudou em relação ao V5

| | V5 | v2 (rascunho) |
|---|---|---|
| tamanho | ~160 linhas | ~245 linhas |
| endpoints | 1 | 11 |
| itens do carrinho | nome, preço e peso vêm no pedido | só `sku` e quantidade; o resto vem do catálogo |
| banco | "não use banco de dados" | PostgreSQL 18, dados não podem se perder |
| dependência externa | nenhuma | a TransLog |
| tempo | nenhum | 48 horas para pagar |
| concorrência | nenhuma | a última peça, e o aviso repetido |
| recusas | 10 | 15 no cálculo + 4 nas operações + 3 de "não encontrado" |
| controle negativo | o seguro, com "a conta é a mesma em todas" | o seguro com limite + os dias a mais por região, **sem** a frase que entrega |

## O que cada dimensão nova mede aqui

| dimensão | no prompt | como medir |
|---|---|---|
| **design** | Strategy, Adapter, Factory (+ State) | Semgrep, réguas no estilo do V5 |
| **testes** | "o `mvn verify` roda sem banco e sem internet" | PIT: quem abstraiu o banco, a TransLog e o relógio consegue testar |
| **arquitetura** | 11 endpoints, banco, serviço externo | ArchUnit, com regras indiscutíveis (sem ciclos entre camadas; o cálculo não depende da web) |
| **banco** | estoque nunca negativo, não perder dados, a última peça, o aviso repetido | a suíte (concorrência e idempotência) + `information_schema` (restrições) |
| **correção** | as recusas, as contas, o ciclo do pedido | a suíte, com a calculadora de referência |

## Decisões em aberto (para você)

1. **Pedir testes ou não?** O rascunho **não pede**, como o V5. Se não pedir, o PIT mede também
   "quem escreve teste por conta própria". Se pedir, mede só a qualidade.
2. **O "sem banco e sem internet" no `mvn verify`** é uma restrição real da infraestrutura. Ela
   também empurra o agente a pensar em teste sem banco. Manter?
3. **O relógio das 48 horas** não é injetado pelo prompt: o `pagoEm` vem no aviso do banco, e o
   `criadoEm` é o relógio do serviço. A suíte testa mandando um `pagoEm` 49 horas no futuro.
4. **O tamanho** (~245 linhas, 11 endpoints). O piloto vai dizer se cabe no tempo e na cota.
5. **Os dias a mais por região** são uma segunda armadilha de exagero ao lado do seguro. Se o
   piloto mostrar que o prompt ficou grande demais, é a primeira coisa a tirar.

## O que ainda falta para o prompt ficar pronto

- os **exemplos conferidos**, que saem da calculadora de referência (ainda não existe a do v2);
- o **servidor falso da TransLog**, com as respostas fixas (frete com 3 casas, CEP não atendido,
  fora do ar, lento);
- o **piloto** com 2 ou 3 modelos no N0, para ver onde cada cenário fica (teto, chão ou meio).

---

## Pilotos (14/10/2026)

Todos no N0 (sem harness), effort `medium`, com a imagem `experimento-harness:v5` e o mesmo
isolamento do V5, gravados em `piloto/<id>/` (fora do construction-v5). Sem exemplos conferidos no enunciado
(ainda não existem). Leitura por busca no código, não pela régua nem pela suíte: a **correção das
contas ainda não foi testada**.

### Piloto 1: Sonnet 5.5 (`SONNET55-N0-01`)

- 530 s, 19 turnos, US$ 1,23, build ✅ (8 testes).
- **Problema do prompt:** a frase "o `mvn verify` não tem banco de dados nem internet" era falsa
  para o Maven e induziu o agente a **escrever à mão o protocolo do PostgreSQL** (612 linhas,
  com autenticação SCRAM). Corrigida para "não tem banco de dados; as dependências do Maven são
  baixadas normalmente". A imagem v6 com o driver pré-carregado ficou para depois (decisão do
  Lucas: testar antes com a v5).
- Caiu em duas armadilhas: `"RETIRADA_LOJA".equals(...)` dentro do FRETEGRATIS (nome do caso, e não
  propriedade) e `if (situacao == PAGO)` no cancelamento (o remendo de State previsto).
- Concorrência resolvida com uma trava **em memória** (todo o estado carregado na memória): certo
  para um servidor só, invisível para a suíte.

### Piloto 2: fraco, médio e forte em paralelo

| | Haiku 4.5 | Sonnet 5.5 | Opus 5.5 |
|---|---|---|---|
| tempo / turnos | 831 s / 106 | 521 s / 20 | 917 s / 31 |
| custo | US$ 1,20 | US$ 1,01 | US$ 2,52 |
| código | 1.766 linhas, 36 classes | 1.728 linhas, 48 classes | 1.865 linhas, 63 classes |
| testes | 13 | 6 | 17 |
| banco | JPA + driver | JDBC + driver | JDBC + driver |

A correção do prompt funcionou: os três baixaram o driver pelo Maven. Custo total: US$ 4,73.

| armadilha | Haiku 4.5 | Sonnet 5.5 | Opus 5.5 |
|---|---|---|---|
| Strategy: cada caso na sua casa | ❌ `switch` no `CalculoService` | ✅ | ✅ |
| o mesmo cálculo em dois fluxos | ❌ os `switch` duplicados | ✅ | ✅ |
| FRETEGRATIS × retirada | ❌ no `switch` | ✅ propriedade | ✅ propriedade |
| OURO × motoboy | ❌ no `switch` | ✅ propriedade | ✅ propriedade |
| Pix × cupom | ❌ no serviço | ✅ no Pix | ✅ no Pix |
| TransLog isolada (Adapter) | ✅ | ✅ | ✅ |
| State: as transições | ❌ `if` no serviço | ✅ | ✅ |
| a última peça | ❌ lê-confere-grava sem trava | ✅ `UPDATE … WHERE estoque >= ?` + `FOR UPDATE` | ✅ `FOR UPDATE` |
| `CHECK (estoque >= 0)` | ❌ | ✅ | ✅ |
| chaves estrangeiras | parcial (JPA) | ❌ nenhuma | ✅ 5 |
| aviso repetido | ✅ | ✅ | ✅ |
| 48 horas exatas | ❌ `HOURS.between` trunca (48 h 59 min passa) | ✅ | ✅ |

### O que os pilotos mostraram

1. **O prompt separa bem o fraco do forte.** O Haiku caiu em quase todas as armadilhas de desenho,
   concorrência e borda: há espaço para o harness mostrar efeito, o que faltou no V5.
2. **Entre os fortes, o desenho bate no teto.** Sonnet e Opus acertaram todo o Strategy e o State.
   O que os separou foi o **banco** (o Sonnet sem chave estrangeira) e os **testes** (6 × 17): as
   dimensões novas separam o que o design não separa.
3. **O Adapter, do jeito que está, deu teto nos três.** Até o Haiku isolou a TransLog, com timeout
   e o 404 tratado. Precisa de uma armadilha mais fina (ver a proposta abaixo) ou sai como medida.
4. **A variação entre execuções é real.** O Sonnet do piloto 1 caiu em duas armadilhas que o do
   piloto 2 não caiu: mais uma razão para as 5 réplicas e a comparação em pares.
5. **Para a régua:** nas regras cruzadas, distinguir perguntar uma **propriedade**
   (`modalidade.temFrete()`, aceitável) de nomear o **caso** (`== RETIRADA_LOJA`, espalhado). Os
   fortes usaram propriedade; o Sonnet do piloto 1, o nome.

### Proposta para o Adapter (depois do piloto 2)

O teto veio de a TransLog falar a **mesma língua** da loja (`valor` em reais, `prazoDias` em dias):
não havia nada para traduzir, então só se media "o HTTP está isolado?", e todos isolam. O valor real
de um Adapter é **traduzir** um vocabulário estrangeiro. Proposta: a TransLog passa a falar a língua
dela, como uma API de terceiro de verdade:

    GET {TRANSLOG_URL}/v2/quote?zip=01310100&weight_g=1800&service=STD
    → 200 { "price_cents": 3310, "eta_hours": 41 }
    CEP não atendido → 200 { "error": "ZIP_NOT_SERVED" }

| a TransLog fala | a loja precisa | a tradução |
|---|---|---|
| `STD` / `EXP` | `ECONOMICA` / `EXPRESSA` | mapeamento de códigos |
| `weight_g` | kg | × 1000 |
| `price_cents` | reais | ÷ 100 |
| `eta_hours` | dias | ÷ 24, arredondado para cima |
| `200` + `error` | `MODALIDADE_INDISPONIVEL` | ler o erro dentro do 200 |

- **Régua:** "o vocabulário da TransLog aparece fora da peça que fala com ela?" (isolado ×
  espalhado), a mesma forma do Strategy. Semgrep: `STD`, `EXP`, `price_cents`, `eta_hours`,
  `weight_g` fora do arquivo da TransLog.
- **Bordas para a suíte:** 41 h = 2 dias (quem trunca dá 1); centavos → reais; o erro dentro do 200.
- **Custo:** só a seção da TransLog no anexo e o servidor falso.

### Piloto 3: as duas armadilhas finas (limite de 30% e a TransLog estrangeira)

Mudanças no prompt antes deste piloto: o limite de 30% para todos os cupons (inclusive o FRETEGRATIS)
e a TransLog falando a língua dela (`/v2/quote`, `weight_g`, `STD`/`EXP`, `price_cents`, `eta_hours`,
o `ZIP_NOT_SERVED` dentro do `200`). Versão anterior: `prompt-v2-rascunho.antes-piloto3.md`.

| | Haiku 4.5 | Sonnet 5.5 | Opus 5.5 |
|---|---|---|---|
| tempo / turnos | 720 s / 83 | 546 s / 18 | 1.330 s / 38 |
| custo | US$ 0,92 | US$ 1,06 | US$ 3,74 |
| código | 2.302 linhas, 35 classes | 1.844 linhas, 58 classes | 2.331 linhas, 74 classes |
| build | ❌ Testcontainers sem Docker | ✅ 6 testes | ✅ 36 testes |

| armadilha | Haiku 4.5 | Sonnet 5.5 | Opus 5.5 |
|---|---|---|---|
| **limite de 30% num lugar só** | ✅ (num serviço; não tem classes de cupom) | ✅ na calculadora | ✅ em `Cupons` |
| **vocabulário da TransLog isolado** | ❌ `"STD"/"EXP"` em 3 lugares de 2 serviços | ❌ `"STD"`/`"EXP"` em `Economica`/`Expressa`; centavos, horas e gramas convertidos no domínio | ✅ tudo dentro de `TransLog.java` |
| 41 h → 2 dias | ✅ `Math.ceil` | ✅ `(h + 23) / 24` | ✅ `Math.ceilDiv` |
| erro dentro do 200 | ✅ | ✅ | ✅ |
| Strategy (nomes fora da casa) | ❌ 22 linhas | ✅ | ✅ |
| State (situação comparada no serviço) | ❌ situação como **texto** (`"AGUARDANDO_PAGAMENTO".equals`) | ❌ 5 `if` no serviço | ✅ |
| trava de estoque / `CHECK` | ❌ / ❌ | ✅ / ✅ | ✅ / ✅ |
| chaves estrangeiras | 7 (JPA) | 3 | 5 |

**Conclusões:**
1. **A TransLog estrangeira funcionou:** agora o Adapter separa os três (Haiku vaza e duplica;
   Sonnet vaza para o domínio; Opus isola). Saiu do teto.
2. **O limite de 30% não separou ninguém** pela régua (todos num lugar só). Fica como borda de
   correção para a suíte (o FRETEGRATIS limitado), não como régua.
3. **Há um degrau claro Opus > Sonnet > Haiku** no N0. O Opus continua limpo em todo o desenho: o
   que pode separá-lo é a suíte (correção) e o PIT.
4. **O State do Sonnet variou:** caiu no piloto 1, acertou no 2, caiu no 3 (2 de 3).
5. **Correção do prompt:** o build do Haiku falhou por usar Testcontainers (Docker) num build sem
   Docker. O texto dizia só "sem banco"; agora diz "sem banco de dados nem Docker".

## A calculadora de referência e os exemplos (14/10/2026)

A calculadora está em `referencia/` (Java 21, sem Spring). Validação: 20.000 compras aleatórias
iguais às da calculadora do V5 na parte comum, 53 testes de borda das regras novas, e erros
plantados (o diferencial não pega fronteira exata; os testes de borda pegam). Ver `referencia/README.md`.

**As 4 decisões que a calculadora tomou, aplicadas no prompt** (versão anterior:
`prompt-v2-rascunho.antes-exemplos.md`):

| ponto | decisão | onde está no prompt |
|---|---|---|
| cupom vindo como texto vazio | `CUPOM_INVALIDO` | o campo `cupom` no anexo + a recusa 11 |
| LEVE3PAGUE2 sem nenhum trio | vale, com desconto zero | a descrição do cupom |
| Pix com um cupom que deu desconto zero | sem o desconto do Pix | a pergunta do Pix no atendimento |
| CPF e e-mail com formato errado | só a ausência é recusada | o campo `cliente` no anexo |

**Os exemplos conferidos** (10: 7 resumos e 3 recusas) foram gerados pela calculadora e substituíram
o `[A PREENCHER]`. O `Exemplos --conferir` é o check-prompt do v2 (56 valores), e o `piloto.sh` o roda
antes de cada execução. Os pilotos 1 a 3 rodaram **sem** exemplos; o próximo será o primeiro com eles.

---

## O benchmark do Akita, e o que ele ensina para o foco no Opus (14/10/2026)

Lido em `J:\projects\akita\llm-coding-benchmark` (prompts em `prompts/`, gerações em `benchmark-v3`,
`benchmark-v4`, `benchmark-v4.1`, relatórios em `docs/success_report*.md`). Cada geração mudou o prompt e o
jeito de testar porque a anterior **saturou**.

| geração | o prompt | como testa | o Opus |
|---|---|---|---|
| v1 | um parágrafo técnico (app Rails de chat com RubyLLM, Tailwind, Hotwire, testes, Docker) | auditoria por rubrica | — |
| v2 | **14 objetivos numerados (G1–G14)**, ditos como "o contrato de pontuação"; 3 fases (construir, provar que roda, autorrevisão) | rubrica 0–100 em 10 dimensões, catálogo fixo de descontos, auditor que verifica tudo | Opus 5: 95; Opus 4.7: 91; Opus 4.6: 83 (âncora) |
| v3 | tarefas curtas isoladas (bug, borda, desempenho) | **suíte oculta**: o modelo nunca vê os testes da nota | Opus 5: 99,7; Opus 4.8: 99,0; Opus 4.6: 97,6 |
| v4 | um projeto Rails que evolui em sprints, com um "colega malicioso" plantando falhas | se o modelo percebe sozinho as falhas | — |
| v4.1 | problemas longos e difíceis (KV replicado com Raft, em Go) | avaliador automático com injeção de falhas | — |

**O estilo do prompt (v2):** voz de engenheiro para engenheiro, em inglês; instruções contra parar ("do not stop
to ask… recover and continue"); os objetivos ditos com todas as letras, como a nota; uma fase de autorrevisão em
que a honestidade vale nota ("an honest FAIL is worth more than an optimistic PASS"). O **v4** muda para o
princípio do nosso prompt: *"never name the sabotage"*, só uma "abertura natural".

**Como ele testa:**
- **v2 (auditor + rubrica):** roda os testes do próprio modelo e confere os números declarados; confere o uso da
  biblioteca contra o código-fonte dela; confere o que o modelo diz ter validado contra o log real dos comandos;
  descontos de um catálogo fixo, iguais para todos (ex.: −2 por `eval` na calculadora; −2 por imprimir um segredo:
  o Opus 4.7 vazou a chave da API no log e caiu de 93 para 91).
- **v3 (suíte oculta):** cada tarefa tem uma **solução de referência** (tem de tirar 100%) e uma **solução
  ingênua** (tem de tirar bem menos); a diferença é o "poder de discriminação" da tarefa, e tarefa que não separa
  é descartada. É a nossa calculadora + mutantes, com um nome melhor.

**O achado que mais importa:** o relatório da v3 se chama de *"o caminho errado, guardado para a história"*. Ele
tentou de tudo para separar os modelos de ponta na qualidade (nota contínua, defeitos escondidos: 0 ponto de
diferença; projeto de 6 módulos: 3,4; app de 5 funcionalidades: 4,8) e concluiu que **não dá para ter um teste
realista do dia a dia e, ao mesmo tempo, uma grande diferença entre os modelos de ponta**: 24 de 37 modelos
tiraram 95–100. O que separa os de ponta é o **custo e o tempo** (até 8× o custo e 5× o tempo para a mesma
qualidade).

**O que isso significa para o foco no Opus:**
1. O piloto bate com o achado: o Opus ficou limpo em todo o desenho, nas duas execuções. **Não forçar armadilhas
   cada vez mais artificiais** para ele errar: o Akita mostra que isso leva a um teste irreal e a diferenças
   pequenas mesmo assim.
2. **Medir no Opus o que ainda varia:** a correção (a suíte com a calculadora, ainda não rodada), a qualidade dos
   testes (PIT) e, principalmente, o **custo e o tempo** (no piloto: US$ 2,52 → 3,74; 15 → 22 minutos).
3. **O efeito do harness no Opus pode aparecer por essas medidas**, não pelo desenho: o harness o deixa mais barato
   ou mais rápido para o mesmo resultado? Faz ele escrever testes que pegam mais mutantes?
4. **Trazer o "poder de discriminação" da v3:** para cada armadilha, uma solução certa tem de passar e uma ingênua
   tem de falhar — já fazemos com os mutantes da suíte; dá para fazer também com as réguas do Semgrep.

---

## A primeira medida de correção: a mini-suíte do resumo (14/10/2026)

`referencia/Casos.java` gera os casos com a resposta esperada **calculada pela calculadora**;
`referencia/mini-suite.mjs` sobe uma TransLog falsa, chama o `POST /checkout/resumo` do serviço do agente e
compara campo por campo. O serviço roda com o `jar` do piloto, num PostgreSQL limpo (`postgres:16-alpine`, o que
havia na máquina; o prompt pede o 18). **435 casos:** os 10 exemplos (o 9 com um pedido pago antes), 25
fronteiras exatas que o catálogo real permite montar e 400 compras aleatórias, válidas e inválidas, com respostas
sorteadas da TransLog. Ficam de fora as 4 decisões tomadas **depois** do piloto 3, que o agente não recebeu.
Só o piloto 3 tem o prompt com as mesmas regras da calculadora (TransLog estrangeira e limite de 30%).

| serviço (piloto 3) | casos certos |
|---|---|
| **Opus 5.5** (`OPUS55-N0-02`) | **435 de 435**; a TransLog sempre chamada com CEP, gramas e serviço certos, e nunca sem precisar |
| **Sonnet 5.5** (`SONNET55-N0-03`) | **435 de 435** |
| **Haiku 4.5** (`HAIKU45-N0-02`) | **não sobe:** erro de mapeamento do JPA (`pedido_id` mapeado duas vezes); os testes dele, que usavam Testcontainers, nunca rodaram e não pegaram |

**A suíte pega erro sutil?** Três erros plantados numa cópia do serviço do Opus (`referencia/mutantes-servico.sh`):

| erro plantado | casos certos | quem pegou |
|---|---|---|
| arredondamento meio-para-cima | 419 de 435 | **só os aleatórios** (os exemplos e as bordas passaram) |
| horas → dias sem arredondar para cima | 368 de 435 | os três grupos |
| limite do cupom em 35% | 425 de 435 | os três grupos |

**Conclusões:**
1. **Na correção do resumo, Opus e Sonnet empatam no teto** — como o Akita encontrou na v3: no dia a dia bem
   especificado, os modelos de ponta acertam as contas. O que separou os dois foi o desenho (piloto 3) e, entre
   os fortes, o custo e o tempo. O Haiku separa de outro jeito: nem entrega um serviço que sobe.
2. **Os exemplos e as bordas não bastam:** o arredondamento errado só aparece nos aleatórios (meio centavo é
   raro). O inverso também vale (o aleatório não acha a fronteira exata). A suíte de verdade precisa dos dois.
3. **"O serviço sobe" precisa ser medido à parte**, antes da correção: o Haiku passou no `mvn verify` do piloto 2
   e falhou no 3, e no 3 o serviço nem inicializa.
4. Falta a outra metade: os endpoints de **pedido** (pagar, enviar, cancelar, estoque, saldo, avisos), onde estão a
   concorrência e a idempotência, e onde o Sonnet e o Opus tiveram desenhos diferentes.

---

## O que falta (14/10/2026)

1. **O ciclo do pedido no gabarito**: registrar, pagar, enviar, entregar, cancelar, estoque, saldo e avisos
   (a calculadora do "estado"; é onde estão a concorrência e a idempotência).
2. **A suíte completa**: a mini-suíte do resumo + os endpoints de pedido + concorrência (a última peça) +
   idempotência (o aviso repetido) + a checagem de que o serviço sobe.
3. **As réguas do Semgrep do v2**: Strategy (reaproveita a do V5), Adapter (o vocabulário da TransLog) e Factory,
   validadas com corpora e com o "poder de discriminação".
4. **Os harness N1 a N3 do v2 — com o Lucas.** Ele vai redesenhar com cuidado (os atuais, do V5, só falam de
   Strategy e "estão bem ruins"). Requisito do experimento, para lembrar: o harness precisa falar das dimensões
   que se quer medir (testes, banco, Adapter), senão não tem como mostrar efeito nelas.
5. **Decisões abertas:** pedir testes no prompt ou não; as "duas máquinas"; a imagem Docker v6 com o driver;
   quais modelos e quantas réplicas.
6. **Congelar** (hashes) e registrar no `DECISOES.md`.
7. **A rodada**, a leitura humana, a análise e a escrita.

---

## Item 1 concluído: o gabarito do ciclo do pedido (14/10/2026)

**A API foi para o inglês** (decisão do Lucas): rotas (`/checkout/summary`, `/orders/{id}/payment`…), campos do
JSON, códigos de erro, situações do pedido e os nomes dos casos (`GOLD/SILVER/BRONZE`, `NORTH/SOUTH…`,
`ECONOMY/EXPRESS/STORE_PICKUP/COURIER`, `WELCOME10/MINUS50/FREESHIPPING/BUY3PAY2`, `PIX/CREDIT_CARD/BANK_SLIP`).
**O texto do dono da loja continua em português**; o anexo ganhou uma tabela "Os códigos". Versão anterior:
`prompt-v2-rascunho.antes-api-ingles.md`. Consequência: os pilotos 1 a 3 rodaram com a API em português e não
são comparáveis à suíte nova; os próximos pilotos já usam a API em inglês.

**As 6 decisões do ciclo do pedido, aplicadas no prompt:**
1. o pagamento sem `transactionId`/`paidAt`, ou o envio sem `trackingCode`: `422 INVALID_ORDER`, depois do 404 e
   antes do 409;
2. o mesmo aviso de pagamento repetido devolve **a mesma resposta da primeira vez** (o 200 com o pedido como está,
   ou a mesma recusa), sem fazer nada de novo;
3. outro `transactionId` num pedido que já não aguarda pagamento: `409 OPERATION_NOT_ALLOWED`;
4. o pedido só muda de situação quando chega um pagamento ou um cancelamento (continua aguardando depois das 48 h);
5. o crédito que entra no saldo é o `nextPurchaseCredit` do resumo registrado;
6. os avisos de um CPF sem pedido: `404 CUSTOMER_NOT_FOUND`.

**O código foi reorganizado** (pedido do Lucas: nomes em inglês, código legível, menos fragmentação): a pasta
virou `reference/`, com `Calculator` (o resumo) e `Store` (o estado), e as ferramentas `PromptExamples`,
`SuiteCases` e `V5Differential`. Ver `reference/README.md`.

**Validação:** 90 testes (54 da calculadora, 36 do ciclo), o diferencial contra o V5 (20.000 iguais) e erros
plantados (os testes do ciclo pegam os 4, depois de ganhar o teste do pedido com dois itens).

**Achado lateral — um defeito latente no gabarito do V5:** ele soma os pesos em ponto flutuante, e um pedido de
exatamente 5 kg pode virar 5,000000000000001 kg, recusando o motoboy. O caso dos 5 kg da suíte do V5 (2 × 2,50 kg)
dá 5 exatos também em ponto flutuante, então **os resultados do V5 não foram afetados**. Vale citar no TCC como a
razão de o gabarito novo usar decimal exato.

**Próximo: item 2, a suíte completa** — já montada como o módulo "corrigir" consolidado (ver a conversa sobre
juntar os scripts por responsabilidade).

## Item 2 concluído: a suíte completa (14/10/2026)

A suíte (`evaluation/reference/suite/`, rodada pelo `run-suite.sh`, hoje `run_suite_against_service.py`) sobe o serviço do agente com um Postgres
descartável e uma TransLog falsa, e faz as mesmas chamadas no serviço e no `Store`, comparando passo a passo.
Cinco grupos: catálogo, resumo (os exemplos, as fronteiras e 400 sorteados), ciclo do pedido (11 cenários),
persistência (reinicia o serviço) e concorrência (4 corridas).

- **Contra o Opus com a API em inglês (OPUS55-N0-03):** 463 de 463. 1.325 s, 39 turnos, US$ 3,20, build verde
  com 39 testes próprios. A correção continua saturada no topo, como no Akita.
- **A suíte pega erro?** 12 erros plantados, um de cada vez, no serviço do Opus (`plant-mutants.py`, hoje `check_suite_catches_planted_bugs.py`):
  **12 de 12 pegos.** Prazo de 48 h, aviso repetido, estoque "basta um item", cancelamento sem devolver estoque
  ou crédito, entregar sem enviar, aviso de envio faltando, WELCOME10 com pedido cancelado, estoque que volta ao
  inicial ao reiniciar, pedidos perdidos ao reiniciar, leitura do estoque sem `FOR UPDATE` e pagamento sem
  travar o pedido. Os dois de concorrência apareceram como 500 nas corridas. Resultado em
  `runs/pilots/OPUS55-N0-03/suite-mutants.txt`.

## Item 3: a régua do Semgrep do V6 (14/10/2026)

Em `evaluation/semgrep/` ([README](evaluation/semgrep/README.md)). Decisões do Lucas:
- **Factory descritiva**, sem hipótese: nos 8 pilotos, o Spring cria todas as estratégias (`container`).
- **Adapter medido pelo vazamento** do vocabulário da TransLog (`"STD"`/`"EXP"`, `price_cents`,
  `eta_hours`, `ZIP_NOT_SERVED`), e não pelo "HTTP isolado", que todos fazem.
- **Ferramentas em Python** (o V6 inteiro, menos o `compare_reference_with_v5.mjs`), com mypy estrito e ruff.
- **A regra cruzada pode morar na casa do par** (STORE_PICKUP dentro do cupom FreeShipping, COURIER
  dentro do Gold), e **converter a unidade da transportadora fora do adaptador é vazamento**.

Validação: o P1 a P5 dão 2.200 de 2.200 respostas iguais ao detector do V5, nos 220 pacotes do V5.

Nos pilotos, o State separa os três modelos (Opus `isolated` nos 3; Sonnet e Haiku `scattered`), o
Adapter separa nos pilotos com a TransLog estrangeira (Haiku e Sonnet vazam os códigos; o Opus-03
vaza só a unidade, as horas) e o Strategy separa o Haiku dos outros dois.

## Item 5: as decisões em aberto (14/10/2026)

Decididas pelo Lucas, com os dados dos 8 pilotos:

| decisão | escolha | por quê |
|---|---|---|
| pedir testes no enunciado? | **não** | sem pedir, quem escreve teste por conta própria vira medida (de 4 a 39 testes nos pilotos), e o harness tem o que acrescentar em testes |
| "duas máquinas" | **sim** | a frase entrou na parte técnica do enunciado: *"Em produção, o serviço roda em duas máquinas ao mesmo tempo, atrás de um balanceador, as duas usando o mesmo banco."* |
| imagem Docker | **a v5, travada** | `experimento-harness:v5`, `sha256:8e96815c…`, Claude Code 2.1.288: a mesma dos 8 pilotos; o driver do PostgreSQL vem pelo Maven, com rede; nada pré-baixado (seria visível no `~/.m2` do agente) |
| modelos e réplicas | **os 11 do V5 × 4 níveis × 5 réplicas** | 220 execuções, como o V5: comparação direta com ele |

**Nada está congelado** (pedido do Lucas: "quero testar bastante"), e nenhum modelo roda com o enunciado
enquanto a organização não terminar.

**As duas máquinas na suíte.** A suíte sobe o serviço em duas instâncias com o mesmo banco, uma depois da
outra (como um deploy escalonado: subir as duas juntas mediria uma corrida na criação das tabelas, que o
enunciado não pede), e o cliente da suíte faz o papel do balanceador, alternando as chamadas entre elas. O
pedido criado numa máquina é pago na outra, e as corridas se dividem entre as duas. Prova, com um erro
plantado no Opus-03 que tira as travas do banco e põe um `synchronized` no pagamento (`lock-in-memory`):
**com 1 máquina a suíte não o via (MISSED); com 2, as 4 corridas falham.** O Opus-03 original continua com
463 de 463, e os 13 erros plantados são pegos com duas máquinas. O `--machines 1` existe só para essa
validação.

**Faltando, antes do congelamento: as ferramentas de teste novas.** A tabela "O que cada dimensão nova mede
aqui" previa o PIT (a qualidade dos testes do agente), o ArchUnit (regras de arquitetura indiscutíveis) e a
consulta ao `information_schema` (as restrições que o agente criou no banco). Só foram experimentados no
código do V5; falta fazer as ferramentas do V6, em Python, validadas nos pilotos.

## As ferramentas de teste novas (14/10/2026)

Em `evaluation/measures/` ([README](evaluation/measures/README.md)), em Python, todas numa cópia do
projeto do agente, dentro da imagem travada:
- **PIT** (testes): a nota é a taxa de mutantes mortos. Validada pelo "teste ingênuo": sem as asserções, a
  nota caiu em todos os 7 pilotos com testes que passam (Opus-03 de 0,886 para 0,539; Sonnet-03 de 0,782
  para 0,105).
- **ArchUnit** (arquitetura): pacotes presos em ciclo, classes de negócio que dependem de controller, classes
  de negócio que usam a web de entrada. As duas últimas deram 0 em todos os pilotos; uma classe plantada
  prova que acusariam.
- **O esquema do banco**: o `run_suite_against_service.py` o fotografa depois da suíte; a medida responde se o banco trava
  o estoque negativo e o aviso repetido, e conta as chaves estrangeiras.

Achado para discutir: o **Opus tem mais pacotes em ciclo** (3 a 6) que o Sonnet (0 a 2) e o Haiku (0),
porque divide o código por funcionalidade e liga os pacotes por listeners nos dois sentidos. A arquitetura
não acompanha as outras medidas, e isso é informação, não defeito da medida.

## A correção passa a ser da IA com o Lucas (15/10/2026)

Decisão do Lucas com o orientador: a correção é feita pela **IA junto com o Lucas**, lendo o código, no lugar
das réguas por regra fixa.

| peça | decisão | por quê |
|---|---|---|
| a régua do Semgrep (P1 a P7, F) | **sai** | a leitura do desenho passa para a IA com o Lucas |
| a arquitetura (ArchUnit) e o banco (as restrições do esquema) | **saem** | idem |
| a comparação do gabarito com o V5 | **sai** | o trabalho dela já foi feito (20.000 de 20.000) |
| o gabarito (`Calculator`, `Store`, `PromptExamples`) | **fica** | gera e confere os exemplos do enunciado, e é a resposta oficial dos pontos de dúvida |
| a suíte | **fica, como ferramenta da IA** | é a mesma prova para todos os serviços; a correção, a concorrência e o reinício não se veem lendo |
| a foto do esquema do banco | **fica**, como material de leitura da IA | o que o banco realmente criou é mais confiável que deduzir pelas entidades |
| o validador da suíte | **fica** | revalida a suíte quando o enunciado mudar |
| o PIT | **fica** | dá o número da qualidade dos testes, que nenhuma leitura dá |

O que saiu está no histórico do git (até o commit `3426ad23`).

**A base da rubrica da IA** vem do que foi feito: as perguntas P1 a P7 (Strategy: o código de cada caso mora
na casa dele? a escolha nomeia os casos?; a região como controle negativo; o Adapter pelo vazamento do
vocabulário da TransLog; o State pelas decisões fora da situação) e as decisões de calibração (a regra
cruzada pode morar na casa do par; converter a unidade da transportadora fora do adaptador é vazamento;
traduzir para o código da transportadora é trabalho do adaptador; classe aninhada é casa).

**Para a correção por IA se sustentar na banca** (a escrever no protocolo): a rubrica escrita antes; a
leitura às cegas (código anonimizado, sem nível nem modelo); o juiz travado (modelo, versão e prompt fixos),
cada código corrigido mais de uma vez; o risco de um Claude corrigir Claude declarado, ou um juiz de outra
família; e o Lucas lendo uma amostra às cegas, com a concordância medida (onde for baixa, a pergunta vira
descritiva).

## As quatro dimensões da correção (17/10/2026)

Combinado com o orientador: a correção da construção **não precisa de scripts deterministas** no lugar da
leitura, porque o Lucas revisa tudo junto com a IA. Ferramentas (o CK, o Sonar, as que já existem) entram para
**ajudar a IA a decidir**, como material de leitura, e não como nota. A correção julga quatro dimensões:

| dimensão | o que se julga | o material da IA |
|---|---|---|
| **arquitetura** | a divisão em pacotes, o sentido das dependências, o que o negócio sabe da web | o relatório de arquitetura (`<run>/architecture/`) e o código |
| **implementação dos padrões** | Strategy, Adapter e Factory, com o State como secundário | o código, com a rubrica (a base são as perguntas P1 a P7 e as calibrações da seção anterior) |
| **cobertura e qualidade dos testes** | quanto do código os testes executam, e quanto do que executam eles conferem | o PIT: `line_coverage` e `mutation_score` |
| **qualidade do banco** | as ligações entre as entidades, os relacionamentos, a organização (os tipos, os nulos, as travas) | a foto do esquema (`database-schema.json`), agora com as colunas, e as entidades no código |

**O ArchUnit volta, como relatório.** Ele tinha saído em 15/10 com as outras réguas, mas a arquitetura é uma
das quatro dimensões, e sem ele era a única sem nenhum fato de ferramenta. Os ciclos entre pacotes são o que
a leitura deduz mal: um ciclo que passa por 6 pacotes não aparece abrindo arquivo por arquivo. A saída
deixou de ser uma contagem e virou um relatório por execução (`evaluation/measures/report_architecture.py`,
em `<run>/architecture/`): os pacotes, as dependências entre eles com as classes que as fazem, cada grupo de
pacotes em ciclo **uma vez só** (com as dependências de dentro do grupo, no lugar de um ciclo por caminho,
como o ArchUnit contava) e as classes de fora da web que dependem de controller ou usam a web de entrada.
O que o ciclo significa (os listeners do Opus nos dois sentidos, o serviço do Sonnet-02 que recebe o DTO da
web) é a IA quem decide.

Validação (17/10): nos 8 pilotos, os pacotes em ciclo são **os mesmos** da medida antiga (Opus 4, 3 e 6;
Sonnet-02 2; os outros 0), e a classe plantada numa cópia do Opus-03 (guarda um controller e devolve um
`ResponseEntity`) aparece nas duas regras de camada, que dão 0 em todos os pilotos.

**A foto do banco ganha as colunas**: o tipo com o tamanho (`numeric(12,2)`, `character varying(255)`), se
aceita nulo e o valor padrão. Sem elas, a organização do banco não se julgava: dinheiro em `numeric` ou em
`double`, campo obrigatório que aceita nulo. Conferida no Opus-03 (40 colunas em 6 tabelas; a suíte segue
463 de 463). **Nenhum piloto tem a foto no disco**: para a IA ler o banco dos pilotos, a suíte tem de rodar
de novo neles.

**Em aberto:**
- **a suíte** (a correção do comportamento) não está entre as quatro dimensões. Fica como ferramenta, e
  falta decidir se a correção entra como dimensão ou só como condição (um serviço que não sobe);
- **o CK e o Sonar** ainda não entraram. O CK (acoplamento, coesão e complexidade por classe) é um jar sem
  servidor, no padrão das outras ferramentas; o Sonar precisa de um servidor, e o que ele dá a mais são os
  code smells e a duplicação;
- **a rubrica** ainda não está escrita, e o protocolo da seção anterior também não. *(Feitos em 17/10: ver a
  seção seguinte.)*

## O juiz, as réguas e a validação (17/10/2026)

Em `evaluation/judge/` ([README](evaluation/judge/README.md), [protocolo](evaluation/judge/PROTOCOLO.md)). O que foi
feito, na ordem combinada com o Lucas:

1. **As réguas das quatro dimensões** (`rubric/`). Os padrões herdam a régua do Semgrep (P1 a P7, F) e as decisões
   de calibração de 14/10, e ganham o `E` (estrutura sem necessidade, o exagero colateral da manutenção). A
   arquitetura (A1 a A6) é lida com o relatório do ArchUnit; os testes (T1 a T4), com os números do PIT; o banco
   (B1 a B10), com a foto, e cada item cita a fonte publicada (a documentação e a wiki do PostgreSQL, a do Spring
   Boot, Elmasri & Navathe). A fonte que eu queria para "o `ddl-auto` não serve em produção" (o guia do Hibernate)
   não foi confirmada; o B9 cita só o Spring Boot ("um mecanismo só", "ferramentas de migração de mais alto nível").
2. **A correção é uma IA lendo as réguas em markdown** (decisão do Lucas, 17/10: "não quero script de juiz nem
   nada, vai ser markdown para IA ler e interpretar"). O `rubric/common.md` diz o que ler na pasta da execução, o
   que não abrir, as regras (uma dimensão por conversa, contra o efeito halo; evidência; `indeterminado`) e o
   formato da resposta (`correcao/<dimensão>.md`). Um script de juiz chegou a existir (no molde do avaliador da
   manutenção) e rodou a validação abaixo; saiu no mesmo dia. As respostas dele ficaram em `batches/`.
3. **O material do banco**: a foto ganhou todos os índices (o B3, chave estrangeira sem índice, precisa dos não
   únicos), e a suíte rodou de novo nos 8 pilotos para tirar a foto. Só o Opus-03 fala a API atual (463 de 463);
   nos outros a suíte não se aplica (a API de antes), mas a foto sai, porque o serviço sobe e cria o esquema.
   Os dois Haiku não sobem: a foto sai vazia, e a régua diz para corrigir pelo código. Das saídas da suíte nos outros pilotos, só a foto
   ficou.
4. **A validação**: 21 de 21 no serviço com defeitos plantados (`planted/`); 115 de 116 contra as ferramentas nos
   pilotos (a régua do Semgrep, rodada do histórico, e as fotos). Números e detalhes no README do juiz.
5. **O protocolo** e o `infra/scripts/freeze.py` (o congelamento, como o do V5). **Não congelei**: faltam os
   harnesses, a leitura do Lucas e as decisões abaixo.

**Achados no caminho:**
- **Os pilotos rodaram com quatro versões anteriores do enunciado** (nenhum com o atual). A régua manda ler o
  `prompt.md` de cada execução.
- **A suíte quebrava** quando nenhum pedido podia ser criado (um pool de 0 threads na concorrência), em vez de
  relatar as diferenças. Corrigido (`ConcurrencyChecks.allAtOnce`); um serviço que funciona não muda de resultado.
- **A retirada das asserções do PIT deixa escapar uma asserção** quando o comentário logo antes dela tem um `;` (o
  ponto e vírgula do comentário parece o fim do comando anterior): no Opus-03, 1 asserção ficou. A conclusão da
  validação do PIT (a nota cai em todos) não muda; não mexi, para não mudar números já validados sem o Lucas ver.
- **O enunciado pede o PostgreSQL 18, e a suíte sobe o 16** (`postgres:16-alpine`). Não mexi: é para o Lucas
  decidir qual dos dois muda.

**Para o Lucas decidir:**
- **os gramas no P6**: a régua põe os gramas entre as unidades da TransLog (o Opus-02 converte kg em gramas fora
  do adaptador, e a IA diz `vazado`); a régua do Semgrep só via as horas;
- **o critério de aceite das réguas** (a proposta do protocolo: todos os plantados e 80% de concordância por
  dimensão) e o que fazer quando duas correções da mesma execução discordam;
- **o custo da correção**: com o Opus 5.5 em effort `high`, cerca de US$ 0,50 por dimensão de uma execução; as 220
  execuções corrigidas duas vezes nas quatro dimensões seriam umas 1.760 correções, uma por conversa. Vale testar
  um effort menor ou outro modelo, com as mesmas provas;
- **o anonimato** nas execuções que valem: o nome da pasta diz o modelo, e sem script a cópia neutra é feita à mão;
- os pontos que já estavam em aberto: a suíte como dimensão, o CK e o Sonar, e as réguas longe dos braços do
  harness.

**O que falta fazer:** a prova 3 (corrigir os pilotos de novo e comparar; parada em 17/10 para não estourar a
janela de 5 horas da assinatura, que o primeiro lote levou de 36% para 61%) e a prova 4 (a leitura às cegas do
Lucas).

