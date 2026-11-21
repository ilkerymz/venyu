package com.venyu.event.internal.domain;

// Etkinliğin her durumda düzenlenebilen alanları.
public record EventDetails(String title, String description, EventVisibility visibility) {
}
