https://www.youtube.com/watch?v=aYfZN8t6AQs&t=118s

memoria conversacional:


memoria longo prazo:
- a cada turno do agente ele vai ver a memoria de longo prazo independente da memoria conversacional
- **men 0**
	- rep https://github.com/ourmem/omem
	- main memory
		- vector DB
		- memories
		- meta-date
			- date da memoria criada atualizada
			- date de expiração atribuido a quem criou, se tiver utilizando varios agents
			- se a memoria pertence a usuario ou agente
				- se for memoria do usuario colocar um hash ela também contera o hash da propria memoria, usado estritamente para desduplicação
	- entity memory
		- mem0 ira retirar entidades da memoria principal, lugares, pessoas, etc e amazenara aqui
		- vector DB
		- entidades
			- linked a uma ou mais memorias principais
	- SQLite
		- log
		- mantera as ultimas 10 mensagens enviadas ao pipeline 

ingestion

o input messages
	1. procedural memory
	2. infar = false, pegara todas as mensagem de entradas e encorporar no banco de dados
	3. infar = true, extrassão com pipe de llm (foco vai ser nesse)

infar = true o que acontece depois?
- carregar o contexto recente, extrassão feita com uma LLM,
- a LLM exportarar um json extruturado com as memorias estraidas, para fazer isso a llm tera um prompt e um contexto muito especifico sobre usuiario e assunto, para extrair algo relevante dessas mensagens
- prompt
	- inclui o papel da llm, destrator de memoria
	- resumo do usuário
	- messagens
	- memorias recentes que foram salvas no banco de dados para saber o que estamos falando
	- memorias relevantes, pesquisa no banco de dados para procurar memorias relevantes para essa mensagem, isso é feito achatando as mensagens, transofrmando em uma a uma a string criando um embbading dessa mensagem, procurando no banco memorias relacionadas a essa mensagem e essas mensagens relevantes também serao anexadas ao contexto e ele também incluira as ultimas em mensagens
	- last 10 mensagges
sempre que esse pipe for rodado, ele sempre salvara a mensagem no banco sql e mantera apenas as 10 mais recentes, isso é muito util para indenteificar e descobrir oq os pronomes significa nessa nova mensagem