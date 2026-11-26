package com.example.core.calendar

import java.util.Calendar
import java.util.TimeZone

data class JalaliDate(val year: Int, val month: Int, val day: Int)

data class GregorianDate(val year: Int, val month: Int, val day: Int)

enum class CalendarSystem { JALALI, GREGORIAN }

enum class WeekStartDay(val saturdayBasedOffset: Int, val persianLabel: String) {
    SATURDAY(0, "شنبه"),
    SUNDAY(1, "یکشنبه"),
    MONDAY(2, "دوشنبه")
}

object PersianCalendar {

    private const val UNIX_EPOCH_JDN = 2440588L

    private val JALALI_BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    val jalaliMonthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val gregorianMonthNamesFa = listOf(
        "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
        "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"
    )

    // 0 = Saturday ... 6 = Friday
    val weekdayNames = listOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    val weekdayShortNames = listOf(
        "ش", "ی", "د", "س", "چ", "پ", "ج"
    )

    private data class JalCalResult(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): JalCalResult {
        val gy = jy + 621
        var leapJ = -14
        var jp = JALALI_BREAKS[0]
        var jump = 0
        for (i in 1 until JALALI_BREAKS.size) {
            val jm = JALALI_BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + (jump % 33) / 4
            jp = jm
        }
        var n = jy - jp
        leapJ += (n / 33) * 8 + ((n % 33) + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) {
            leapJ += 1
        }
        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) {
            n = n - jump + ((jump + 4) / 33) * 33
        }
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return JalCalResult(leap = leap, gy = gy, march = march)
    }

    fun isJalaliLeapYear(jy: Int): Boolean = jalCal(jy).leap == 0

    fun isGregorianLeapYear(gy: Int): Boolean =
        (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)

    fun jalaliMonthLength(jy: Int, jm: Int): Int = when {
        jm in 1..6 -> 31
        jm in 7..11 -> 30
        jm == 12 -> if (isJalaliLeapYear(jy)) 30 else 29
        else -> 30
    }

    fun gregorianMonthLength(gy: Int, gm: Int): Int = when (gm) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isGregorianLeapYear(gy)) 29 else 28
        else -> 30
    }

    fun gregorianToJdn(gy: Int, gm: Int, gd: Int): Long {
        val a = (14 - gm) / 12
        val y = gy.toLong() + 4800L - a
        val m = gm.toLong() + 12L * a - 3L
        return gd.toLong() + (153L * m + 2L) / 5L + 365L * y + y / 4L - y / 100L + y / 400L - 32045L
    }

    fun jdnToGregorian(jdn: Long): GregorianDate {
        val a = jdn + 32044L
        val b = (4L * a + 3L) / 146097L
        val c = a - (146097L * b) / 4L
        val d = (4L * c + 3L) / 1461L
        val e = c - (1461L * d) / 4L
        val m = (5L * e + 2L) / 153L
        val day = (e - (153L * m + 2L) / 5L + 1L).toInt()
        val month = (m + 3L - 12L * (m / 10L)).toInt()
        val year = (100L * b + d - 4800L + m / 10L).toInt()
        return GregorianDate(year, month, day)
    }

    fun jalaliToJdn(jy: Int, jm: Int, jd: Int): Long {
        val r = jalCal(jy)
        val jdn1f = gregorianToJdn(r.gy, 3, r.march)
        val dayOfYear = if (jm <= 6) {
            (jm - 1) * 31 + (jd - 1)
        } else {
            186 + (jm - 7) * 30 + (jd - 1)
        }
        return jdn1f + dayOfYear.toLong()
    }

    fun jdnToJalali(jdn: Long): JalaliDate {
        val gy = jdnToGregorian(jdn).year
        var jy = gy - 621
        var r = jalCal(jy)
        var jdn1f = gregorianToJdn(r.gy, 3, r.march)
        var k = (jdn - jdn1f).toInt()
        if (k < 0) {
            jy -= 1
            r = jalCal(jy)
            jdn1f = gregorianToJdn(r.gy, 3, r.march)
            k = (jdn - jdn1f).toInt()
        }
        return if (k <= 185) {
            JalaliDate(jy, 1 + k / 31, (k % 31) + 1)
        } else {
            val rem = k - 186
            JalaliDate(jy, 7 + rem / 30, (rem % 30) + 1)
        }
    }

    fun gregorianToEpochDay(gy: Int, gm: Int, gd: Int): Long =
        gregorianToJdn(gy, gm, gd) - UNIX_EPOCH_JDN

    fun epochDayToGregorian(epochDay: Long): GregorianDate =
        jdnToGregorian(epochDay + UNIX_EPOCH_JDN)

    fun jalaliToEpochDay(jy: Int, jm: Int, jd: Int): Long =
        jalaliToJdn(jy, jm, jd) - UNIX_EPOCH_JDN

    fun epochDayToJalali(epochDay: Long): JalaliDate =
        jdnToJalali(epochDay + UNIX_EPOCH_JDN)

    fun todayEpochDay(): Long {
        val cal = Calendar.getInstance()
        return gregorianToEpochDay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun currentMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun epochDayAndMinuteToMillis(epochDay: Long, minuteOfDay: Int): Long {
        val g = epochDayToGregorian(epochDay)
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.YEAR, g.year)
            set(Calendar.MONTH, g.month - 1)
            set(Calendar.DAY_OF_MONTH, g.day)
            set(Calendar.HOUR_OF_DAY, (minuteOfDay / 60).coerceIn(0, 23))
            set(Calendar.MINUTE, (minuteOfDay % 60).coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Returns 0 for Saturday (شنبه) ... 6 for Friday (جمعه).
     * 1970-01-01 (epochDay 0) was Thursday (پنج‌شنبه = 5).
     */
    fun dayOfWeekSaturdayFirst(epochDay: Long): Int {
        val mod = ((epochDay + 5L) % 7L + 7L) % 7L
        return mod.toInt()
    }

    fun weekdayName(epochDay: Long): String = weekdayNames[dayOfWeekSaturdayFirst(epochDay)]

    fun startOfWeekEpochDay(epochDay: Long, weekStartDay: WeekStartDay): Long {
        val currentDow = dayOfWeekSaturdayFirst(epochDay)
        val diff = ((currentDow - weekStartDay.saturdayBasedOffset) % 7 + 7) % 7
        return epochDay - diff
    }

    fun weekdayOrder(weekStartDay: WeekStartDay): List<Int> =
        List(7) { index -> (weekStartDay.saturdayBasedOffset + index) % 7 }

    // BiDi & Digit Formatting
    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun formatDigits(input: String, usePersianDigits: Boolean): String {
        if (!usePersianDigits) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatNumber(value: Int, usePersianDigits: Boolean): String =
        formatDigits(value.toString(), usePersianDigits)

    fun formatNumber(value: Long, usePersianDigits: Boolean): String =
        formatDigits(value.toString(), usePersianDigits)

    /** Unicode Left-to-Right Isolate so numbers/times/dates never invert in RTL layouts. */
    fun ltrIsolate(text: String): String = "\u2066$text\u2069"

    fun formatFullDate(
        epochDay: Long,
        calendarSystem: CalendarSystem,
        usePersianDigits: Boolean
    ): String {
        return when (calendarSystem) {
            CalendarSystem.JALALI -> {
                val j = epochDayToJalali(epochDay)
                val dayStr = formatNumber(j.day, usePersianDigits)
                val yearStr = formatNumber(j.year, usePersianDigits)
                "$dayStr ${jalaliMonthNames[j.month - 1]} $yearStr"
            }
            CalendarSystem.GREGORIAN -> {
                val g = epochDayToGregorian(epochDay)
                val dayStr = formatNumber(g.day, usePersianDigits)
                val yearStr = formatNumber(g.year, usePersianDigits)
                "$dayStr ${gregorianMonthNamesFa[g.month - 1]} $yearStr"
            }
        }
    }

    fun formatShortDate(
        epochDay: Long,
        calendarSystem: CalendarSystem,
        usePersianDigits: Boolean
    ): String {
        val raw = when (calendarSystem) {
            CalendarSystem.JALALI -> {
                val j = epochDayToJalali(epochDay)
                "%04d/%02d/%02d".format(j.year, j.month, j.day)
            }
            CalendarSystem.GREGORIAN -> {
                val g = epochDayToGregorian(epochDay)
                "%04d/%02d/%02d".format(g.year, g.month, g.day)
            }
        }
        return ltrIsolate(formatDigits(raw, usePersianDigits))
    }

    fun formatMonthYear(
        year: Int,
        month: Int,
        calendarSystem: CalendarSystem,
        usePersianDigits: Boolean
    ): String {
        val monthName = when (calendarSystem) {
            CalendarSystem.JALALI -> jalaliMonthNames[(month - 1).coerceIn(0, 11)]
            CalendarSystem.GREGORIAN -> gregorianMonthNamesFa[(month - 1).coerceIn(0, 11)]
        }
        return "$monthName ${formatNumber(year, usePersianDigits)}"
    }

    fun formatTime(
        minuteOfDay: Int,
        use24Hour: Boolean,
        usePersianDigits: Boolean
    ): String {
        val clamped = minuteOfDay.coerceIn(0, 1439)
        val h = clamped / 60
        val m = clamped % 60
        return if (use24Hour) {
            val raw = "%02d:%02d".format(h, m)
            ltrIsolate(formatDigits(raw, usePersianDigits))
        } else {
            val suffix = if (h < 12) "ق.ظ" else "ب.ظ"
            val hour12 = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            val raw = "%02d:%02d".format(hour12, m)
            "${ltrIsolate(formatDigits(raw, usePersianDigits))} $suffix"
        }
    }

    fun formatPercent(percent: Int, usePersianDigits: Boolean): String =
        "${formatNumber(percent.coerceIn(0, 100), usePersianDigits)}٪"

    fun formatDuration(minutes: Int, usePersianDigits: Boolean): String {
        if (minutes <= 0) return "${formatNumber(0, usePersianDigits)} دقیقه"
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${formatNumber(h, usePersianDigits)} ساعت و ${formatNumber(m, usePersianDigits)} دقیقه"
            h > 0 -> "${formatNumber(h, usePersianDigits)} ساعت"
            else -> "${formatNumber(m, usePersianDigits)} دقیقه"
        }
    }

    fun currentGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "صبح بخیر"
            in 12..16 -> "ظهر بخیر"
            in 17..20 -> "عصر بخیر"
            else -> "شب بخیر"
        }
    }
}
