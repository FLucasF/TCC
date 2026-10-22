package br.com.marealta.messaging;

import java.util.function.UnaryOperator;

/** Uma regra: onde vale e como embrulha o próximo passo do envio. */
public record MessageRule(RuleScope scope, UnaryOperator<MessageSender> wrapper) {}
