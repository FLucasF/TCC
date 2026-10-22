package hotel.documents;

/** Common interface for all document types. */
public interface DocumentType {
    String title();
    long reservationId();
    String text();
}
