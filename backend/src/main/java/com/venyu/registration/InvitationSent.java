package com.venyu.registration;

// Davet e-postası gönderilsin (notification dinler). token ham haliyle sadece bu event'te yaşar.
public record InvitationSent(String email, String eventTitle, String token) {
}
