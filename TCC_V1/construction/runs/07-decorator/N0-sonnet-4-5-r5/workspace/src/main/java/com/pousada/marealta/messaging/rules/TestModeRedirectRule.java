package com.pousada.marealta.messaging.rules;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.MessageStatus;
import com.pousada.marealta.messaging.MessageRule;

import java.util.Collections;
import java.util.List;

/**
 * Regra que redireciona mensagens em modo de teste para evitar envio a destinatários reais.
 *
 * No modo de teste:
 * - Mensagens são enviadas para testes@pousadamarealta.com.br
 * - Assunto recebe prefixo "[TESTE] "
 * - Primeira linha do texto indica o destinatário original
 * - Mensagens bloqueadas mantêm o endereço original
 */
public class TestModeRedirectRule implements MessageRule {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final String TEST_PREFIX = "[TESTE] ";

    private final boolean testMode;

    public TestModeRedirectRule(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public List<Message> process(Message message) {
        if (!testMode) {
            return Collections.singletonList(message);
        }

        // Mensagens bloqueadas não são redirecionadas
        if (message.getStatus() == MessageStatus.BLOCKED) {
            return Collections.singletonList(message);
        }

        String originalRecipient = message.getTo();
        message.setTo(TEST_EMAIL);
        message.setSubject(TEST_PREFIX + message.getSubject());
        message.setBody("Destinatário original: " + originalRecipient + "\n" + message.getBody());

        return Collections.singletonList(message);
    }
}
