package br.com.marealta.messaging;

import java.util.function.UnaryOperator;

/** Uma regra de envio: onde vale e como embrulha o envio que está por dentro dela. */
public record SendingRule(Scope scope, UnaryOperator<MessageSender> wrapper) {}
