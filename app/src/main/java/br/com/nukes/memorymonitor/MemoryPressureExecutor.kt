package br.com.nukes.memorymonitor

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object MemoryPressureExecutor {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )

    fun dispatchPreparation(context: Context) {
        scope.launch {
            val memoryInvestigator = MemoryInvestigator(context)
            val topApps = memoryInvestigator.getTopMemoryConsumingApps(5)

            if (topApps.isEmpty()) return@launch

            MemoryPolicy.compareWithBaseline(topApps)
        }
    }

    fun dispatchCritical(context: Context) {
        scope.launch {
            val investigator = MemoryInvestigator(context)
            val topApps = investigator.getTopMemoryConsumingApps(3)
            MemoryPolicy.handleCritical(context, topApps)
        }
    }
}