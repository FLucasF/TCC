package com.pousada.marealba.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CpfUtilTest {

    @Test
    public void testFormat() {
        assertEquals("111.222.333-44", CpfUtil.format("11122233344"));
    }

    @Test
    public void testFormatNull() {
        assertNull(CpfUtil.format(null));
    }

    @Test
    public void testFormatInvalidLength() {
        assertEquals("123", CpfUtil.format("123"));
    }
}
