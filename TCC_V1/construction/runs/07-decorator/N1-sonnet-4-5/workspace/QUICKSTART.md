# Guia Rápido - Pousada Maré Alta

## Início Rápido (3 passos)

### 1. Build do projeto

```bash
mvn clean package
```

### 2. Inicie o PostgreSQL

```bash
docker-compose up -d
```

Aguarde alguns segundos para o PostgreSQL iniciar.

### 3. Execute o sistema

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=pousada
export DB_PASSWORD=pousada123
java -jar target/reservas-1.0.0.jar
```

O sistema estará rodando em http://localhost:7070

## Testando a API

Em outro terminal:

```bash
./test-api.sh
```

Ou teste manualmente:

```bash
# Listar quartos
curl http://localhost:7070/rooms | jq .

# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .

# Confirmar reserva (substitua {id} pelo ID retornado)
curl -X POST http://localhost:7070/reservations/1/confirmation | jq .

# Ver mensagens enviadas
curl http://localhost:7070/messages | jq .
```

## Modo de Teste

Para ativar o modo de teste (todos os emails vão para testes@pousadamarealta.com.br):

```bash
export HOTEL_TEST_MODE=true
java -jar target/reservas-1.0.0.jar
```

## Parando o sistema

1. Pressione Ctrl+C no terminal do Java
2. Pare o PostgreSQL: `docker-compose down`

## Rodando os testes

```bash
# Testes unitários (não precisam de banco)
mvn test

# Testes de integração (precisam de PostgreSQL)
docker-compose up -d
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=pousada
export DB_PASSWORD=pousada123
mvn verify
```

## Arquitetura do Sistema

O sistema foi desenvolvido seguindo as orientações do CLAUDE.md:

- **Adicionar novos casos é simples**: Para adicionar uma nova regra de mensagem, basta criar uma classe que implementa `MessageRule` e registrá-la no `MessageProcessor`
- **Sem repetição**: A sequência de processamento de mensagens está definida uma vez só, e cada regra muda apenas o que é dela
- **Organização para variação futura**: As regras de mensagens estão preparadas para receber novas regras (limite diário, horários permitidos, etc) sem mexer no código existente

### Exemplo: Adicionando uma nova regra

```java
public class BusinessHoursRule implements MessageRule {
    @Override
    public void apply(ProcessedMessage message) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(8, 0)) || now.isAfter(LocalTime.of(22, 0))) {
            message.setStatus(MessageStatus.BLOCKED);
        }
    }
}

// Em App.java, adicione ao MessageProcessor:
rules.add(new BusinessHoursRule());
```

Pronto! A nova regra está integrada sem modificar nenhuma outra parte do código.
