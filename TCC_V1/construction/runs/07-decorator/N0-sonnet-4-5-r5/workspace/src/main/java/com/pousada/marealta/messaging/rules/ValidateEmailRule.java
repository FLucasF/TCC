package com.pousada.marealta.messaging.rules;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.MessageStatus;
import com.pousada.marealta.messaging.MessageRule;

import java.util.Collections;
import java.util.List;

/**
 * Regra que valida endereços de e-mail e bloqueia mensagens para endereços inválidos.
 *
 * Um e-mail é válido quando:
 * - Tem um único @
 * - Tem algo antes do @
 * - Depois do @, tem um domínio com pelo menos um ponto (ex: gmail.com)
 */
public class ValidateEmailRule implements MessageRule {

    @Override
    public List<Message> process(Message message) {
        if (!isValidEmail(message.getTo())) {
            message.setStatus(MessageStatus.BLOCKED);
        }
        return Collections.singletonList(message);
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        int atCount = 0;
        int atPosition = -1;

        for (int i = 0; i < email.length(); i++) {
            if (email.charAt(i) == '@') {
                atCount++;
                atPosition = i;
            }
        }

        // Deve ter exatamente um @
        if (atCount != 1) {
            return false;
        }

        // Deve ter algo antes do @
        if (atPosition == 0) {
            return false;
        }

        // Deve ter algo depois do @
        String domain = email.substring(atPosition + 1);
        if (domain.isEmpty()) {
            return false;
        }

        // O domínio deve ter pelo menos um ponto
        if (!domain.contains(".")) {
            return false;
        }

        // O ponto não pode ser no início ou fim do domínio
        if (domain.startsWith(".") || domain.endsWith(".")) {
            return false;
        }

        return true;
    }
}
