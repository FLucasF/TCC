# Decisões de projeto

O `OBJETIVO.md` e os READMEs dizem **o que** o estudo é; este arquivo diz **por
quê**, e o que foi descartado no caminho. Cada entrada é curta e aponta para onde
está o detalhe. Uma decisão que mudou não é apagada: ganha uma entrada nova, que
diz o que substituiu.

Para acrescentar: data, a decisão, o motivo, o que se descartou e onde está o
detalhe. Datas de 2026. As anteriores ao TCC_V3 (o harness antigo e a v2) estão
em `docs/briefings/decisoes-do-harness.md` e no histórico do git.

---

## Desenho do experimento

**24/09: pares simultâneos.** Cada execução com harness roda no mesmo instante
que a sem harness, do mesmo modelo.
*Por quê:* horário, fila e carga do servidor ficam iguais, e a comparação é
dentro do par. *Onde:* `infra/scripts/rodada.sh`, OBJETIVO §2.

**24/09: o enunciado ganha P4 (clube) e P5 (controle negativo).**
*Por quê:* o piloto bateu no teto: com três pontos simétricos, Opus e Sonnet
acertaram tudo nos dois braços, e não sobrava espaço para o harness. O P4 tem um
caso assimétrico (o OURO), o P5 é um ponto onde usar o padrão é errado.
*Onde:* `experiment/prompt/README.md`.

**26/09: um experimento só; o piloto fica fora da análise.**
*Por quê:* o estímulo do piloto é outro (três pontos), e as execuções não são
comparáveis. Ele fica como o motivo de P4 e P5. *Onde:* `experiment/prompt/README.md`.

**06/10: o TCC_V3 é a bancada; o experimento que vale roda no V4.**
*Por quê:* os lotes daqui (inclusive o `EXT`) serviram para achar defeitos de
bancada, de enunciado e de instrumento; misturá-los com o lote que vale
contaminaria o pré-registro. *Onde:* OBJETIVO §2.

**06/10: a escada de níveis N0 a N3.** N0 sem harness; N1 o `CLAUDE.md`; N2 o N1
mais uma skill; N3 o N2 mais processo (desenho antes do código e revisor).
*Por quê:* a proposta do orientador (12/09); cada nível acumula o anterior, para
cada degrau ser comparado com o de baixo. *Descartado:* a verificação automática
(build e testes rodando sozinhos), porque as 62 execuções da bancada já rodavam o
Maven por conta própria (o Haiku, 20 de 20); um sensor que visse mais teria de usar
a suíte ou as métricas, e o nível seria corrigido pelo gabarito. Ela foi para o
fim, como N4, e o processo subiu para N3, para os níveis existentes ficarem
contíguos. *Onde:* `experiment/harnesses/README.md`.

**06/10: a skill do N2 é pública e intacta (`gof-patterns`).**
*Por quê:* as skills escritas aqui para a tarefa repetiam o vocabulário do
enunciado ("entra um caso novo toda semana"), o que entregaria a resposta.
*Descartado:* skills próprias. *Onde:* `experiment/harnesses/README.md`,
`experiment/third-party/gof-patterns/ORIGEM.md`.

**06/10: no V4, os quatro níveis rodam juntos, em quartetos, com 5 réplicas.**
*Por quê:* todos os níveis se comparam em pares sem repetir o N0 por lote, e a
tendência de N0 a N3 fica possível. *Descartado:* um lote por nível (90 execuções
em vez de 60). *Onde:* `PLANO-IMPLEMENTACAO.md`, Parte 6.

**06/10: o custo é todo secundário.**
*Por quê:* orientação de 12/09; a pergunta do TCC é sobre desenho, e no piloto o
sinal do custo trocou entre os modelos. O custo é medido e publicado, mas não
sustenta conclusão. *Onde:* OBJETIVO §4.

**07/10: o V4 fica só com o Strategy; o State vai para `history/state/`.**
*Por quê:* o State entrou para provar que a bancada aceita um segundo padrão, e
provou (enunciado, suíte e agregação funcionam); a régua dele nunca foi calibrada.
Com um padrão, o V4 cai de 120 para 60 execuções. *Onde:* `history/state/README.md`.

## Enunciado

**06/10: o contrato técnico (API, erros e exemplos) fica no enunciado.**
*Por quê:* é o que permite a mesma suíte de caixa-preta em todas as execuções.
Sem ele, cada agente inventa a sua API: um teste com um "usuário comum", sem
contrato, mostrou os modelos divergindo no formato da API, o que é outra pergunta
e deixaria a correção para ser lida à mão. É igual em todos os níveis, então não
distorce a comparação; o custo é de realismo, declarado.
*Descartado:* tirar o contrato; validar a correção com uma IA lendo o código (é
opinião, varia entre leituras e seria o Claude corrigindo o Claude).

**06/10: o cliente "entende o básico" e montou a parte técnica pesquisando.**
*Por quê:* antes a história tinha três autores (cliente, "desenvolvedor do site",
"time técnico"), e um cliente leigo dificilmente entregaria um contrato.
*Onde:* `experiment/prompt/README.md`.

**06/10: os exemplos não viram BDD (Gherkin).**
*Por quê:* BDD é uma instrução de processo ("comece pelos cenários") e chegaria a
todos os níveis, esbarrando no N3, que é o nível de processo; tende a levar parte
dos agentes ao Cucumber, variação que não vem do harness; e deixa o gabarito ainda
mais explícito. Os exemplos já são cenários, em português simples.

**06/10: as três inconsistências do Strategy foram corrigidas no V4** (exemplos 1
a 4 sem clube nem região, a resposta do anexo, o limite do boleto). O exemplo 2
fica em Centro-Oeste para a parcela não cair perto de um empate de arredondamento.
*Onde:* `experiment/prompt/README.md`, OBJETIVO §6.

**07/10: o imposto por região (P5) vira seguro por região.**
*Por quê:* imposto somado no checkout não existe no Brasil. O P5 precisa só da
forma (cinco regiões, só a porcentagem muda), e o seguro a mantém: é a forma que,
no imposto, já pegou um exagero na bancada. *Descartado:* embalagem para presente
com preço fixo (parece tabela de preço, e o controle negativo arriscava ficar no
piso, sem ninguém exagerando); trocar o cenário inteiro (recomeçar a preparação).
*Onde:* `experiment/prompt/README.md`.

## Correção (a suíte de aceitação)

**03/10: suíte de caixa-preta pela API, e a unidade é o caso.**
*Por quê:* cada execução tem classes diferentes, mas o contrato é o mesmo; contar
campos dava denominadores diferentes e pesava um erro pelo número de campos que ele
contamina. *Onde:* `evaluation/acceptance-prototype/README.md`.

**03/10: mutantes provam que a suíte reprova o errado; toda fronteira tem caso no
valor exato.** *Por quê:* quatro mutantes passavam pela suíte de 30/09, todos em
fronteiras ("até 5 kg", "passa de R$ 1.000"...). *Onde:* o mesmo README.

**03/10 e 07/10: como verificar a calculadora de referência.** Começou como
"escrever o serviço de referência do zero"; em 03/10 virou uma conferência à mão;
em 07/10, duas frentes: a **aritmética** por implementações independentes (as dos
agentes, que chegam aos mesmos números) e as **leituras do enunciado** revisadas
pelo Lucas. *Por quê:* refazer a conta à mão repete o que as implementações
independentes já verificam; o risco que sobra é de leitura, e só o autor decide
o que o texto quer dizer. *Onde:* o mesmo README, "Como se sabe que a suíte mede
certo". A troca vai ao orientador.

**07/10: as cinco leituras da calculadora ficam.** Revisadas pelo Lucas, sem
divergência: o OURO com FRETEGRATIS é aceito sem erro, o brinde conta os produtos
antes do cupom, o LEVE3PAGUE2 conta por item, sem juros o final é o total, e na
Price só a parcela é arredondada. *Onde:* o mesmo README, seção 4.

## Leitura de desenho e métricas

**27/09: a régua em três níveis** (propriedades, ficha do padrão, gabarito do
enunciado), com evidência `arquivo:linha` obrigatória e a forma registrada como
descrição, não veredito. *Por quê:* duas pessoas aplicam e chegam ao mesmo
resultado, e um padrão novo só escreve o terceiro nível. *Onde:* `evaluation/regua.md`.

**06/10: métricas automáticas (SonarQube e CK) só como secundárias, com versões
travadas.** O CK foi compilado de um commit novo porque a versão publicada ignora
`record`s. *Por quê:* medem tamanho e complexidade, não desenho; servem para
conferir as hipóteses. *Onde:* `evaluation/tools/README.md`, OBJETIVO §4.9.

**07/10: o Jev (TypeSafe AI) não entra no estudo.**
*Por quê:* a documentação oficial diz que ele não é calculadora (fora da correção)
e que erra mais com raciocínio de vários passos, estado grande e texto que
"argumenta pela própria classificação", que é o que a régua pede e o que os
comentários dos agentes fazem. Pior: um leitor levado por nomes de classe daria nota
maior aos níveis com a skill de padrões, e o viés andaria junto com o tratamento.
*Ficou como ideia futura:* um nível de sensor de desenho no harness, com perguntas
pequenas sobre cada trecho editado.

**07/10: o `ler-cego.sh` fica para depois.** Ele só é preciso antes da leitura do
V4, não para rodar. *Onde:* `PLANO-IMPLEMENTACAO.md`, Parte 4.

## Regras de leitura das hipóteses (OBJETIVO §4.1)

**05/10: hipóteses com nomes ("Eixo: afirmação") no lugar de códigos, e ★ para as
principais.** *Por quê:* legibilidade para quem lê o OBJETIVO. *Onde:* OBJETIVO
§4 e §7 (a correspondência com os códigos antigos).

**05/10: a regra do teto, os pares não empatados na medida sim/não, e o saldo na
hipótese do modelo.** *Por quê:* um modelo que já acerta tudo não tem como mostrar
melhora; numa medida sim/não quase todos os pares empatam. *Onde:* OBJETIVO §4.1.

**06/10: regras para 15 pares.** As principais comparam N1 com N0; a tendência
de N0 a N3 (teste de Page) e o tamanho do efeito (saldo / n) são informação, não
critério. *Por quê:* com 5 pares por modelo nenhum teste por modelo chega a 5%, e a
leitura precisa ser decidida antes dos dados. *Onde:* OBJETIVO §4.1.

**07/10: o "não piora" olha o saldo (piores − melhores), não só os piores.**
*Por quê:* sem efeito nenhum, alguns pares saem diferentes por acaso para os dois
lados; com 3 melhores e 3 piores, contar só os piores declararia que o harness
piora. *Onde:* OBJETIVO §4.1.

**07/10: o "altera", em medida contínua, exige 12 de 15, e 10 ou 11 é inconclusivo.**
*Por quê:* 12 é o menor número cuja chance de sair por sorte fica abaixo de 5%
(3,5%); 11 já tem 11,8%. Abaixo de 12, não chegar à prova não é provar o
contrário: com 10 ou 11 há indício, e só os pares divididos (9 a 6 ou mais
equilibrado) contrariam. *Onde:* OBJETIVO §4.1, com a tabela das chances.

**07/10: o "altera", em medida sim/não, exige 5 pares não empatados, todos do
mesmo lado; 3 a 0 ou 4 a 0 é inconclusivo.** *Por quê:* com 4 pares, a chance de
sorte era 12,5%; com 5, 6,3%; com 6, 3,1%. O rigoroso seria 6, mas o exagero é
raro e poucos pares devem diferir: a hipótese do exagero, principal, ficaria
inconclusiva quase sempre. *Descartado:* 4 (fraco demais) e 6 (quase impossível de
alcançar). *Onde:* OBJETIVO §4.1.

## Organização

**05/10: as pastas em inglês.** *Por quê:* preferência do autor. O caminho dentro
do container (`/experimento/prompt.md`) ficou, para o ambiente das execuções não
mudar. *Onde:* `README.md`.
