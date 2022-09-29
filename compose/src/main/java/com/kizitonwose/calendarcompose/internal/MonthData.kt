package com.kizitonwose.calendarcompose.internal

import android.os.Parcelable
import com.kizitonwose.calendarcompose.CalendarDay
import com.kizitonwose.calendarcompose.CalendarMonth
import com.kizitonwose.calendarcompose.DayPosition
import com.kizitonwose.calendarcompose.OutDateStyle
import com.kizitonwose.calendarcore.atStartOfMonth
import com.kizitonwose.calendarcore.yearMonth
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.temporal.WeekFields

@Parcelize // Parcelize because it is used as LazyRow key.
internal data class MonthData(val month: YearMonth, val inDays: Int, val outDays: Int) :
    Parcelable {

    @IgnoredOnParcel
    private val totalDays = inDays + month.lengthOfMonth() + outDays

    @IgnoredOnParcel
    private val firstDay = month.atStartOfMonth().minusDays(inDays.toLong())

    @IgnoredOnParcel
    private val rows = (0 until totalDays).chunked(7)

    @IgnoredOnParcel
    private val cache = mutableMapOf<Int, CalendarDay>()

    @IgnoredOnParcel
    val calendarMonth =
        CalendarMonth(month, rows.map { week -> week.map { dayOffset -> getDay(dayOffset) } })

    private fun getDay(columnOffset: Int): CalendarDay {
        return cache.getOrPut(columnOffset) {
            val date = firstDay.plusDays(columnOffset.toLong())
            val owner = when (date.yearMonth) {
                month -> DayPosition.MonthDate
                month.minusMonths(1) -> DayPosition.InDate
                month.plusMonths(1) -> DayPosition.OutDate
                else -> throw IllegalArgumentException("Invalid date: $date in month: $month")
            }
            return@getOrPut CalendarDay(date, owner)
        }
    }
}

internal fun getCalendarMonthData(
    startMonth: YearMonth,
    offset: Int,
    firstDayOfWeek: DayOfWeek,
    outDateStyle: OutDateStyle
): MonthData {
    val month = startMonth.plusMonths(offset.toLong())
    val firstDay = month.atStartOfMonth()
    val inDays = firstDayOfWeek.daysUntil(firstDay.dayOfWeek)
    val outDays = (inDays + month.lengthOfMonth()).let { totalDays ->
        val endOfRow = if (totalDays % 7 != 0) 7 - (totalDays % 7) else 0
        val endOfGrid = if (outDateStyle == OutDateStyle.EndOfRow) 0 else run {
            val weekOfMonthField = WeekFields.of(firstDayOfWeek, 1).weekOfMonth()
            val weeksInMonth = month.atEndOfMonth().get(weekOfMonthField)
            return@run (6 - weeksInMonth) * 7
        }
        return@let endOfRow + endOfGrid
    }
    return MonthData(month, inDays, outDays)
}

internal fun getBoxCalendarMonthData(
    startMonth: YearMonth,
    offset: Int,
    firstDayOfWeek: DayOfWeek
): MonthData {
    val month = startMonth.plusMonths(offset.toLong())
    val firstDay = month.atStartOfMonth()
    val inDays = if (offset == 0) {
        firstDayOfWeek.daysUntil(firstDay.dayOfWeek)
    } else {
        -firstDay.dayOfWeek.daysUntil(firstDayOfWeek)
    }
    val outDays = (inDays + month.lengthOfMonth()).let { totalDays ->
        if (totalDays % 7 != 0) 7 - (totalDays % 7) else 0
    }
    return MonthData(month, inDays, outDays)
}