package br.com.marealta.notificacao;

import br.com.marealta.dominio.Dinheiro;
import br.com.marealta.dominio.OuvinteReserva;
import br.com.marealta.dominio.Reserva;

public class AvisoFinanceiro implements OuvinteReserva {

    static final String ENDERECO = "financeiro@pousadamarealta.com.br";

    private final CaixaDeSaida caixa;

    public AvisoFinanceiro(CaixaDeSaida caixa) {
        this.caixa = caixa;
    }

    @Override
    public void reservaConfirmada(Reserva reserva) {
        caixa.enviarEmail(ENDERECO, "Pagamento recebido - reserva " + reserva.id(),
                "Pagamento recebido da reserva nº " + reserva.id() + ": " + Dinheiro.formatar(reserva.valorPago()) + ".");
    }

    @Override
    public void reservaConfirmadaCancelada(Reserva reserva) {
        if (reserva.reembolso().signum() > 0) {
            caixa.enviarEmail(ENDERECO, "Reembolso - reserva " + reserva.id(),
                    "Reembolso da reserva nº " + reserva.id() + ": valor a devolver "
                            + Dinheiro.formatar(reserva.reembolso()) + ".");
        }
    }
}
