package com.marealta.persistence;

import java.sql.SQLException;

public class UncheckedSqlException extends RuntimeException {
    public UncheckedSqlException(SQLException cause) {
        super(cause);
    }
}
