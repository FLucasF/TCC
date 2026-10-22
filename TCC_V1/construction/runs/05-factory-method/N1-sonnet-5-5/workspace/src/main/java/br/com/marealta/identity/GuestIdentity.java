package br.com.marealta.identity;

import java.util.Map;

public record GuestIdentity(IdentityKind kind, Map<String, String> values) {}
