package com.venyu.registration.internal.domain;

// Onaylanan kayda verilen bilet: QR'daki kod ve (sadece misafirde) "yönet" token'ının hash'i.
public record TicketCredentials(String ticketCode, String manageTokenHash) {
}
