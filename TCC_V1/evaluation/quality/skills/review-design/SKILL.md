---
name: review-design
description: Revisa o design de baixo nível de um projeto Java da revisão de qualidade - como o agente resolveu a variação que o texto pede (o pattern, com as classes acerto, alternativa equivalente, malfeito ou omissão), a estrutura sem necessidade (o exagero) e o design do resto do código. Sem script - leitura do código com os critérios de cada pattern em references/. Use na coluna "Design de baixo nível" da revisão.
---

# O design de baixo nível

Esta skill é material de trabalho para a revisão em andamento: leia os critérios do pattern do cenário e o código, e
escreva a seção "Design de baixo nível" do `review.md`. Não há script: a medida é a leitura.

## Os critérios do pattern

O texto que o agente recebeu pede, sem dizer o nome, um pattern: ele mostra uma variação que já tem vários casos e
que vai continuar crescendo. Leia a referência do pattern do cenário (`task.md` diz qual), que traz o ponto de
partida (na construção e na manutenção), o que o cenário pede e o que conta como cada classe:

| cenário | pattern | referência |
|---|---|---|
| 01 | Strategy | `references/strategy.md` |
| 03 | Observer | `references/observer.md` |
| 05 | Factory Method | `references/factory-method.md` |
| 07 | Decorator | `references/decorator.md` |
| 09 | Template Method | `references/template-method.md` |

A parte "o comportamento pedido, para conferir" das referências fica para a coluna do comportamento, que tem o seu
roteiro.

## Como ler

- **Na construção,** leia o `enunciado.md` inteiro, com atenção à parte do fim, e depois o código do `projeto/` que
  trata dessa parte; o resto quando precisar.
- **Na manutenção,** leia o `pedido.md` inteiro, depois o `changes.patch` inteiro e os arquivos da `final/` que ele
  toca; os casos "que já existiam" são os que a base tinha, e eles também têm de ir para a estrutura.

Julgue pelo código, nunca pelos nomes: uma classe chamada `Strategy` que esconde um `switch` não é um Strategy.

## A classe e o exagero

- `acerto`: o pattern aplicado direito, para todos os casos, inclusive os que já existiam;
- `alternativa equivalente`: outra estrutura que a referência do pattern aceita de antemão;
- `malfeito`: a estrutura existe, mas convive com o jeito sem ela (só parte dos casos nela, quem usa a estrutura
  ainda perguntando qual é o caso, um lugar que continua fazendo à mão);
- `omissão`: a variação ficou num `if`, num `case` ou numa cópia por caso.

Na dúvida entre duas classes, dê a que a evidência sustenta melhor e conte a dúvida na seção "Dúvidas".

O **exagero** é estrutura sem necessidade fora da parte que o pattern resolve: uma interface com uma implementação só
que nada substitui, uma fábrica, um builder, uma camada que só repassa. `exagero` se houver, `contenção` se não.

## A nota: o design do resto do código

A classe mede a parte do pattern; a nota mede o design de baixo nível do projeto inteiro, o que um revisor de código
apontaria numa leitura cuidadosa:

- duplicação (a mesma regra ou o mesmo trecho copiado em vários lugares);
- métodos longos que fazem várias coisas, e condições aninhadas difíceis de seguir;
- os tipos certos para o domínio (`BigDecimal` para dinheiro com o arredondamento pedido, `LocalDate` para datas,
  `enum` ou tipos próprios em vez de texto solto);
- o estado: objetos que mudam sem controle, campos públicos, `static` mutável;
- os erros: exceções engolidas, `catch (Exception)` que esconde a causa, `null` sem cuidado;
- nomes que dizem o que a coisa faz.

- `bom`: um código que um revisor exigente aceitaria como está (a classe do pattern não entra na nota);
- `com problemas`: alguns desses defeitos, localizados;
- `ruim`: defeitos espalhados, que tornam o código difícil de mudar com segurança.

Na seção, dê a classe, o exagero e a nota, e o porquê de cada um com `arquivo:linha`.
