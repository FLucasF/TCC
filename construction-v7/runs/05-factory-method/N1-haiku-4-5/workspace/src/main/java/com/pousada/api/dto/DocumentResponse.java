package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Document;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentResponse {
    @JsonProperty("title")
    public String title;

    @JsonProperty("reservationId")
    public Long reservationId;

    @JsonProperty("guestName")
    public String guestName;

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

    @JsonProperty("roomNumber")
    public Integer roomNumber;

    @JsonProperty("roomType")
    public String roomType;

    @JsonProperty("checkIn")
    public String checkIn;

    @JsonProperty("checkOut")
    public String checkOut;

    @JsonProperty("nights")
    public Long nights;

    @JsonProperty("subtotal")
    public BigDecimal subtotal;

    @JsonProperty("discount")
    public BigDecimal discount;

    @JsonProperty("total")
    public BigDecimal total;

    @JsonProperty("text")
    public String text;

    public static DocumentResponse fromDocument(Document doc) {
        DocumentResponse dto = new DocumentResponse();
        dto.title = doc.getTitle();
        dto.reservationId = doc.getReservationId();
        dto.guestName = doc.getGuestName();
        dto.roomNumber = doc.getRoomNumber();
        dto.roomType = doc.getRoomType();
        dto.checkIn = doc.getCheckIn();
        dto.checkOut = doc.getCheckOut();
        dto.nights = doc.getNights();
        dto.subtotal = doc.getSubtotal();
        dto.discount = doc.getDiscount();
        dto.total = doc.getTotal();
        dto.text = doc.getText();

        if (doc.getGuestCpfId() != null) {
            dto.guestCpf = doc.getGuestCpfId().guestCpf;
        }
        if (doc.getCompanyCnpjId() != null) {
            dto.companyCnpj = doc.getCompanyCnpjId().companyCnpj;
            dto.companyName = doc.getCompanyCnpjId().companyName;
        }
        if (doc.getGuestPassportId() != null) {
            dto.guestPassport = doc.getGuestPassportId().guestPassport;
            dto.guestCountry = doc.getGuestPassportId().guestCountry;
        }

        return dto;
    }
}
