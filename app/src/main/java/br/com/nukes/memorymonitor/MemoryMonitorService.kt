package br.com.nukes.memorymonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class MemoryMonitorService : Service() {
    private var lastExecutionTime = 0L

    override fun onCreate() {
        super.onCreate()
        startForeground(
            1001,
            buildSilentNotification()
        )
        registerComponentCallbacks(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        when (level) {
            TRIM_MEMORY_UI_HIDDEN -> {
                /*
                O que significa:
                    UI não está mais visível
                    Sistema pede pra soltar recursos visuais

                O que fazer:
                    liberar bitmaps
                    cache de UI
                */
                Log.d("MemoryMonitorService", "TRIM_MEMORY_UI_HIDDEN")
            }
            TRIM_MEMORY_BACKGROUND -> {
                /*
                O que significa:
                    App está em background
                    Sistema está limpando memória

                O que fazer:
                    salvar estado
                    fechar recursos ociosos
                */
                Log.d("MemoryMonitorService", "TRIM_MEMORY_BACKGROUND")
            }
            TRIM_MEMORY_MODERATE -> {
                /*
                O que significa:
                    A memória está baixa e o sistema está matando apps em background

                O que fazer:
                    usar para log
                    salvar estado mínimo
                    garantir persistência
                    não iniciar investigação pesada
                    não disparar ranking
                */
            }
            TRIM_MEMORY_COMPLETE -> {
                /*
                O que significa:
                    Seu processo está na fila da morte
                    Última chance antes de kill

                O que fazer:
                    salvar tudo
                    parar qualquer coisa pesada
                    não iniciar novos jobs
                */
                Log.d("MemoryMonitorService", "TRIM_MEMORY_COMPLETE")
            }
            TRIM_MEMORY_RUNNING_MODERATE -> {
                /*
                O que significa:
                    O sistema começou a sentir pressão
                    Ainda está tudo “sob controle”
                    O LMK ainda não está agressivo

                O que fazer:
                    NÃO agir pesado
                    logar
                    marcar timestamp
                    preparar coleta futura
                */
                Log.d("MemoryMonitorService", "TRIM_MEMORY_RUNNING_MODERATE")

                if (canExecute().not()) return
                if (isAboveThreshold(MODERATE_THRESHOLD).not()) return

                MemorySnapshotStore.capture(applicationContext)
            }
            TRIM_MEMORY_RUNNING_LOW -> {
                /*
                O que significa:
                    A memória já está baixa
                    O sistema está se aproximando de matar processos

                O que fazer:
                    checar % de RAM
                    debounce
                    preparar coleta
                    ainda não matar nada
                */
                Log.d("MemoryMonitorService", "TRIM_MEMORY_RUNNING_LOW")
                if (canExecute().not()) return
                if (isAboveThreshold(LOW_THRESHOLD).not()) return

                MemoryPressureExecutor.dispatchPreparation(applicationContext)
            }
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                /*
                O que significa:
                    O sistema está à beira de cair
                    O Android vai matar processos
                    Pode causar ANR, freeze, lag

                O que fazer:
                    coletar métricas
                    identificar app vilão
                    enviar pro backend
                    aplicar policy (com cuidado)
                */
                Log.d("MemoryMonitorService", "TRIM_MEMORY_RUNNING_CRITICAL")
                if (canExecute().not()) return
                if (isAboveThreshold(CRITICAL_THRESHOLD).not()) return

                MemoryPressureExecutor.dispatchCritical(applicationContext)
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onDestroy() {
        unregisterComponentCallbacks(this)
        super.onDestroy()
    }

    private fun buildSilentNotification(): Notification {
        val channelId = "memory_monitor"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Memory Monitor",
                NotificationManager.IMPORTANCE_NONE
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Memory Monitor")
            .setContentText("Executando em segundo plano")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun canExecute(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastExecutionTime < DEBOUNCE_INTERVAL_MS) {
            return false
        }
        lastExecutionTime = now
        return true
    }

    private fun isAboveThreshold(threshold: Int): Boolean {
        return MemoryUtils.isMemoryAboveThreshold(
            applicationContext,
            threshold
        )
    }

    private fun snapshotBaseline() {
        MemorySnapshotStore.capture(applicationContext)
    }

    private fun prepareInvestigation() {
        MemoryPressureExecutor.dispatchPreparation(applicationContext)
    }

    private fun triggerFullInvestigation() {
        MemoryPressureExecutor.dispatchCritical(applicationContext)
    }

    companion object {
        private const val DEBOUNCE_INTERVAL_MS = 2 * 60 * 1000L

        private const val MODERATE_THRESHOLD = 60
        private const val LOW_THRESHOLD = 70
        private const val CRITICAL_THRESHOLD = 80
    }
}