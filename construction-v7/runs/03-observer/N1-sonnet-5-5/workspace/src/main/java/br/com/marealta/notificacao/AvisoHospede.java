package br.com.marealta.notificacao;

import br.com.marealta.dominio.Comprovante;
import br.com.marealta.dominio.OuvinteReserva;
import br.com.marealta.dominio.Reserva;

public class AvisoHospede implements OuvinteReserva {

    private final CaixaDeSaida caixa;

    public AvisoHospede(CaixaDeSaida caixa) {
        this.caixa = caixa;
    }

    @Override
    public void reservaConfirmada(Reserva reserva) {
        String texto = "Olá, " + reserva.nomeHospede() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Comprovante.de(reserva).texto() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        caixa.enviarEmail(reserva.emailHospede(), "Reserva confirmada - Pousada Maré Alta", texto);
    }
}
