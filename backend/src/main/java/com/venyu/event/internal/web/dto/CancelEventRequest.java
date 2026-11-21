package com.venyu.event.internal.web.dto;

import jakarta.validation.constraints.Size;

// Body opsiyonel; reason katılımcılara giden iptal bildiriminde gösterilir.
public record CancelEventRequest(@Size(max = 500) String reason) {
}
