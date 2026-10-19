package br.com.marealta.dominio;

import java.time.LocalDate;

public record NovaReserva(int quarto, String nomeHospede, String emailHospede, String cpfHospede,
                          LocalDate entrada, LocalDate saida, int hospedes, PlanoTarifa plano) {
}
