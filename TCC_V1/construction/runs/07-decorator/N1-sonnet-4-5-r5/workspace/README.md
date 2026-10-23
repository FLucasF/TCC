# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Requisitos

- Java 21
- Maven 3.8+
- PostgreSQL 18 (ou via variável de ambiente)

## Compilação e execução

### Com Maven

```bash
# Compilar e gerar JAR executável
mvn clean package

# Executar testes
mvn verify

# Executar aplicação (requer PostgreSQL rodando)
java -jar target/marealta-1.0.0.jar
```

### Com Docker Compose (recomendado)

```bash
# Iniciar aplicação e banco de dados
docker-compose up

# Parar aplicação
docker-compose down
```

## Configuração

O sistema usa variáveis de ambiente para configuração:

- `DB_URL`: URL JDBC do PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco (padrão: `postgres`)
- `PORT`: Porta HTTP do servidor (padrão: `7070`)
- `HOTEL_TEST_MODE`: Ativar modo de teste (`true`/`false`, padrão: `false`)

## Arquitetura do sistema de mensagens

O sistema implementa um processador de mensagens baseado em regras independentes, seguindo os princípios do arquivo CLAUDE.md:

### Regras implementadas

1. **EmailValidationRule**: Valida formato de e-mail (bloqueia se inválido)
2. **TestModeRule**: Redireciona para `testes@pousadamarealta.com.br` em modo de teste
3. **ArchiveCopyRule**: Gera cópia para `arquivo@pousadamarealta.com.br` em produção

### Adicionando novas regras

Para adicionar uma nova regra de processamento de mensagens:

1. Criar classe implementando `MessageRule`
2. Implementar método `process(OutgoingMessage message)`
3. Adicionar a regra no método `createMessageProcessor` em `App.java`

Exemplo:
```java
public class MinhaNovaRegra implements MessageRule {
    @Override
    public ProcessingResult process(OutgoingMessage message) {
        // Sua lógica aqui
        return new ProcessingResult();
    }
}
```

Cada regra pode:
- Modificar a mensagem (destinatário, assunto, corpo)
- Bloquear a mensagem (`message.block()`)
- Gerar mensagens adicionais (`result.addMessage(...)`)

A sequência de processamento é definida pela ordem das regras no `MessageProcessor`.

## API REST

### Quartos

- `GET /rooms`: Lista todos os quartos

### Reservas

- `POST /reservations`: Criar reserva
- `GET /reservations/{id}`: Buscar reserva
- `POST /reservations/{id}/confirmation`: Confirmar reserva (envia e-mail)
- `POST /reservations/{id}/cancellation`: Cancelar reserva
- `POST /reservations/{id}/check-in`: Fazer check-in
- `POST /reservations/{id}/check-out`: Fazer check-out (retorna comprovante)
- `GET /reservations/{id}/document`: Segunda via do comprovante

### Mensagens

- `GET /messages`: Lista todas as mensagens enviadas/bloqueadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD`: Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM`: Relatório financeiro (CSV)

## Estrutura do projeto

```
src/main/java/com/pousada/
├── domain/           # Entidades de domínio
├── repository/       # Acesso a dados
├── service/          # Lógica de negócio
│   └── messaging/    # Sistema de regras de mensagens
├── api/              # Controllers REST
├── config/           # Configuração (Database)
└── App.java          # Aplicação principal
```

## Licença

Propriedade da Pousada Maré Alta.
