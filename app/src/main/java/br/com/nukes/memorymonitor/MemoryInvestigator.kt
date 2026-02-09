package br.com.nukes.memorymonitor

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE

class MemoryInvestigator(private val context: Context) {
    private val activityManager: ActivityManager by lazy {
        context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
    }

    data class AppMemoryUsage(
        val packageName: String,
        val memoryMb: Int
    )

    fun getTopMemoryConsumingApps(limit: Int): List<AppMemoryUsage> {
        val process = activityManager.runningAppProcesses ?: return emptyList()

        val pidMap = process.associateBy { it.pid }
        val pids = pidMap.keys.toIntArray()
        val memoryInfos = activityManager.getProcessMemoryInfo(pids)

        val usages = mutableListOf<AppMemoryUsage>()

        memoryInfos.forEachIndexed { index, info ->
            val pid = pids[index]
            val processInfo = pidMap[pid] ?: return@forEachIndexed

            val memoryMb = info.totalPss / 1024

            processInfo.pkgList?.forEach { packageName ->
                usages.add(AppMemoryUsage(packageName, memoryMb))
            }
        }

        return usages
            .groupBy { it.packageName.trim() }
            .map { (packageName, appUsages) ->
                AppMemoryUsage(packageName, appUsages.sumOf { it.memoryMb })
            }
            .sortedByDescending { it.memoryMb }
            .take(limit)
    }
}