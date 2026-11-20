package com.venyu.venue.internal.web.dto;

import jakarta.validation.constraints.Size;

public record HoldRequest(@Size(max = 500) String note) {
}