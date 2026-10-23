package br.com.maralta.dto;

import br.com.maralta.model.RatePlan;
import br.com.maralta.model.Reservation;
import br.com.maralta.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationResponse {
    private Long id;
    private String roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestCpf;
    private String companyCnpj;
    private String companyName;
    private String guestPassport;
    private String guestCountry;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests;
    private RatePlan ratePlan;
    private int nights;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public static ReservationResponse from(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.id = reservation.getId();
        response.roomNumber = reservation.getRoomNumber();
        response.guestName = reservation.getGuestName();
        response.guestEmail = reservation.getGuestEmail();

        if (reservation.getIdentification() != null) {
            response.guestCpf = reservation.getIdentification().getGuestCpf();
            response.companyCnpj = reservation.getIdentification().getCompanyCnpj();
            response.companyName = reservation.getIdentification().getCompanyName();
            response.guestPassport = reservation.getIdentification().getGuestPassport();
            response.guestCountry = reservation.getIdentification().getGuestCountry();
        }

        response.checkIn = reservation.getCheckIn();
        response.checkOut = reservation.getCheckOut();
        response.guests = reservation.getGuests();
        response.ratePlan = reservation.getRatePlan();
        response.nights = reservation.getNights();
        response.subtotal = reservation.getSubtotal();
        response.discount = reservation.getDiscount();
        response.total = reservation.getTotal();
        response.status = reservation.getStatus();
        response.amountPaid = reservation.getAmountPaid();
        response.refund = reservation.getRefund();

        return response;
    }

    public Long getId() {
        return id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public String getCompanyCnpj() {
        return companyCnpj;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getGuestPassport() {
        return guestPassport;
    }

    public String getGuestCountry() {
        return guestCountry;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public RatePlan getRatePlan() {
        return ratePlan;
    }

    public int getNights() {
        return nights;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public BigDecimal getRefund() {
        return refund;
    }
}
