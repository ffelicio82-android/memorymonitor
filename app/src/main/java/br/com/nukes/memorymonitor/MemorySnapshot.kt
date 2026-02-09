package br.com.nukes.memorymonitor

import android.content.Context
import android.util.Log

object MemorySnapshotStore {

    private var lastSnapshot: Map<String, Int> = emptyMap()

    fun capture(context: Context) {
        val memoryInvestigator = MemoryInvestigator(context)
        lastSnapshot = memoryInvestigator
            .getTopMemoryConsumingApps(limit = 10)
            .associate { it.packageName to it.memoryMb }

        Log.i("MDM_MEMORY", "📸 Baseline capturado: $lastSnapshot")
    }

    fun getBaseline(): Map<String, Int> = lastSnapshot
}