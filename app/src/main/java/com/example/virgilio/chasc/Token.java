package com.example.virgilio.chasc;

import java.security.SecureRandom;

// Class used to obtain an alfanumeric token (used as reference for range schedules)
public class Token {
    private final String VALID = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private final int DEFAULT_LENGTH = 32;
    private SecureRandom rnd;
    private String value;

    public Token() {
        this.rnd = new SecureRandom();
        this.value = randomString(DEFAULT_LENGTH);
    }

    public Token(int length) {
        this.rnd = new SecureRandom();
        this.value = randomString(length);
    }

    public String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i=0; i<length; i++) {
            sb.append(VALID.charAt(rnd.nextInt(VALID.length())));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return value;
    }
}
