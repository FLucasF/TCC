package hotel.messages;

public final class ProcessingResult {
    private final boolean blocked;
    private final EmailToSend email;

    public ProcessingResult(boolean blocked, EmailToSend email) {
        this.blocked = blocked;
        this.email = email;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public EmailToSend email() {
        return email;
    }

    public static ProcessingResult blocked() {
        return new ProcessingResult(true, null);
    }

    public static ProcessingResult passed(EmailToSend email) {
        return new ProcessingResult(false, email);
    }
}
