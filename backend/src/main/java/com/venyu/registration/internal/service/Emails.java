package com.venyu.registration.internal.service;

import java.util.Locale;

final class Emails {

    private Emails() {
    }

    // Misafir ve davet e-postaları bu haliyle saklanır ve unique index'ler buna göre çalışır.
    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
