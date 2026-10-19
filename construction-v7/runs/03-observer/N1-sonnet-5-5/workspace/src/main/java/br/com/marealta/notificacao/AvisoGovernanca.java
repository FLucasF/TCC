package br.com.marealta.notificacao;

import br.com.marealta.dominio.Dinheiro;
import br.com.marealta.dominio.OuvinteReserva;
import br.com.marealta.dominio.Reserva;

public class AvisoGovernanca implements OuvinteReserva {

    static final String ENDERECO = "governanca@pousadamarealta.com.br";

    private final CaixaDeSaida caixa;

    public AvisoGovernanca(CaixaDeSaida caixa) {
        this.caixa = caixa;
    }

    @Override
    public void reservaConfirmada(Reserva reserva) {
        int quarto = reserva.quarto().numero();
        caixa.enviarEmail(ENDERECO, "Reserva confirmada - quarto " + quarto,
                "A reserva nº " + reserva.id() + " do quarto " + quarto + " foi confirmada.\n"
                        + "Entrada: " + Dinheiro.formatar(reserva.entrada()) + "\n"
                        + "Saída: " + Dinheiro.formatar(reserva.saida()) + "\n"
                        + "Hóspedes: " + reserva.hospedes());
    }

    @Override
    public void reservaConfirmadaCancelada(Reserva reserva) {
        int quarto = reserva.quarto().numero();
        caixa.enviarEmail(ENDERECO, "Reserva cancelada - quarto " + quarto,
                "A reserva nº " + reserva.id() + " do quarto " + quarto + " foi cancelada.\n"
                        + "Datas liberadas: de " + Dinheiro.formatar(reserva.entrada())
                        + " (entrada) a " + Dinheiro.formatar(reserva.saida()) + " (saída).");
    }
}
