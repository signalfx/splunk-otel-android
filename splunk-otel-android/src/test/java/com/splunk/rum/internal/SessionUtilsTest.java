package com.splunk.rum.internal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SessionUtilsTest {

    @Test
    void testConvert(){
        long result = SessionUtils.convertToUInt32("c06947ed1f53b1a69be3c6899bc11a3e");
        assertEquals(3742903036L, result);
    }

}