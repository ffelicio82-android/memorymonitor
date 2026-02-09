package br.com.nukes.memorymonitor

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE

object MemoryUtils {
    fun isMemoryAboveThreshold(context: Context, threshold: Int): Boolean {
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)

        val used = info.totalMem - info.availMem
        val percent = (used * 100) / info.totalMem

        return percent >= threshold
    }
}