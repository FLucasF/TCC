package hotel.reservations;

/** A company booking a room for an employee, identified by CNPJ and company name. */
public record CompanyIdentification(String companyCnpj, String companyName) implements Identification {
}
