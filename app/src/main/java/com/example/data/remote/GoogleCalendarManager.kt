package com.example.data.remote

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.util.Calendar
import java.util.TimeZone

data class CalendarEventItem(
    val id: Long = 0,
    val title: String,
    val description: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isAllDay: Boolean = false,
    val recurrenceRule: String? = null // e.g. "FREQ=MONTHLY"
)

class GoogleCalendarManager(private val context: Context) {

    /**
     * Add a calendar event directly to the primary user calendar via CalendarContract.
     */
    fun addCalendarEvent(
        title: String,
        description: String,
        startTimeMillis: Long,
        endTimeMillis: Long = startTimeMillis + 3600000,
        isAllDay: Boolean = false,
        recurrenceRule: String? = null
    ): Result<Long> {
        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startTimeMillis)
                put(CalendarContract.Events.DTEND, endTimeMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.CALENDAR_ID, 1) // Primary calendar
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.ALL_DAY, if (isAllDay) 1 else 0)
                if (!recurrenceRule.isNullOrBlank()) {
                    put(CalendarContract.Events.RRULE, recurrenceRule)
                }
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri != null) {
                val eventId = uri.lastPathSegment?.toLongOrNull() ?: 1L
                Result.success(eventId)
            } else {
                // Fallback to launching system Intent if direct write permission isn't granted
                launchCalendarIntent(title, description, startTimeMillis, endTimeMillis)
                Result.success(0L)
            }
        } catch (e: Exception) {
            // Intent fallback
            launchCalendarIntent(title, description, startTimeMillis, endTimeMillis)
            Result.success(0L)
        }
    }

    /**
     * Launch Intent to open system Google Calendar event creation dialog.
     */
    fun launchCalendarIntent(
        title: String,
        description: String,
        startTimeMillis: Long,
        endTimeMillis: Long
    ) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Helper to get upcoming default financial reminder dates (e.g. 5th of next month for electricity bill).
     */
    fun getNextMonthDate(dayOfMonth: Int, hour: Int = 10, minute: Int = 0): Long {
        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.DAY_OF_MONTH) >= dayOfMonth) {
            calendar.add(Calendar.MONTH, 1)
        }
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        return calendar.timeInMillis
    }
}
