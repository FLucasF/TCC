package br.com.marealta.notificacao;

import java.util.ArrayList;
import java.util.List;

/** Lista das mensagens enviadas; os e-mails ainda não saem de verdade. */
public class CaixaDeSaida {

    private final List<Mensagem> enviadas = new ArrayList<>();

    public synchronized void enviarEmail(String para, String assunto, String texto) {
        enviadas.add(new Mensagem(enviadas.size() + 1, Canal.EMAIL, para, assunto, texto));
    }

    public synchronized List<Mensagem> enviadas() {
        return List.copyOf(enviadas);
    }
}
