# Quick Start - Pousada Maré Alta

## 1. Pré-requisitos

- Java 21+
- Maven 3.8+
- PostgreSQL 18+

## 2. Preparar Banco de Dados

```bash
# Criar banco de dados
psql -U postgres -c "CREATE DATABASE mare_alta;"

# Ou usar outro usuário:
psql -U seu_usuario -c "CREATE DATABASE mare_alta;"
```

## 3. Compilar o Projeto

```bash
cd /workspace
mvn clean package
```

Isto vai gerar o JAR em `target/mare-alta-reservas-1.0.0.jar`.

## 4. Executar a Aplicação

```bash
# Defina as variáveis de ambiente
export DB_URL="jdbc:postgresql://localhost:5432/mare_alta"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha"
export PORT=7070

# Execute
java -jar target/mare-alta-reservas-1.0.0.jar
```

Você verá:
```
[main] INFO io.javalin.Javalin - Starting Javalin ...
[main] INFO org.eclipse.jetty.server.Server - jetty-11.0.25...
```

A aplicação está pronta em `http://localhost:7070`.

## 5. Testar a API

### Listar Quartos

```bash
curl http://localhost:7070/rooms | jq
```

Resposta esperada:
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": "249.90"
  },
  ...
]
```

### Criar uma Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta esperada (id pode variar):
```json
{
  "id": 1,
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@email.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": "779.80",
  "discount": "0.00",
  "total": "779.80",
  "status": "PENDING",
  "amountPaid": "0.00",
  "refund": null
}
```

### Confirmar a Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation | jq
```

Resposta: status muda para `CONFIRMED`, amountPaid recebe o valor.

### Ver Mensagens Enviadas

```bash
curl http://localhost:7070/messages | jq
```

Você verá:
- Email para o hóspede (confirmação)
- Email para governança (entrada/saída)
- Email para financeiro (pagamento)

### Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in | jq
```

Status muda para `CHECKED_IN`.

### Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq
```

Retorna o comprovante, status muda para `CHECKED_OUT`.

### Obter Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document | jq
```

### Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Retorna CSV:
```
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

### Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

Retorna CSV:
```
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80
total,,,,,779.80
```

## 6. Rodar Testes

```bash
mvn verify
```

Executa testes de unidade para validar:
- Cálculo de preços
- Arredondamento
- Alta temporada
- Formatação de valores

## 7. Usar o Script de Teste

Um script completo está disponível:

```bash
chmod +x test.sh
./test.sh
```

(Requer aplicação rodando)

## 8. Adicionar um Novo Notificador

Para notificar o restaurante sobre confirmações:

1. Criar `src/main/java/com/pousada/marealta/notification/notifiers/RestaurantNotifier.java`:

```java
package com.pousada.marealta.notification.notifiers;

import com.pousada.marealta.notification.ReservationEvent;
import com.pousada.marealta.notification.ReservationNotifier;
import com.pousada.marealta.db.MessageRepository;
import com.pousada.marealta.enums.MessageChannel;
import com.pousada.marealta.model.Reservation;

public class RestaurantNotifier implements ReservationNotifier {
    private static final String RESTAURANT_EMAIL = "restaurante@pousadamarealta.com.br";

    @Override
    public void handleEvent(ReservationEvent event) throws Exception {
        if (event.getType() == ReservationEvent.Type.CONFIRMED) {
            Reservation reservation = event.getReservation();
            
            String subject = "Nova reserva - " + reservation.getGuests() + " hóspedes";
            String body = String.format(
                "Entrada: %s\nHóspedes: %d",
                reservation.getCheckIn(),
                reservation.getGuests()
            );
            
            MessageRepository.create(MessageChannel.EMAIL, RESTAURANT_EMAIL, subject, body);
        }
    }
}
```

2. Registrar em `App.initializeNotifiers()`:

```java
private static void initializeNotifiers() {
    NotificationManager.registerNotifier(new GuestNotifier());
    NotificationManager.registerNotifier(new HousekeepingNotifier());
    NotificationManager.registerNotifier(new FinanceNotifier());
    NotificationManager.registerNotifier(new RestaurantNotifier());  // Nova linha
}
```

3. Recompilar:

```bash
mvn clean package
```

## Troubleshooting

### Porta já está em uso
```bash
lsof -i :7070
kill -9 <PID>
```

### Erro de conexão com banco
Verifique as variáveis de ambiente:
```bash
echo $DB_URL
echo $DB_USER
```

Teste a conexão:
```bash
psql -h localhost -U postgres -d mare_alta -c "SELECT 1"
```

### Compilação falha
Verifique Java version:
```bash
java -version  # Deve ser 21+
```

Limpe o cache Maven:
```bash
mvn clean
```

## Próximos Passos

1. Implementar persistência de mensagens em arquivo ou email real
2. Adicionar autenticação para endpoints sensíveis
3. Adicionar validação mais robusta de CPF
4. Criar interface web para gerenciamento
5. Adicionar logs mais detalhados
6. Implementar cancelamento automático de reservas não confirmadas após X dias
