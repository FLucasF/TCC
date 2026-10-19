package br.com.marealta;

import br.com.marealta.dominio.OuvinteReserva;
import br.com.marealta.dominio.Pousada;
import br.com.marealta.dominio.ReservaService;
import br.com.marealta.notificacao.AvisoFinanceiro;
import br.com.marealta.notificacao.AvisoGovernanca;
import br.com.marealta.notificacao.AvisoHospede;
import br.com.marealta.notificacao.CaixaDeSaida;
import br.com.marealta.relatorio.Relatorios;
import br.com.marealta.web.Rotas;
import io.javalin.Javalin;

import java.util.List;

public class Aplicacao {

    public static void main(String[] args) {
        String porta = System.getenv("PORT");
        criar().start(porta == null || porta.isBlank() ? 7070 : Integer.parseInt(porta.trim()));
    }

    public static Javalin criar() {
        Pousada pousada = new Pousada();
        CaixaDeSaida caixa = new CaixaDeSaida();

        // Quem for avisado das reservas entra só nesta lista.
        List<OuvinteReserva> ouvintes = List.of(
                new AvisoHospede(caixa),
                new AvisoGovernanca(caixa),
                new AvisoFinanceiro(caixa));

        ReservaService reservas = new ReservaService(pousada, ouvintes);
        Javalin app = Javalin.create();
        new Rotas(pousada, reservas, caixa, new Relatorios(pousada, reservas)).registrar(app);
        return app;
    }
}
