package com.venyu.event.internal.service;

import java.security.SecureRandom;

// Paylaşım link'i için tahmin edilemeyen kısa kimlik. Karışan karakterler (0/o, 1/l/i) alfabede yok.
final class Slugs {

    private static final String ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final int LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Slugs() {
    }

    static String generate() {
        StringBuilder slug = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            slug.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return slug.toString();
    }
}
