package com.venyu.form;

import java.util.List;

public record EventForm(int version, List<FormField> fields) {
}
