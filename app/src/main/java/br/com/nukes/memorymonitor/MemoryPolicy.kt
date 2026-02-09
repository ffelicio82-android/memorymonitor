package br.com.nukes.memorymonitor

import android.content.Context
import android.util.Log
import br.com.nukes.memorymonitor.MemoryInvestigator.AppMemoryUsage

object MemoryPolicy {
    fun compareWithBaseline(current: List<AppMemoryUsage>) {
        val baseline = MemorySnapshotStore.getBaseline()

        current.forEach { app ->
            val old = baseline[app.packageName] ?: return@forEach
            val delta = app.memoryMb - old

            if (delta > 100) { // crescimento suspeito
                Log.w(
                    "MDM_MEMORY",
                    "⚠ Crescimento detectado: ${app.packageName} +${delta}MB"
                )
            }
        }
    }

    fun handleCritical(context: Context, topApps: List<AppMemoryUsage>) {
        topApps.forEach {
            Log.d("MemoryPolicy", "Top app: ${it.packageName} - ${it.memoryMb} MB")
        }
    }
}