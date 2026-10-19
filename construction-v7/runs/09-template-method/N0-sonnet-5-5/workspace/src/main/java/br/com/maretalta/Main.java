package br.com.maretalta;

import br.com.maretalta.report.TourismTaxPolicy;

public class Main {
    public static void main(String[] args) {
        String port = System.getenv("PORT");
        TourismTaxPolicy tax = TourismTaxPolicy.parse(
                System.getenv("TOURISM_TAX_PER_GUEST_NIGHT"), System.getenv("TOURISM_TAX_BY_YEAR"));
        new App(tax).create().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}
