package br.com.maralta.dto;

import br.com.maralta.model.GuestIdentification;
import br.com.maralta.model.RatePlan;
import br.com.maralta.model.Reservation;

import java.time.LocalDate;

public class CreateReservationRequest {
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

    public Reservation toReservation() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);

        GuestIdentification identification = new GuestIdentification();
        identification.setGuestCpf(guestCpf);
        identification.setCompanyCnpj(companyCnpj);
        identification.setCompanyName(companyName);
        identification.setGuestPassport(guestPassport);
        identification.setGuestCountry(guestCountry);
        reservation.setIdentification(identification);

        return reservation;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public void setGuestCpf(String guestCpf) {
        this.guestCpf = guestCpf;
    }

    public String getCompanyCnpj() {
        return companyCnpj;
    }

    public void setCompanyCnpj(String companyCnpj) {
        this.companyCnpj = companyCnpj;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getGuestPassport() {
        return guestPassport;
    }

    public void setGuestPassport(String guestPassport) {
        this.guestPassport = guestPassport;
    }

    public String getGuestCountry() {
        return guestCountry;
    }

    public void setGuestCountry(String guestCountry) {
        this.guestCountry = guestCountry;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public void setGuests(int guests) {
        this.guests = guests;
    }

    public RatePlan getRatePlan() {
        return ratePlan;
    }

    public void setRatePlan(RatePlan ratePlan) {
        this.ratePlan = ratePlan;
    }
}
