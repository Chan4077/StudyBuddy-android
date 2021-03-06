package com.edricchan.studybuddy.extensions

import java.text.SimpleDateFormat
import java.util.*

/**
 * Converts a [Long] to a [Date]
 */
fun Long.toDate(): Date = Date(this)

/**
 * Converts a [Long] to a [Date]
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Use Long.toDate()")
fun Long?.toDate(): Date? = this?.let { Date(it) }

/**
 * Converts a [Long] to a [SimpleDateFormat]
 * @param format A format that [SimpleDateFormat] supports
 * @param locale The [Locale] to use
 * @see SimpleDateFormat
 * @see Date.toDateFormat
 */
fun Long.toDateFormat(format: String, locale: Locale = Locale.getDefault()) =
    SimpleDateFormat(format, locale).format(Date(this))

/**
 * Converts a [Long] to a [SimpleDateFormat]
 * @param format A format that [SimpleDateFormat] supports
 * @param locale The [Locale] to use
 * @see SimpleDateFormat
 * @see Date.toDateFormat
 */
fun Long?.toDateFormat(format: String, locale: Locale = Locale.getDefault()) = this?.let {
    SimpleDateFormat(format, locale).format(Date(it))
}