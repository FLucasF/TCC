package br.com.marealta.servico;

import java.util.ArrayList;
import java.util.List;

/** Guarda as mensagens "enviadas" enquanto os e-mails não saem de verdade. */
public class CaixaDeSaida {

    private final List<Mensagem> enviadas = new ArrayList<>();

    public synchronized void enviarEmail(String destinatario, String assunto, String texto) {
        enviadas.add(new Mensagem(enviadas.size() + 1, "EMAIL", destinatario, assunto, texto));
    }

    public synchronized List<Mensagem> enviadas() {
        return List.copyOf(enviadas);
    }
}
