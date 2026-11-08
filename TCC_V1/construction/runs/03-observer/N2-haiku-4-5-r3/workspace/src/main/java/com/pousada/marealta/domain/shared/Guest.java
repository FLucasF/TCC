package com.pousada.marealta.domain.shared;

import java.util.Objects;
import java.util.regex.Pattern;

public class Guest {
    private final String name;
    private final String email;
    private final String cpf;

    private static final Pattern CPF_PATTERN = Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public Guest(String name, String email, String cpf) {
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");
        Objects.requireNonNull(cpf, "CPF cannot be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (!CPF_PATTERN.matcher(cpf).matches()) {
            throw new IllegalArgumentException("CPF must be in format XXX.XXX.XXX-XX");
        }

        this.name = name;
        this.email = email;
        this.cpf = cpf;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public String cpf() {
        return cpf;
    }

    public String cpfUnformatted() {
        return cpf.replaceAll("[.-]", "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Guest)) return false;
        Guest guest = (Guest) o;
        return cpf.equals(guest.cpf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpf);
    }

    @Override
    public String toString() {
        return name + " (" + cpf + ")";
    }
}
