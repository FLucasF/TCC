package br.com.marealta.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Comprovante(int reserva, String nomeHospede, String cpfHospede, int quarto, TipoQuarto tipoQuarto,
                          LocalDate entrada, LocalDate saida, int noites,
                          BigDecimal subtotal, BigDecimal desconto, BigDecimal total) {

    public static Comprovante de(Reserva r) {
        Preco p = r.preco();
        return new Comprovante(r.id(), r.nomeHospede(), r.cpfHospede(), r.quarto().numero(), r.quarto().tipo(),
                r.entrada(), r.saida(), p.noites(), p.subtotal(), p.desconto(), p.total());
    }

    public String texto() {
        StringBuilder t = new StringBuilder();
        t.append("Comprovante de reserva nº ").append(reserva).append('\n');
        t.append("Hóspede: ").append(nomeHospede).append(" (CPF ").append(cpfHospede).append(")\n");
        t.append("Quarto: ").append(quarto).append(" (").append(tipoQuarto).append(")\n");
        t.append("Entrada: ").append(Dinheiro.formatar(entrada)).append(" | Saída: ").append(Dinheiro.formatar(saida))
                .append(" | ").append(noites).append(noites == 1 ? " diária" : " diárias").append('\n');
        t.append("Diárias: ").append(Dinheiro.formatar(subtotal)).append('\n');
        if (desconto.signum() > 0) {
            t.append("Desconto: ").append(Dinheiro.formatar(desconto)).append('\n');
        }
        t.append("Total: ").append(Dinheiro.formatar(total));
        return t.toString();
    }
}
