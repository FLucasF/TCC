# A régua do Semgrep (V6)

Lê o desenho de cada pacote com regras fixas, **sem IA**: o mesmo código dá sempre a mesma
resposta. É a régua do V5 reescrita em Python, com os nomes da API em inglês (os nomes em português
continuam reconhecidos como sinônimos), mais três padrões novos.

| ponto | padrão | pergunta | valores |
|---|---|---|---|
| P1 entrega, P2 cupom, P3 pagamento, P4 clube | Strategy | `location`: algum caso aparece fora da casa dele? | `isolated`, `scattered` |
| | | `selection`: o caminho até a conta nomeia os casos? | `lookup`, `single-conditional`, `conditional-in-calculation` |
| P5 região (controle negativo) | — | `form` | `enum-data`, `enum-body`, `classes`, `map`, `switch`, `other` |
| | | `proportion`: a resposta tem o tamanho do problema? | `data`, `conditional`, `structure` |
| P6 TransLog | Adapter | o vocabulário da transportadora fica do lado dela? | `isolated`, `leaked` |
| P7 situação do pedido | State (secundário) | as decisões sobre a situação moram na própria situação? | `isolated`, `scattered` |
| F | Factory (**descritiva**, sem hipótese) | como os objetos das estratégias são criados | `container`, `configuration`, `factory`, `new`, `enum`, combinados com `+`, ou `none` |

Qualquer pergunta pode sair `undetermined`: o pacote não cita nenhum nome do ponto (o agente usou
outros nomes) ou o Semgrep não conseguiu ler um arquivo. Um `undetermined` nunca vira um `isolated`
falso.

## Como roda

Da pasta `TCC_V6`, com o Docker aberto:

```bash
python evaluation/semgrep/detect.py <raiz-dos-pacotes> <saida.csv>
```

A raiz tem uma subpasta por pacote; dentro dela, o projeto é o do `pom.xml` mais raso, a mesma
regra do build e da suíte. A saída tem uma linha por pacote, com as respostas, a **evidência** (a
primeira linha que contou de cada regra) e os **avisos**; a primeira linha do CSV traz o hash das
regras, do classificador, da cópia limpa e da imagem do Semgrep (`semgrep/semgrep:1.95.0`, travada
pelo hash, a mesma do V5).

| arquivo | o que faz |
|---|---|
| `points.py` | os nomes dos casos de cada ponto, os sinônimos, o vocabulário da TransLog e as regras cruzadas (a única tabela a mudar para outro enunciado) |
| `rules.py` | gera o `rules.yml` a partir da tabela; `--check` confere que o `rules.yml` é o gerado. Nunca se edita o `.yml` à mão |
| `rules.yml` | as 64 regras: cada uma marca uma **linha** de código |
| `clean_copy.py` | a cópia sem comentários que o Semgrep lê, com as linhas no mesmo lugar, só do projeto que o build compila |
| `classify.py` | transforma as linhas marcadas nas respostas da régua |
| `detect.py` | encadeia tudo; recusa se o `rules.yml` não for o gerado, ou se a saída já existir |
| `compare_with_v5.py` | a validação contra o detector do V5 (abaixo) |

## Como as respostas saem

**P1 a P5** seguem a régua do V5 (`../../../TCC_V5/evaluation/regua.md`, §2 e §3): o que nomeia um caso,
o que não conta (a recusa, a lista de válidos, o registro, a fábrica) e o alarme de nenhum nome.

**P6, Adapter.** O lado da transportadora são os arquivos que fazem a chamada HTTP, mais os arquivos
da mesma pasta que não conhecem os nossos casos (os records da resposta, os enums dela). O
vocabulário dela é o formato da API (`price_cents`, `eta_hours`, `ZIP_NOT_SERVED`, a rota
`/v2/quote`), os nomes desses campos no nosso código (`priceCents`, `etaHours`) e os códigos de
serviço (`"STD"`, `"EXP"`). Usar qualquer um deles fora do lado da transportadora é `leaked`;
**declarar** um campo com esse nome (o record da resposta, um `@JsonProperty`) não é.

**P7, State.** Uma decisão é uma comparação com uma situação (`status == PAID`), um rótulo de
`switch` ou um conjunto que não cita todas as situações. A casa é o tipo da situação (`OrderStatus`,
`OrderState`, uma classe por situação), inclusive aninhado (`enum Status` dentro de `Order`). Uma
decisão fora dela é `scattered`. Não contam a lista de válidos e o rótulo que só traduz o texto
para a constante.

**F, Factory.** Só descreve: `container` (estratégias com `@Component`/`@Service`), `configuration`
(métodos `@Bean`), `factory` (um `switch`/`if` que só devolve o objeto, o `single-conditional` dos
pontos), `new` (um `new` de uma classe de estratégia fora dessas formas), `enum` (constantes com corpo).

## Validação contra o V5

O P1 a P5 são a régua do V5 reescrita, então o detector novo tem de dar as mesmas respostas que o
detector congelado do V5, nos mesmos pacotes. Nos 220 pacotes cegos do V5
(`TCC_V5/evaluation/strategy/packages`), comparados com o `TCC_V5/analysis/semgrep-V5-STRATEGY.csv`:
**2.200 de 2.200 respostas iguais** (14/10/2026). As mudanças de calibração abaixo não mudaram
nenhuma resposta do V5, cujo enunciado não tinha as regras cruzadas nem a transportadora.

```bash
python evaluation/semgrep/detect.py ../TCC_V5/evaluation/strategy/packages v6-on-v5.csv
```

```bash
python evaluation/semgrep/compare_with_v5.py ../TCC_V5/analysis/semgrep-V5-STRATEGY.csv v6-on-v5.csv
```

**Velocidade.** Os arquivos vão para dentro do container num `tar` pela entrada padrão, e não por
uma pasta montada: montada, a pasta do Windows fazia o Semgrep ler cada arquivo atravessando a VM
do Docker, e os 220 pacotes passaram de meia hora a 4% de CPU. Com o `tar`, levam 37 segundos.

## Decisões de calibração (14/10/2026, nos 8 pilotos)

1. **Tradução para o código da transportadora não é decisão de entrega.** No Opus-03, o
   `case EXPRESS -> "EXP"` está dentro do adaptador, e o `EXPRESS` ali é o serviço da TransLog
   (`CarrierService.EXPRESS`), homônimo da nossa entrega. Uma linha que só devolve `"STD"`/`"EXP"`
   é trabalho do adaptador, medido no P6.
2. **Classe aninhada é casa.** O V5 decidia a casa pelo nome do arquivo; o V6 olha também o tipo em
   volta da linha (uma `class FreteGratis` dentro de `Cupons.java`, no Sonnet-01).
3. **A regra cruzada pode morar na casa do par** (decisão do Lucas). O enunciado tem duas regras que
   obrigam a citar um caso de outro ponto: o FREESHIPPING não vale na retirada, e o frete grátis do
   GOLD não vale no motoboy. Citar o caso dentro da casa do par (STORE_PICKUP dentro do cupom
   FreeShipping, COURIER dentro do nível Gold, e o inverso) não espalha; citar num terceiro lugar
   (um serviço) espalha. A evidência mostra essas linhas em `pN-cross-rule-home`.
4. **Conversão de unidade fora do adaptador é vazamento** (decisão do Lucas). O Opus-03 isola o
   HTTP e o formato da TransLog, mas converte as horas em dias (`quote.etaHours()`) na estratégia
   de entrega: o adaptador entregou a unidade da transportadora, e não a nossa. Sai `leaked`.

## Resultado nos 8 pilotos

| | P1–P4 | P5 | P6 | P7 | F |
|---|---|---|---|---|---|
| Haiku (2) | `scattered`/`conditional-in-calculation` em todos | `conditional` | `leaked` no piloto 2 | `scattered` | `none` |
| Sonnet (3) | `isolated`/`lookup` | `data` | `leaked` no piloto 3 | `scattered` nos 3 | `container` |
| Opus (3) | `isolated`/`lookup` | `data` | `leaked` no piloto 3 | `isolated` nos 3 | `container` |

Os pilotos 1 e 2 de cada modelo ainda tinham a TransLog sem vocabulário estrangeiro: o P6 deles
não mede nada (sai `isolated` por falta do que vazar). A Factory saiu `container` em todos os que
têm estratégias, como os pilotos já indicavam: por isso ela é descritiva.
