package com.marealta.mail;

/** What this installation wants from its outgoing mail. */
public record MailSettings(boolean testMode, boolean archiveCopy) {
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
}
