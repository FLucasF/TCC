package br.com.pousada.marealta.controller;

import br.com.pousada.marealta.exception.AppException;
import br.com.pousada.marealta.model.*;
import br.com.pousada.marealta.repository.ReservationRepository;
import br.com.pousada.marealta.repository.RoomRepository;
import br.com.pousada.marealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReservationController(ReservationService reservationService,
                                ReservationRepository reservationRepository,
                                RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public void createReservation(Context ctx) {
        Map<String, Object> body = ctx.bodyAsClass(Map.class);

        validateRequiredFields(body, "roomNumber", "guestName", "guestEmail", "checkIn", "checkOut", "guests", "ratePlan");

        String roomNumber = (String) body.get("roomNumber");
        String guestName = (String) body.get("guestName");
        String guestEmail = (String) body.get("guestEmail");
        LocalDate checkIn = parseDate((String) body.get("checkIn"));
        LocalDate checkOut = parseDate((String) body.get("checkOut"));
        int guests = getIntValue(body, "guests");
        RatePlan ratePlan = parseRatePlan((String) body.get("ratePlan"));

        if (!checkIn.isBefore(checkOut)) {
            throw AppException.invalidRequest("Data de saída deve ser posterior à data de entrada");
        }

        GuestIdentification identification = parseIdentification(body);

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setIdentification(identification);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);

        Reservation created = reservationService.createReservation(reservation);
        ctx.status(201).json(toReservationDto(created));
    }

    public void getReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(AppException::reservationNotFound);
        ctx.json(toReservationDto(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(toReservationDto(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(toReservationDto(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toReservationDto(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkOut(id);
        Document document = reservationService.getDocument(id);
        ctx.json(toDocumentDto(document));
    }

    public void getDocument(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Document document = reservationService.getDocument(id);
        ctx.json(toDocumentDto(document));
    }

    private void validateRequiredFields(Map<String, Object> body, String... fields) {
        for (String field : fields) {
            if (!body.containsKey(field) || body.get(field) == null) {
                throw AppException.invalidRequest("Campo obrigatório ausente: " + field);
            }
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw AppException.invalidRequest("Formato de data inválido: " + dateStr);
        }
    }

    private int getIntValue(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw AppException.invalidRequest("Valor inválido para " + field);
    }

    private RatePlan parseRatePlan(String ratePlanStr) {
        try {
            return RatePlan.valueOf(ratePlanStr);
        } catch (IllegalArgumentException e) {
            throw AppException.invalidRequest("Plano de tarifa inválido: " + ratePlanStr);
        }
    }

    private GuestIdentification parseIdentification(Map<String, Object> body) {
        String cpf = (String) body.get("guestCpf");
        String cnpj = (String) body.get("companyCnpj");
        String companyName = (String) body.get("companyName");
        String passport = (String) body.get("guestPassport");
        String country = (String) body.get("guestCountry");

        int typeCount = 0;
        if (cpf != null) typeCount++;
        if (cnpj != null) typeCount++;
        if (passport != null) typeCount++;

        if (typeCount != 1) {
            throw AppException.invalidRequest("Deve haver exatamente um tipo de identificação");
        }

        if (cpf != null) {
            return new GuestIdentification.Individual(cpf);
        } else if (cnpj != null) {
            if (companyName == null) {
                throw AppException.invalidRequest("companyName é obrigatório quando companyCnpj é informado");
            }
            return new GuestIdentification.Company(cnpj, companyName);
        } else {
            if (country == null) {
                throw AppException.invalidRequest("guestCountry é obrigatório quando guestPassport é informado");
            }
            return new GuestIdentification.Foreign(passport, country);
        }
    }

    private Map<String, Object> toReservationDto(Reservation reservation) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", reservation.getId());
        dto.put("roomNumber", reservation.getRoomNumber());
        dto.put("guestName", reservation.getGuestName());
        dto.put("guestEmail", reservation.getGuestEmail());

        GuestIdentification identification = reservation.getIdentification();
        if (identification instanceof GuestIdentification.Individual individual) {
            dto.put("guestCpf", individual.cpf());
            dto.put("companyCnpj", null);
            dto.put("companyName", null);
            dto.put("guestPassport", null);
            dto.put("guestCountry", null);
        } else if (identification instanceof GuestIdentification.Company company) {
            dto.put("guestCpf", null);
            dto.put("companyCnpj", company.cnpj());
            dto.put("companyName", company.companyName());
            dto.put("guestPassport", null);
            dto.put("guestCountry", null);
        } else if (identification instanceof GuestIdentification.Foreign foreign) {
            dto.put("guestCpf", null);
            dto.put("companyCnpj", null);
            dto.put("companyName", null);
            dto.put("guestPassport", foreign.passport());
            dto.put("guestCountry", foreign.country());
        }

        dto.put("checkIn", reservation.getCheckIn().toString());
        dto.put("checkOut", reservation.getCheckOut().toString());
        dto.put("guests", reservation.getGuests());
        dto.put("ratePlan", reservation.getRatePlan().name());
        dto.put("nights", reservation.getNights());
        dto.put("subtotal", reservation.getSubtotal());
        dto.put("discount", reservation.getDiscount());
        dto.put("total", reservation.getTotal());
        dto.put("status", reservation.getStatus().name());
        dto.put("amountPaid", reservation.getAmountPaid());
        dto.put("refund", reservation.getRefund());

        return dto;
    }

    private Map<String, Object> toDocumentDto(Document document) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("title", document.getTitle());
        dto.put("reservationId", document.getReservationId());
        dto.put("guestName", document.getGuestName());
        dto.put("guestCpf", document.getGuestCpf());
        dto.put("companyCnpj", document.getCompanyCnpj());
        dto.put("companyName", document.getCompanyName());
        dto.put("guestPassport", document.getGuestPassport());
        dto.put("guestCountry", document.getGuestCountry());
        dto.put("roomNumber", document.getRoomNumber());
        dto.put("roomType", document.getRoomType().name());
        dto.put("checkIn", document.getCheckIn().toString());
        dto.put("checkOut", document.getCheckOut().toString());
        dto.put("nights", document.getNights());
        dto.put("subtotal", document.getSubtotal());
        dto.put("discount", document.getDiscount());
        dto.put("total", document.getTotal());
        dto.put("text", document.getText());
        return dto;
    }
}
