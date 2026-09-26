# Testes de extensão

> [!danger] Nunca entra no container
> Vale o mesmo que para o resto de `avaliacao/`. O `.dockerignore` é lista
> branca e já barra a pasta do contexto de build.

Confirmam o **C5** da rubrica com um número em vez de opinião: quantos arquivos
**existentes** precisam mudar para acrescentar uma variante nova.

| ponto | extensão | regra |
|---|---|---|
| **P1** | `DRONE` | R$ 30,00 fixo · prazo 0 · só até 2 kg |
| **P2** | `DEZOFF` | R$ 10,00 de desconto nos produtos · sem condição |
| **P3** | `CARTEIRA_DIGITAL` | 2% de desconto no total · só à vista · indisponível acima de R$ 500,00 |

Valores conferidos com `BigDecimal` e arredondamento meio-para-o-par, pela
mesma rotina já validada contra os quatro exemplos do enunciado e contra E5 e
E6 do gabarito.

---

## Procedimento

> [!warning] Revisto em 21/09/2026: não há mais rubrica antes
> Esta seção dizia "Rubrica antes, extensão depois", porque fazer a extensão
> primeiro influenciava a nota de C5. A rubrica saiu do desenho — plano.md §14.4
> — e **a extensão virou o desfecho primário**. Não há mais ordem a respeitar:
> ela é a primeira coisa, e a única que produz número.

Por pacote, e **um ponto por vez**, sempre partindo do código original:

**1. Congelar o original.** Dentro da cópia do workspace:

```bash
git init -q && git add -A && git commit -qm base
```

Isso existe só para o passo 4 dar o número sozinho. Faça numa **cópia**, nunca
no workspace arquivado.

**2. Implementar a variante**, fazendo a **menor alteração que funcione**. Sem
refatorar de passagem, sem melhorar nada em volta: o que se mede é o custo da
extensão no desenho que o modelo entregou, não o desenho que você faria.

**3. Rodar os casos** contra a aplicação:

```bash
CASOS=avaliacao/testes-extensao/p1-drone.json \
  avaliacao/ferramentas/conferir-exemplos.sh <run_id>
```

Extensão que não passa nos casos não conta como feita, e o pacote recebe a
anotação — não adianta contar arquivos de uma implementação que não funciona.

**4. Contar, mecanicamente:**

```bash
git status --porcelain            # A = criado, M = alterado
git diff --numstat                # linhas alteradas, só nos existentes
```

Registrar três números: **arquivos criados**, **arquivos existentes
alterados**, **linhas alteradas em arquivos existentes**.

**5. Desfazer** antes do ponto seguinte:

```bash
git reset --hard base && git clean -fdq
```

---

## Como o número conversa com o C5

| C5 | arquivos existentes alterados |
|---|---|
| **2** | zero, ou no máximo um registro declarativo |
| **1** | exatamente um |
| **0** | dois ou mais |

O registro declarativo do nível 2 é uma linha num catálogo, um `@Component`, uma
entrada de `Map`. Não é lógica da variante nova.

---

## A limitação, declarada

> [!warning] As três extensões não separam "parametrizado" de "uma classe por variante"

Todas as três são da **mesma família de regra** que algo que já existe:

- `DEZOFF` é valor fixo nos produtos, como o `MENOS50` sem o mínimo
- `DRONE` é tarifa fixa com limite de peso, como o `MOTOBOY`
- `CARTEIRA_DIGITAL` é percentual sobre o total à vista, como o `PIX`, com um
  teto, como o `BOLETO`

Num desenho de regra parametrizada — uma tabela de `{tipo, valor, mínimo}` com
cálculo genérico — as três entram como **linha de dado**, zero arquivos
existentes alterados, C5 = 2. Num desenho de uma classe por variante, entram
como arquivo novo, também C5 = 2.

Então o teste separa `{classes, parametrizado}` de `enum+corpo` (C5 = 1) e de
`switch` (C5 = 0), e **não separa classes de parametrizado**.

Isso foi decidido em 20/09/2026, e a decisão foi **aceitar**. O motivo é que o
empate está certo pela própria definição de C5: um desenho parametrizado que
absorve a variante como dado faz **menos** que "criar uma implementação", não
mais. Ele é genuinamente aberto para extensão daquela família, e a pergunta do
TCC é se o harness aumenta o reconhecimento de Strategy — não qual dos dois
desenhos corretos é mais elegante.

Para separar os dois seria preciso uma segunda extensão por ponto exigindo
comportamento de família nova: por exemplo um cupom cuja elegibilidade dependa
da modalidade de entrega escolhida, que uma tabela de `{tipo, valor, mínimo}`
não expressa sem mudar a assinatura. Dobra o trabalho manual do avaliador — de
54 aplicações para 108, com 18 pacotes — e ficou como trabalho futuro.

---

## Registro

Uma linha por pacote × ponto, na planilha que o `anonimizar.mjs` gera:

```csv
codigo_cego,ponto,extensao,passou_nos_casos,arquivos_criados,arquivos_alterados,linhas_alteradas,forma,observacoes
```

`arquivos_alterados` é o **desfecho primário**: quantos arquivos que já existiam
precisaram mudar para a variante nova entrar. Zero é o melhor resultado.

`forma` é o desfecho **secundário e descritivo**, anotado à mão ao abrir o
pacote: `classes`, `enum` com corpo, mapa de dados, regra parametrizada, `enum`
sem comportamento, ou `switch`/`ifs`. Não entra na comparação principal.

> [!note] Revisto em 22/09/2026
> A coluna era `C5_confirmado`, definida como `sim` quando o número batia com a
> nota dada na leitura do código. Nota que saiu junto com a rubrica em 21/09.
> No lugar entrou `forma`, e este arquivo foi o último a ser alinhado.
