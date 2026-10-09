# Notas da calibração do Lucas

Hesitações, dúvidas sobre o texto da régua e divergências, pacote a pacote. No fim
da calibração, cada item vira uma regra nova, um esclarecimento na régua ou nada (com
o motivo), e entra no `DECISOES.md`. O método está em
[`PLANO-TESTE-APRENDENDO.md`](../../PLANO-TESTE-APRENDENDO.md).

## L3RG

### P1 (entrega), 08/10/2026

Nenhuma hesitação entre valores.

| tipo | o que aconteceu | proposta |
|---|---|---|
| texto da régua | "qual o valor da `forma`?" não ficou claro de primeira: a régua não diz, no começo, que cada propriedade é um campo com uma lista fechada de respostas, e que se escolhe uma | uma frase na abertura do §2: "cada propriedade é um campo; o valor é uma das palavras da tabela dela" |
| procedimento | a `assinatura` exigiu conferir dois lugares (o limite no caso exigente e o uso dele fora, sem nomear o caso); a primeira resposta veio só com o primeiro | no roteiro de leitura, lembrar que `comporta` precisa das duas evidências |

### P2 (cupom), 08/10/2026

Nenhuma hesitação entre valores.

| tipo | o que aconteceu | proposta |
|---|---|---|
| procedimento | a busca por `fretegratis` achou também o método `freteGratis()` do **clube** (o benefício do Ouro, ponto P4): um homônimo de outro ponto. Quem lê precisa separar o que é do caso do que só tem nome parecido | no §1 da régua ("Antes de ler"), avisar que a busca por nome pode pegar homônimos de outros pontos, e que eles não contam na `localizacao` do ponto lido |
| procedimento | de novo, a evidência do `comporta` veio só com o lado do caso exigente (onde cada um usa o contrato), sem a linha em que o código de fora usa o contrato sem nomear o caso. **Segunda vez** | reforça a proposta do P1: o texto da `assinatura` (§2.4) deveria pedir as duas evidências explicitamente |

### P3 (pagamento), 08/10/2026

**Três hesitações**, as primeiras da calibração.

| tipo | o que aconteceu | proposta |
|---|---|---|
| hesitação (`selecao`) | "não sei": logo depois da busca no catálogo, a calculadora tem dois `if` sobre a forma (`aceitaParcelas`, `disponivel`). A regra que resolve (§2.3, "`if` só de validação") existe, mas fica **abaixo** da tabela, e quem lê a tabela primeiro não a vê | trazer a exceção para dentro da linha `consulta` da tabela, ou pôr as três situações antes dela |
| hesitação (`assinatura`) | "o que vc acha?": com **dois** casos exigentes, a pergunta só andou quebrada em seis perguntas de sim ou não, por caso | um roteiro curto no §2.4, repetido por caso exigente: (a) está num método do contrato? (b) o uso de fora nomeia o caso? (c) há `if` com o nome dele fora da classe? |
| procedimento | **terceira vez** que a prova (b) do `comporta` veio incompleta: só o uso de `:67`; faltavam `:71` (o boleto) e `:74` (os juros). Com dois casos exigentes, há um uso de fora para cada | o roteiro acima resolve: a prova (b) é pedida por caso |
| hesitação (`custo_caso_novo`) | "não sei, me ajude": o `VALE` não escreve nada além do que os métodos `default` da interface já dão (só à vista, sempre disponível). A régua não diz como pensar um caso novo que se apoia nos padrões. A resposta final veio com duas condições a confirmar (`CatalogoFormasPagamento.java:16-17`, `CalculadoraResumo.java:66-74`) | um roteiro no §2.6: imaginar o arquivo novo copiando o caso mais parecido, e perguntar, por arquivo que já existe, "precisa de uma linha nova aqui?" (os `default`, o registro, o tipo do resultado, o serviço) |

### P4 (clube), 08/10/2026

**Duas hesitações**; o `custo_caso_novo` ficou `indeterminado`.

| tipo | o que aconteceu | proposta |
|---|---|---|
| procedimento | a busca do Claude por `nivel` pegou "dispo**nivel**" (sete linhas de ruído): busca por pedaço de palavra traz falsos positivos | no §1 da régua, junto do aviso dos homônimos: conferir a palavra inteira antes de considerar a linha |
| hesitação (`assinatura`) | "como assim nomeia?": a régua usa "nomear o caso" em quatro propriedades e não define o termo | definir no começo do §2, com o teste do caso novo: "se eu criar um caso parecido, esta linha precisa mudar?" |
| texto da régua | "como assim qual valor?", **segunda vez** (a primeira foi a `forma` do P1) | reforça a frase de abertura do §2: "cada propriedade é uma coluna; o valor é uma das palavras da tabela dela" |
| hesitação (`custo_caso_novo`) | "não sei", **segunda vez** (a primeira foi o `VALE` do P3), mesmo com o roteiro de sim ou não. Ficou `indeterminado` | é a propriedade que mais precisa de roteiro; um passo a passo foi escrito no [`GUIA-DA-REGUA.md`](../GUIA-DA-REGUA.md) §4.6, candidato a entrar no §2.6 da régua |

Depois deste ponto, o Lucas pediu uma explicação geral da avaliação: virou o
[`GUIA-DA-REGUA.md`](../GUIA-DA-REGUA.md). Os pontos seguintes são lidos já com o guia.
