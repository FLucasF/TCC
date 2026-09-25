# Como ler as hipóteses — questão aberta para o orientador

Escrito em 24/09/2026, **antes de o lote rodar**.

Este documento existe para uma conversa. Ele não decide nada: apresenta o
problema, o que está disponível, e o que cada caminho custa. A decisão é de
escopo e de método, e por isso é do orientador.

A pendência correspondente é a **§7 do `pre-registro.md`**, que está declarada em
aberto e bloqueia o lote.

---

## 1. O problema, em uma frase

**Não existe critério escrito para dizer se uma hipótese foi apoiada.**

O experimento vai produzir 18 execuções e um conjunto de números. Sem um critério
definido **antes**, qualquer critério escolhido **depois** terá sido escolhido
pelos próprios números — e o pré-registro, na §9, declara fora "qualquer corte,
agrupamento ou teste estatístico não listado".

Então ou a §7 fecha antes do lote, ou o trabalho fica sem forma legítima de
concluir.

## 2. O que foi descartado, e por quê

O desenho original previa **Mann-Whitney** comparando os dois braços.

Ele foi descartado por aritmética, não por preferência: comparando **dois grupos
de 3**, existem C(6,3) = 20 arranjos possíveis, então o menor p bicaudal
alcançável é **0,10**. Um p < 0,05 seria impossível por construção, e reportar
"não significativo" descreveria o tamanho da amostra, não o efeito.

A decisão de descartar está correta. O que não aconteceu foi pôr algo no lugar.

## 3. O que o desenho pareado torna disponível

O experimento não tem dois grupos de 3. Ele tem **9 pares simultâneos**: cada
execução `CONTROL` roda no mesmo instante que a `HARNESS` do mesmo modelo, em
containers paralelos. São 3 modelos × 3 réplicas.

O pareamento foi adotado por outro motivo — a leitura por mediana de célula
produzia conclusão errada no dado de calibração. Mas ele tem uma consequência
estatística que vale registrar: **num teste sobre pares, cada par é uma
observação.**

**Teste de sinal exato, bicaudal, sobre 9 pares:**

| pares na direção prevista | p bicaudal |
|---|---|
| 9 de 9 | **0,0039** |
| 8 de 9 | **0,0391** |
| 7 de 9 | 0,18 |
| 6 de 9 | 0,51 |

E ele tolera perda de n por empate:

| pares não-empatados, todos na mesma direção | p bicaudal |
|---|---|
| 7 de 7 | 0,016 |
| 6 de 6 | 0,031 |
| 5 de 5 | 0,063 |
| 4 de 4 | 0,125 |

**Ou seja: significância a α = 0,05 passa a ser alcançável**, o que não era o caso
com a comparação entre grupos.

Uma alternativa, se o desfecho for numérico em vez de ordinal grosso:
**Wilcoxon pareado**, que usa a magnitude da diferença e não só o sinal. Mesmo
piso de p (0,0039 com 9 pares), mais poder quando as magnitudes informam.

## 4. Três problemas honestos com isso

**(a) A H1 agrupada e a H3 se contradizem na suposição.**

O teste de sinal sobre os 9 pares trata-os como 9 observações do **mesmo**
fenômeno, intercambiáveis. Mas a **H3 afirma que o efeito é maior no Haiku 4.5 do
que no Opus 5** — isto é, que o fenômeno **não** é o mesmo nos três modelos.

Se a H3 estiver certa, agrupar os 9 mistura efeitos de tamanhos diferentes. O
teste continua respondendo algo — "o harness ajuda, em média, nestes três
modelos" — mas isso é uma pergunta ligeiramente diferente da que está escrita.

**(b) Empates podem inviabilizar o teste.**

Se o desfecho primário for ordinal grosso — por exemplo três níveis — muitos
pares vão empatar. Com 4 pares não-empatados ou menos, **nenhum resultado
alcança 0,05**, mesmo que todos apontem na mesma direção.

Isso não é hipotético: quanto mais grossa a régua, mais empates. E a régua ainda
não foi definida (ver §6).

**(c) São cinco hipóteses.**

Testar H1 a H5 cada uma a 0,05 é uma família de testes. O tratamento usual é
declarar **uma** como primária confirmatória e as outras como secundárias ou
exploratórias. A alternativa é corrigir para múltiplas comparações, o que com
este n derruba tudo.

## 5. Um problema de desenho, separado da estatística

**A H3 e a H5 comparam magnitudes de efeito.** A H3 diz que o efeito é maior num
modelo que noutro; a H5, que é maior em dois pontos do enunciado que num terceiro.

Com **3 pares por modelo**, a diferença entre "3 de 3" e "2 de 3" é **um par**. E
um par está dentro da variância já medida entre execuções idênticas: no dado de
calibração, execuções com mesmo modelo, mesma condição, mesmo enunciado e mesmo
harness variaram de **1,5× a 2,4×** no consumo, e produziram formas de código
diferentes entre si.

Isto sugere que a H3 e a H5 não são testáveis com este `n`, qualquer que seja o
teste escolhido. A questão para o orientador é se elas passam a ser declaradas
**exploratórias** ou se o desenho muda.

## 6. E o desfecho primário ainda não tem instrumento

O `pre-registro.md` declara isso na §5: o desfecho primário é uma medida de
reconhecimento e implementação de Strategy, aplicada **às cegas** aos pacotes, e o
instrumento é especificado num segundo documento escrito depois de os pacotes
existirem e **antes** de qualquer pacote do lote ser avaliado.

Isso é pré-registro em duas etapas e é legítimo — mas tem consequência direta
aqui: **a escolha da régua determina quantos empates aparecem**, e portanto se o
teste do §3 tem poder ou não.

Régua de três níveis → muitos empates → possivelmente nenhuma significância.
Régua numérica → poucos empates → Wilcoxon pareado fica disponível.

As duas decisões estão acopladas e provavelmente devem ser tomadas juntas.

---

## 7. As opções, sem recomendação

| | caminho | o que ganha | o que custa |
|---|---|---|---|
| **A** | Teste de sinal exato nos 9 pares, α = 0,05, **H1 como única confirmatória**; H2 a H5 declaradas secundárias | Significância alcançável, teste padrão, sem suposição de distribuição | Assume que os três modelos medem o mesmo efeito, o que a H3 nega |
| **B** | Teste por modelo, 3 pares cada, **sem agrupar** | Respeita a H3: cada modelo é sua própria pergunta | Com 3 pares, o menor p bicaudal é 0,25. Nenhum modelo pode atingir significância |
| **C** | **Só descritivo**: publica quantos pares foram em cada direção, por modelo e por ponto, e não declara teste nenhum | Imune a crítica estatística. Honesto com o `n` | Abre mão da significância que o pareamento tornou possível |
| **D** | Aumentar o `n` até o teste por modelo ter poder | Resolve a tensão de verdade | Cada réplica a mais custa 3 execuções e um terço a mais de avaliação manual. Para 3 pares por modelo virarem 6, são 18 execuções extras |

## 8. O que precisa ser respondido

1. A H1 é testada **agrupando os três modelos**, ou **por modelo**?
2. Qual hipótese é a **primária confirmatória**? As outras viram secundárias?
3. A H3 e a H5 continuam confirmatórias, ou passam a exploratórias?
4. Empate sai da conta e reduz o `n`, ou conta contra a hipótese?
5. A H2, que é não-direcional, tem regra própria? O dado de calibração mostra que
   o sinal do efeito sobre o consumo **muda entre modelos** — agrupar os nove
   faria sinais opostos se cancelarem.
6. O `n` de 3 por célula está fechado, ou é revisto à luz disto?

---

> [!note] Procedência de cada afirmação deste documento
> O descarte do Mann-Whitney e a conta dos 20 arranjos vêm do plano da versão
> anterior deste trabalho.
>
> A variância de 1,5× a 2,4× e a comparação entre leitura pareada e mediana de
> célula foram **medidas** nas 49 execuções de calibração, e estão no
> `diario-de-bordo.md` e na §8 do `pre-registro.md`.
>
> **A proposta do teste de sinal sobre os 9 pares, os p-valores da §3 e a tensão
> apontada na §4(a) não vêm de nenhum documento anterior nem de orientação
> recebida.** Foram levantados em 24/09/2026 durante a preparação do lote, e não
> passaram por revisão de ninguém. É por isso que este documento existe em vez de
> a §7 já estar fechada.
