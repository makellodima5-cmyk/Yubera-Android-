package com.yubera.app.features.profile

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun formatRegistrationDate(value: String?): String? {
    val datePart = value?.trim()?.take(10)?.takeIf(String::isNotBlank) ?: return null
    return runCatching {
        LocalDate.parse(datePart).format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT))
    }.getOrNull()
}
