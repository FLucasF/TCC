"""Calibration examples for the tests column of the quality reviewer (TCC_V1/evaluation/quality): two reference
solutions with their tests spoiled on purpose, both expected `ruim`.

- tests-none: 05-pattern with every test gone but one smoke test (the service starts and lists the rooms). Little
  coverage, nothing about the rules.
- tests-hollow: 03-pattern with every assertion turned into a call whose result nobody looks at
  (assertEquals -> Objects.equals, assertTrue/assertFalse -> Boolean.valueOf, assertNull -> Objects.isNull). The
  coverage stays as high as before, but the tests check nothing: only reading them shows it.
"""

import re

from kit import Solution

SMOKE_TEST = """package hotel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class HotelSmokeTest {

    @Test
    void theServiceStarts() throws Exception {
        try (EmptyDatabase database = new EmptyDatabase()) {
            Hotel hotel = new Hotel(database.dataSource());
            int port = hotel.start(0);
            HttpResponse<String> rooms = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/rooms")).build(),
                    HttpResponse.BodyHandlers.ofString());
            hotel.stop();

            assertEquals(200, rooms.statusCode());
        }
    }
}
"""

none = Solution("tests-none")
none.apply("05-pattern")
for test in sorted((none.root / "src" / "test" / "java").rglob("*Test.java")):
    test.unlink()
none.write("src/test/java/hotel/HotelSmokeTest.java", SMOKE_TEST)
none.build_and_save()

HOLLOW = [
    (re.compile(r"(?<![\w.])assertEquals\("), "java.util.Objects.equals("),
    (re.compile(r"(?<![\w.])assert(True|False)\("), "Boolean.valueOf("),
    (re.compile(r"(?<![\w.])assertNull\("), "java.util.Objects.isNull("),
]

hollow = Solution("tests-hollow")
hollow.apply("03-pattern")
for test in sorted((hollow.root / "src" / "test" / "java").rglob("*Test.java")):
    text = test.read_text(encoding="utf-8")
    for pattern, replacement in HOLLOW:
        text = pattern.sub(replacement, text)
    test.write_text(text, encoding="utf-8", newline="\n")
hollow.build_and_save()
