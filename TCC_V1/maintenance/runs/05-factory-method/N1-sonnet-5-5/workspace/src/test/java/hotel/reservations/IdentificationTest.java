package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IdentificationTest {

    private static void assertInvalid(String cpf, String cnpj, String companyName, String passport, String country) {
        HotelException failure = assertThrows(
                HotelException.class, () -> Identification.of(cpf, cnpj, companyName, passport, country));
        assertEquals(ErrorCode.INVALID_REQUEST, failure.errorCode());
    }

    @Test
    void acceptsEachOfTheThreeWays() {
        assertEquals(new Identification.PersonalId("111"), Identification.of("111", null, null, null, null));
        assertEquals(new Identification.Company("12", "Tecnosul"), Identification.of(null, "12", "Tecnosul", null, null));
        assertEquals(new Identification.Foreigner("X1", "Chile"), Identification.of(null, null, null, "X1", "Chile"));
        assertEquals(new Identification.PersonalId("111"), Identification.of("111", " ", "", null, ""));
    }

    @Test
    void refusesNoneAndMoreThanOne() {
        assertInvalid(null, null, null, null, null);
        assertInvalid("111", "12", "Tecnosul", null, null);
        assertInvalid("111", null, null, "X1", "Chile");
        assertInvalid(null, "12", "Tecnosul", "X1", "Chile");
    }

    @Test
    void refusesAWayWithPartOfItsData() {
        assertInvalid(null, "12", null, null, null);
        assertInvalid(null, null, "Tecnosul", null, null);
        assertInvalid(null, null, null, "X1", null);
        assertInvalid(null, null, null, null, "Chile");
        assertInvalid("111", "12", null, null, null);
    }
}
