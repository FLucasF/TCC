package com.pousadamarealta.email;

public abstract class EmailRule {
    protected EmailRule next;

    public void setNext(EmailRule next) {
        this.next = next;
    }

    public final void process(EmailMessage message) {
        if (shouldProcess(message)) {
            doProcess(message);
        }

        if (!message.isBlocked() && next != null) {
            next.process(message);
        }
    }

    protected abstract boolean shouldProcess(EmailMessage message);

    protected abstract void doProcess(EmailMessage message);
}
