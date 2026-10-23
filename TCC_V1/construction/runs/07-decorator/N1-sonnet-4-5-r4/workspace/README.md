# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para pousadas, desenvolvido em Java 21 com Javalin.

## Requisitos

- Java 21
- Maven 3.8+
- PostgreSQL 18+

## Configuração

O sistema utiliza as seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta onde o servidor será iniciado (padrão: 7070)
- `HOTEL_TEST_MODE`: Define se o sistema está em modo de teste (`true` ou `false`)

## Compilação

```bash
mvn package
```

Isso gerará um JAR executável em `target/reservas-1.0-SNAPSHOT.jar`.

## Execução

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="usuario"
export DB_PASSWORD="senha"
export PORT=7070

java -jar target/reservas-1.0-SNAPSHOT.jar
```

## Testes

Execute os testes com:

```bash
mvn verify
```

Os testes de integração requerem que as variáveis de ambiente do banco estejam configuradas.

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos disponíveis

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

## Arquitetura

O sistema segue o padrão de camadas:

- **Model**: Entidades de domínio e enums
- **Repository**: Acesso a dados com PostgreSQL
- **Service**: Regras de negócio
- **Controller**: Endpoints da API REST
- **Messaging**: Sistema extensível de regras de mensagens

### Sistema de Mensagens

O sistema de mensagens utiliza o padrão Chain of Responsibility para aplicar regras:

- **EmailValidationRule**: Valida formato de e-mail
- **TestModeRule**: Redireciona mensagens em modo de teste
- **ArchiveCopyRule**: Cria cópia para arquivo

Novas regras podem ser adicionadas implementando a interface `MessageRule` e registrando-as no `MessageRulePipelineFactory`.

## Modo de Teste

Quando `HOTEL_TEST_MODE=true`:
- Todos os e-mails são redirecionados para `testes@pousadamarealta.com.br`
- O assunto recebe prefixo `[TESTE]`
- O corpo inclui linha com destinatário original
- Não gera cópia para arquivo

Quando em produção:
- E-mails são enviados normalmente
- Toda mensagem gera cópia para `arquivo@pousadamarealta.com.br`
- E-mails inválidos são bloqueados
