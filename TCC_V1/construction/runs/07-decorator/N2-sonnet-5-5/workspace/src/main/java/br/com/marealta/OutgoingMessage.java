package br.com.marealta;

/** Mensagem a caminho da caixa de saída; as regras de envio podem reescrevê-la. */
public record OutgoingMessage(Channel channel, String to, String subject, String body) {
    OutgoingMessage withTo(String newTo) {
        return new OutgoingMessage(channel, newTo, subject, body);
    }

    OutgoingMessage withSubject(String newSubject) {
        return new OutgoingMessage(channel, to, newSubject, body);
    }

    OutgoingMessage withBody(String newBody) {
        return new OutgoingMessage(channel, to, subject, newBody);
    }
}
