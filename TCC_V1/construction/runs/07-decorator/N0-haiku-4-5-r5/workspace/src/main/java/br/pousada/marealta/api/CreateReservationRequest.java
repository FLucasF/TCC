package br.pousada.marealta.api;

import com.google.gson.annotations.SerializedName;

public class CreateReservationRequest {
    @SerializedName("roomNumber")
    public int roomNumber;

    @SerializedName("guestName")
    public String guestName;

    @SerializedName("guestEmail")
    public String guestEmail;

    @SerializedName("guestCpf")
    public String guestCpf;

    @SerializedName("checkIn")
    public String checkIn;

    @SerializedName("checkOut")
    public String checkOut;

    public int guests;

    @SerializedName("ratePlan")
    public String ratePlan;
}
