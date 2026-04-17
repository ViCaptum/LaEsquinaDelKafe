package com.brain.laesquinadelkafe.ui.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
