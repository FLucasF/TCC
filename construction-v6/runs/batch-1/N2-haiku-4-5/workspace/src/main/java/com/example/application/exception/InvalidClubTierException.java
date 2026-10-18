package com.example.application.exception;

public class InvalidClubTierException extends ApplicationException {
    public InvalidClubTierException() {
        super("INVALID_CLUB_TIER", 422);
    }
}
