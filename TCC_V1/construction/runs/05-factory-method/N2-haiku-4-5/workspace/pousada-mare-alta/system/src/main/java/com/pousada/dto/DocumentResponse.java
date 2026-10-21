package com.pousada.dto;

import com.google.gson.annotations.SerializedName;
import com.pousada.document.DocumentContent;

public class DocumentResponse {
    @SerializedName("title")
    public String title;

    @SerializedName("reservationId")
    public int reservationId;

    @SerializedName("guestName")
    public String guestName;

    @SerializedName("guestCpf")
    public String guestCpf;

    @SerializedName("companyCnpj")
    public String companyCnpj;

    @SerializedName("companyName")
    public String companyName;

    @SerializedName("guestPassport")
    public String guestPassport;

    @SerializedName("guestCountry")
    public String guestCountry;

    @SerializedName("roomNumber")
    public int roomNumber;

    @SerializedName("roomType")
    public String roomType;

    @SerializedName("checkIn")
    public String checkIn;

    @SerializedName("checkOut")
    public String checkOut;

    @SerializedName("nights")
    public int nights;

    @SerializedName("subtotal")
    public double subtotal;

    @SerializedName("discount")
    public double discount;

    @SerializedName("total")
    public double total;

    @SerializedName("text")
    public String text;

    public static DocumentResponse fromDocumentContent(DocumentContent doc) {
        DocumentResponse response = new DocumentResponse();
        response.title = doc.title();
        response.reservationId = doc.reservationId();
        response.guestName = doc.guestName();
        response.guestCpf = doc.guestCpf();
        response.companyCnpj = doc.companyCnpj();
        response.companyName = doc.companyName();
        response.guestPassport = doc.guestPassport();
        response.guestCountry = doc.guestCountry();
        response.roomNumber = doc.roomNumber();
        response.roomType = doc.roomType();
        response.checkIn = doc.checkIn();
        response.checkOut = doc.checkOut();
        response.nights = doc.nights();
        response.subtotal = doc.subtotal();
        response.discount = doc.discount();
        response.total = doc.total();
        response.text = doc.text();
        return response;
    }
}
