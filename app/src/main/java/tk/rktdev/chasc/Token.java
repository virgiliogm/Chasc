package tk.rktdev.chasc;

import java.security.SecureRandom;

// Class used to obtain an alphanumeric token (used as reference for range schedules)
public class Token {
    private SecureRandom rnd;
    private String value;

    public Token() {
        int DEFAULT_LENGTH = 32;

        this.rnd = new SecureRandom();
        this.value = randomString(DEFAULT_LENGTH);
    }

    public Token(int length) {
        this.rnd = new SecureRandom();
        this.value = randomString(length);
    }

    private String randomString(int length) {
        String VALID = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
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
