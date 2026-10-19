package com.pousadamareaalta.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamareaalta.model.Reservation;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationResponse(
    @JsonProperty("id") long id,
    @JsonProperty("roomNumber") int roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("companyCnpj") String companyCnpj,
    @JsonProperty("companyName") String companyName,
    @JsonProperty("guestPassport") String guestPassport,
    @JsonProperty("guestCountry") String guestCountry,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    @JsonProperty("guests") int guests,
    @JsonProperty("ratePlan") String ratePlan,
    @JsonProperty("nights") int nights,
    @JsonProperty("subtotal") BigDecimal subtotal,
    @JsonProperty("discount") BigDecimal discount,
    @JsonProperty("total") BigDecimal total,
    @JsonProperty("status") String status,
    @JsonProperty("amountPaid") BigDecimal amountPaid,
    @JsonProperty("refund") BigDecimal refund
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
            r.getId(),
            r.getRoomNumber(),
            r.getGuestName(),
            r.getGuestEmail(),
            r.getGuestCpf(),
            r.getCompanyCnpj(),
            r.getCompanyName(),
            r.getGuestPassport(),
            r.getGuestCountry(),
            com.pousadamareaalta.util.DateUtils.formatDateIso(r.getCheckIn()),
            com.pousadamareaalta.util.DateUtils.formatDateIso(r.getCheckOut()),
            r.getGuests(),
            r.getRatePlan().toString(),
            r.getNights(),
            r.getSubtotal(),
            r.getDiscount(),
            r.getTotal(),
            r.getStatus().toString(),
            r.getAmountPaid(),
            r.getRefund()
        );
    }
}
