package com.pousada.marealta.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("iss")
    public BigDecimal iss;

    @JsonProperty("text")
    public String text;
}
