package com.pousadamarealta.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;

public class MessageServiceTest {

    @Test
    public void testValidEmailAddressPattern() throws Exception {
        Method method = MessageService.class.getDeclaredMethod("isValidEmail", String.class);
        method.setAccessible(true);

        String validEmail = "user@example.com";
        boolean result = (boolean) method.invoke(null, validEmail);
        assertTrue(result);
    }

    @Test
    public void testInvalidEmailNoAt() throws Exception {
        Method method = MessageService.class.getDeclaredMethod("isValidEmail", String.class);
        method.setAccessible(true);

        String email = "usergmail.com";
        boolean result = (boolean) method.invoke(null, email);
        assertFalse(result);
    }

    @Test
    public void testInvalidEmailNoLocalPart() throws Exception {
        Method method = MessageService.class.getDeclaredMethod("isValidEmail", String.class);
        method.setAccessible(true);

        String email = "@example.com";
        boolean result = (boolean) method.invoke(null, email);
        assertFalse(result);
    }

    @Test
    public void testInvalidEmailNoDot() throws Exception {
        Method method = MessageService.class.getDeclaredMethod("isValidEmail", String.class);
        method.setAccessible(true);

        String email = "user@example";
        boolean result = (boolean) method.invoke(null, email);
        assertFalse(result);
    }

    @Test
    public void testInvalidEmailMultipleAt() throws Exception {
        Method method = MessageService.class.getDeclaredMethod("isValidEmail", String.class);
        method.setAccessible(true);

        String email = "user@@example.com";
        boolean result = (boolean) method.invoke(null, email);
        assertFalse(result);
    }
}
