package br.com.pousadamarealta.api.controller;

import br.com.pousadamarealta.domain.exception.InvalidRequestException;
import br.com.pousadamarealta.domain.repository.ReservationRepository;
import br.com.pousadamarealta.domain.repository.RoomRepository;
import br.com.pousadamarealta.domain.service.ReportService;
import br.com.pousadamarealta.infrastructure.database.DatabaseConfig;
import io.javalin.http.Context;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final DatabaseConfig databaseConfig;

    public ReportController(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null || dateParam.isBlank()) {
            throw new InvalidRequestException("Parâmetro 'date' é obrigatório");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateParam, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Formato de data inválido. Use yyyy-MM-dd");
        }

        try (Connection connection = databaseConfig.getConnection()) {
            ReservationRepository reservationRepository = new ReservationRepository(connection);
            RoomRepository roomRepository = new RoomRepository(connection);
            ReportService reportService = new ReportService(reservationRepository, roomRepository);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar requisição", e);
        }
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isBlank()) {
            throw new InvalidRequestException("Parâmetro 'month' é obrigatório");
        }

        YearMonth month;
        try {
            month = YearMonth.parse(monthParam, MONTH_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Formato de mês inválido. Use yyyy-MM");
        }

        try (Connection connection = databaseConfig.getConnection()) {
            ReservationRepository reservationRepository = new ReservationRepository(connection);
            RoomRepository roomRepository = new RoomRepository(connection);
            ReportService reportService = new ReportService(reservationRepository, roomRepository);
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar requisição", e);
        }
    }
}
