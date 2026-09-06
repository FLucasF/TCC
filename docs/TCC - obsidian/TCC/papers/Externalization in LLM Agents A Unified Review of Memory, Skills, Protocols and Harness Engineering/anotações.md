-  classes principais de sobrecarga que o sistema absorve:
	- **memory**:
		- externalizam o estado ao longo do tempo, de modo que a continuidade não dependa mais de um contexto efêmero
		- aborda a continuidade ao longo do tempo
	- **skill**:
		- externalizam a expertise procedimental, de modo que fluxos de trabalho complexos sejam carregados em vez de reinventado
		- abordam a consistência do procedimento
	- **protocols**: 
		- externalizam a estrutura de interação, de modo que a coordenação entre ferramenta e agente siga contratos definidos em vez de instruções ad hoc.
		- abordam a estrutura da interação.
- a “engenharia de agentes” assume cada vez mais a forma de “engenharia de sistemas”. O modelo permanece como o principal motor de raciocínio, mas não é mais a única fonte de inteligência. A capacidade é distribuída pelas estruturas que moldam o que o modelo vê, lembra, invoca e tem permissão para fazer.
- Os LLMs são fortes na síntese flexível e no raciocínio sobre as informações fornecidas; eles são menos confiáveis ​​na memória estável de longo prazo, na repetibilidade procedimental e na interação governada com sistemas externos

## Memória:
### O que é externalizado: o conteúdo do Estado
- O contexto bruto da janela de contexto efêmera e o feedback do ambiente são convertidos em quatro dimensões de memória persistente: contexto de trabalho, experiência episódica, conhecimento semântico e memória personalizada.
- A essência da memória reside em desacoplar o estado do agente ao longo do tempo de seu contexto transitório
- quatro dimensões do estado externalizado:
	- **Contexto de trabalho**: é o estado intermediário em tempo real da tarefa atual: arquivos abertos, variáveis ​​temporárias, hipóteses ativas, planos parciais e pontos de verificação de execução. Ele muda rapidamente e perde valor se estiver desatualizado, mas sem externalização, desaparece assim que a janela de contexto é reiniciada ou um processo é interrompido.
	- **Experiência episódica**: registra o que aconteceu em execuções anteriores: pontos de decisão, chamadas de ferramentas, falhas, resultados e reflexões. Episódios recuperados podem servir como precedentes concretos, ajudar o agente a evitar a repetição de erros conhecidos e fornecer matéria-prima para abstrações posteriores.
	- **Conhecimento semântico**: armazena abstrações que sobrevivem a qualquer episódio individual: fatos do domínio, heurísticas gerais, convenções do projeto e conhecimento estável do mundo 
	- - **Memória personalizada**: armazena informações estáveis ​​sobre usuários, equipes ou ambientes específicos: preferências, hábitos, restrições recorrentes e interações anteriores. Esse estado não deve ser consolidado no armazenamento geral de autoaperfeiçoamento do agente, pois os registros específicos do usuário obedecem a regras diferentes de retenção, recuperação e privacidade.
- ***OBS.:*** *Essas quatro camadas não esgotam tudo o que pode vir a ser útil ao agente. Regularidades procedimentais repetidas podem aparecer inicialmente como padrões em registros episódicos, mas deixam de ser memória propriamente dita assim que o sistema as transforma em orientações explícitas e reutilizáveis. Nesse ponto, elas pertencem à camada de habilidade, e não à camada de memória.*
- O contexto de trabalho permite a retomada imediata, os registros episódicos permitem a reflexão e a recuperação, a memória semântica permite a abstração e a transferência, e a memória personalizada permite a adaptação entre sessões a usuários e ambientes

> [!NOTE]
> - A memória episódica descreve o que aconteceu em um caso específico; a memória semântica descreve o que tende a se manter consistente entre os casos
> 
> Estudar
> - **AriGraph**
> - **IFRAgent**

### Como é externalizado: Arquiteturas de memória
- Os sistemas atuais podem ser classificados em quatro grandes paradigmas arquitetônicos: Contexto Monolítico, Contexto com Armazenamento de Recuperação, Memória Hierárquica e Orquestração, e Sistemas de Memória Adaptativa.
- A evolução não se limita a armazenamentos maiores, mas sim a políticas mais explícitas sobre o que é escrito, promovido, recuperado, compactado ou descartado.
- **Contexto Monolítico**:
- **Contexto com armazenamento de recuperação**:
> [!NOTE]
> Estudar:
> - **GraphRAG**
> - **ENGRAM**
> - **SYNAPSE**

- **Memória Hierárquica e Orquestração**:
> [!NOTE]
> Estudar:
> - **Mem0**
> - **Memória-R1**
> - **Mem-α**
> - Desacoplamento de recursos em dimensões espaço-temporais:
> 	- **MemGPT**
> 	- **MemoryOS**
> - Desacoplamento semântico em dimensões funcionais cognitivas:
> 	- **MemoryBank**
> 	- **MIRIX**
> 	- **MemOS**
> 	- **xMemory**

- **Sistemas de memória adaptativa**:
> [!NOTE]
> Estudar:
> - Módulos dinâmicos
> 	- **MemEvolve**
> 	- **MemVerse**
> - Otimização de estratégia baseada em feedback
> 	- **MemRL** 
> 	- **GAM** 

 O contexto monolítico resolve a questão da existência, os armazenamentos de recuperação resolvem a questão da capacidade, os sistemas hierárquicos resolvem a questão da organização e os sistemas adaptativos começam a resolver a questão das políticas. A memória, portanto, deixa de ser um apêndice passivo para o estímulo. Em agentes maduros, ela se torna parte da superfície de controle que determina sobre qual passado o modelo pode efetivamente agir.
### Demandas de memória da era dos arreios
> [!NOTE]
> Estudar:
> - **InfiAgent**

### Memória como Artefato Cognitivo
- Os LLMs modernos são geradores sem estado: cada chamada começa com um novo contexto, portanto a continuidade deve ser reconstruída em vez de simplesmente mantida.

- A memória não é simplesmente uma conveniência de engenharia para expandir o contexto efetivo. É um artefato cognitivo que remodela a carga temporal da ação. Ao converter a recordação ilimitada em recuperação limitada e selecionada, ela altera a tarefa que o modelo enfrenta a cada ponto de decisão.
- Objetivo de projeto subjacente: tornar o histórico correto legível no momento certo, para que a capacidade inferencial fixa do modelo seja gasta em raciocínio em vez de memorização.

## Observações gerais

- **Ad hoc** é uma expressão em latim que significa literalmente "para isto" ou "para esta finalidade", indicando algo criado, feito ou designado de forma temporária para resolver um problema ou atender a uma necessidade específica.