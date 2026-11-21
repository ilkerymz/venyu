package com.venyu.registration.internal.domain;

// Doğrulanmış form cevapları: JSON hali ve doğrulandığı form versiyonu (formsuz etkinlikte null, "{}").
public record FormAnswers(Integer formVersion, String json) {
}
