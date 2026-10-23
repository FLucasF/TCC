package com.pousadamarealta.messaging;

public class ProcessingContext {
    private final boolean isTestMode;

    public ProcessingContext(boolean isTestMode) {
        this.isTestMode = isTestMode;
    }

    public boolean isTestMode() {
        return isTestMode;
    }
}
