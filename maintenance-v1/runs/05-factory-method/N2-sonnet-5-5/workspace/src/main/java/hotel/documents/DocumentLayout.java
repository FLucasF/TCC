package hotel.documents;

/** How one kind of document is written: its title and its text. */
interface DocumentLayout {

    String title();

    String text(ReservationDocument document);
}
