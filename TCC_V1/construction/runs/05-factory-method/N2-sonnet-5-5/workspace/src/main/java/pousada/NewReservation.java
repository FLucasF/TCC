package pousada;

import java.time.LocalDate;

public record NewReservation(int roomNumber, String guestName, String guestEmail, GuestIdentity identity,
                             LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {

    public static NewReservation from(RequestBody body, IdentityRegistry identities) {
        int roomNumber = body.integer("roomNumber");
        String guestName = body.text("guestName");
        String guestEmail = body.text("guestEmail");
        GuestIdentity identity = identities.parse(body);
        LocalDate checkIn = body.date("checkIn");
        LocalDate checkOut = body.date("checkOut");
        int guests = body.integer("guests");
        RatePlan ratePlan = body.enumeration("ratePlan", RatePlan.class);
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalidRequest("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw ApiException.invalidRequest("É preciso ao menos um hóspede");
        }
        return new NewReservation(roomNumber, guestName, guestEmail, identity, checkIn, checkOut, guests, ratePlan);
    }
}
