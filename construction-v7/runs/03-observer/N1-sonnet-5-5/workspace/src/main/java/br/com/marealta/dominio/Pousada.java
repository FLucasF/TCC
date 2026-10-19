package br.com.marealta.dominio;

import java.util.List;
import java.util.Optional;

public class Pousada {

    private final List<Quarto> quartos = List.of(
            new Quarto(101, TipoQuarto.STANDARD),
            new Quarto(102, TipoQuarto.STANDARD),
            new Quarto(201, TipoQuarto.DELUXE),
            new Quarto(202, TipoQuarto.DELUXE),
            new Quarto(301, TipoQuarto.SUITE));

    public List<Quarto> quartos() {
        return quartos;
    }

    public Optional<Quarto> quarto(int numero) {
        return quartos.stream().filter(q -> q.numero() == numero).findFirst();
    }
}
