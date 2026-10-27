package com.example.qa

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class AlertaService : Service() {

    private val db = FirebaseFirestore.getInstance()
    private var primeiraCarga = true

    override fun onCreate() {
        super.onCreate()
        criarCanalNotificacao()
        iniciarComoForeground()
        escutarAlertasEmSegundoPlano()
    }

    private fun verificarSePodeNotificar(): Boolean {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("notificacoes_liberadas", false)
    }

    private fun iniciarComoForeground() {
        val notificationIntent = Intent(this, HistoricoActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, "ALERTA_CHANNEL_ID")
            .setContentTitle("Monitoramento de Alertas Ativo")
            .setContentText("O app está pronto para receber emergências.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun escutarAlertasEmSegundoPlano() {
        db.collection("alertas")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    android.util.Log.e("AlertaService", "Erro no Firestore: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (!verificarSePodeNotificar()) {
                    return@addSnapshotListener
                }

                val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                val ultimoIdProcessado = prefs.getString("ultimo_alerta_id", "")

                if (snapshots != null) {
                    for (dc in snapshots.documentChanges) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            val docId = dc.document.id

                            if (docId == ultimoIdProcessado) {
                                continue
                            }

                            val disp = dc.document.getString("dispositivo")
                                ?: dc.document.getString("device")
                                ?: "Dispositivo"
                            val st = dc.document.getString("status") ?: "Emergência"

                            if (primeiraCarga) {
                                prefs.edit().putString("ultimo_alerta_id", docId).apply()
                            } else {
                                dispararNotificacaoBackground(disp, st)
                                prefs.edit().putString("ultimo_alerta_id", docId).apply()
                            }
                        }
                    }
                    primeiraCarga = false
                }
            }
    }

    private fun dispararNotificacaoBackground(dispositivo: String, status: String) {
        val intent = Intent(this, HistoricoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "ALERTA_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alerta!: $status")
            .setContentText("Dispositivo: $dispositivo acionou emergência.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ALERTA_CHANNEL_ID",
                "Canal de Alertas",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}