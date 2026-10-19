package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservationResponse {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @JsonProperty("id")
    public Long id;

    @JsonProperty("roomNumber")
    public Integer roomNumber;

    @JsonProperty("guestName")
    public String guestName;

    @JsonProperty("guestEmail")
    public String guestEmail;

    @JsonProperty("guestCpf")
    public String guestCpf;

    @JsonProperty("companyCnpj")
    public String companyCnpj;

    @JsonProperty("companyName")
    public String companyName;

    @JsonProperty("guestPassport")
    public String guestPassport;

    @JsonProperty("guestCountry")
    public String guestCountry;

    @JsonProperty("checkIn")
    public String checkIn;

    @JsonProperty("checkOut")
    public String checkOut;

    @JsonProperty("guests")
    public Integer guests;

    @JsonProperty("ratePlan")
    public String ratePlan;

    @JsonProperty("nights")
    public Long nights;

    @JsonProperty("subtotal")
    public BigDecimal subtotal;

    @JsonProperty("discount")
    public BigDecimal discount;

    @JsonProperty("total")
    public BigDecimal total;

    @JsonProperty("status")
    public String status;

    @JsonProperty("amountPaid")
    public BigDecimal amountPaid;

    @JsonProperty("refund")
    public BigDecimal refund;

    public static ReservationResponse fromReservation(Reservation res) {
        ReservationResponse dto = new ReservationResponse();
        dto.id = res.getId();
        dto.roomNumber = res.getRoomNumber();
        dto.guestName = res.getGuestName();
        dto.guestEmail = res.getGuestEmail();
        dto.checkIn = res.getCheckIn().format(DATE_FORMAT);
        dto.checkOut = res.getCheckOut().format(DATE_FORMAT);
        dto.guests = res.getGuests();
        dto.ratePlan = res.getRatePlan().toString();
        dto.nights = res.getNights();
        dto.subtotal = res.getSubtotal();
        dto.discount = res.getDiscount();
        dto.total = res.getTotal();
        dto.status = res.getStatus().toString();
        dto.amountPaid = res.getAmountPaid();
        dto.refund = res.getRefund();

        // Set identification fields based on type
        com.pousada.domain.GuestIdentification id = res.getGuestIdentification();
        if (id instanceof com.pousada.domain.CpfIdentification cpfId) {
            dto.guestCpf = cpfId.guestCpf;
        } else if (id instanceof com.pousada.domain.CnpjIdentification cnpjId) {
            dto.companyCnpj = cnpjId.companyCnpj;
            dto.companyName = cnpjId.companyName;
        } else if (id instanceof com.pousada.domain.PassportIdentification passportId) {
            dto.guestPassport = passportId.guestPassport;
            dto.guestCountry = passportId.guestCountry;
        }

        return dto;
    }
}
