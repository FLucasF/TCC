package br.com.pousadamarealta.api.controller;

import br.com.pousadamarealta.api.dto.CreateReservationRequest;
import br.com.pousadamarealta.api.dto.DocumentResponse;
import br.com.pousadamarealta.api.dto.ReservationResponse;
import br.com.pousadamarealta.domain.exception.*;
import br.com.pousadamarealta.domain.model.RatePlanType;
import br.com.pousadamarealta.domain.model.Reservation;
import br.com.pousadamarealta.domain.model.Room;
import br.com.pousadamarealta.domain.rateplan.RatePlan;
import br.com.pousadamarealta.domain.repository.MessageRepository;
import br.com.pousadamarealta.domain.repository.ReservationRepository;
import br.com.pousadamarealta.domain.repository.RoomRepository;
import br.com.pousadamarealta.domain.service.DocumentService;
import br.com.pousadamarealta.domain.service.MessageService;
import br.com.pousadamarealta.domain.service.ReservationService;
import br.com.pousadamarealta.infrastructure.database.DatabaseConfig;
import io.javalin.http.Context;

import java.sql.Connection;

public class ReservationController {
    private final DatabaseConfig databaseConfig;
    private final DocumentService documentService;

    public ReservationController(DatabaseConfig databaseConfig, DocumentService documentService) {
        this.databaseConfig = databaseConfig;
        this.documentService = documentService;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        RatePlanType ratePlanType;
        try {
            ratePlanType = RatePlanType.valueOf(request.ratePlan());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Plano de tarifa inválido");
        }

        RatePlan.ReservationRequest ratePlanRequest = new RatePlan.ReservationRequest(
            request.roomNumber(),
            request.guestName(),
            request.guestEmail(),
            request.guestCpf(),
            request.checkIn(),
            request.checkOut(),
            request.guests(),
            request.companyCode()
        );

        try (Connection connection = databaseConfig.getConnection()) {
            ReservationService reservationService = createReservationService(connection);
            Reservation reservation = reservationService.createReservation(ratePlanRequest, ratePlanType);
            ctx.status(201).json(ReservationResponse.from(reservation));
        } catch (InvalidRequestException | RoomNotFoundException |
                 RoomUnavailableException | CapacityExceededException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar requisição", e);
        }
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.roomNumber().isBlank()) {
            throw new InvalidRequestException("Número do quarto é obrigatório");
        }
        if (request.guestName() == null || request.guestName().isBlank()) {
            throw new InvalidRequestException("Nome do hóspede é obrigatório");
        }
        if (request.guestEmail() == null || request.guestEmail().isBlank()) {
            throw new InvalidRequestException("Email do hóspede é obrigatório");
        }
        if (request.guestCpf() == null || request.guestCpf().isBlank()) {
            throw new InvalidRequestException("CPF do hóspede é obrigatório");
        }
        if (request.checkIn() == null) {
            throw new InvalidRequestException("Data de entrada é obrigatória");
        }
        if (request.checkOut() == null) {
            throw new InvalidRequestException("Data de saída é obrigatória");
        }
        if (request.ratePlan() == null || request.ratePlan().isBlank()) {
            throw new InvalidRequestException("Plano de tarifa é obrigatório");
        }
    }

    public void getReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try (Connection connection = databaseConfig.getConnection()) {
            ReservationService reservationService = createReservationService(connection);
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar requisição", e);
        }
    }

    public void confirmReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try (Connection connection = databaseConfig.getConnection()) {
            ReservationService reservationService = createReservationService(connection);
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationNotFoundException | OperationNotAllowedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar requisição", e);
        }
    }

    public void checkIn(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try (Connection connection = databaseConfig.getConnection()) {
            ReservationService reservationService = createReservationService(connection);
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationNotFoundException | OperationNotAllowedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar requisição", e);
        }
    }

    public void checkOut(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try (Connection connection = databaseConfig.getConnection()) {
            ReservationService reservationService = createReservationService(connection);
            RoomRepository roomRepository = new RoomRepository(connection);
            Reservation reservation = reservationService.checkOut(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            ctx.json(DocumentResponse.from(reservation, room));
        } catch (ReservationNotFoundException | OperationNotAllowedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar requisição", e);
        }
    }

    public void cancelReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try (Connection connection = databaseConfig.getConnection()) {
            ReservationService reservationService = createReservationService(connection);
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationNotFoundException | OperationNotAllowedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar requisição", e);
        }
    }

    public void getDocument(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try (Connection connection = databaseConfig.getConnection()) {
            ReservationService reservationService = createReservationService(connection);
            RoomRepository roomRepository = new RoomRepository(connection);
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getAmountPaid().compareTo(java.math.BigDecimal.ZERO) == 0) {
                throw new OperationNotAllowedException("Documento só disponível para reservas pagas");
            }

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            ctx.json(DocumentResponse.from(reservation, room));
        } catch (ReservationNotFoundException | OperationNotAllowedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar requisição", e);
        }
    }

    private ReservationService createReservationService(Connection connection) {
        ReservationRepository reservationRepository = new ReservationRepository(connection);
        RoomRepository roomRepository = new RoomRepository(connection);
        MessageRepository messageRepository = new MessageRepository(connection);
        MessageService messageService = new MessageService(messageRepository);
        return new ReservationService(reservationRepository, roomRepository, messageService, documentService);
    }
}
