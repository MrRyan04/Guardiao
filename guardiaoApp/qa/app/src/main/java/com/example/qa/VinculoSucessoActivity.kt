package com.example.qa

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class VinculoSucessoActivity : AppCompatActivity() {

    private lateinit var fecharapp2: Button
    private lateinit var historico: Button
    private val db = FirebaseFirestore.getInstance()
    private var primeiraCarga = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_vinculo_sucesso)

        ativarNotificacoesGlobais()

        criarCanalNotificacao()

        historico = findViewById(R.id.historico)
        fecharapp2 = findViewById(R.id.fecharapp2)

        historico.setOnClickListener {
            val telahistorico = Intent(this@VinculoSucessoActivity, HistoricoActivity::class.java)
            startActivity(telahistorico)
            finish()
        }

        fecharapp2.setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        escutarAlertasEmTempoReal()
    }

    private fun ativarNotificacoesGlobais() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("notificacoes_liberadas", true).apply()

        try {
            val serviceIntent = Intent(this, AlertaService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("VinculoSucessoActivity", "Erro ao iniciar AlertaService: ${e.message}", e)
        }
    }

    private fun verificarSePodeNotificar(): Boolean {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("notificacoes_liberadas", false)
    }

    private fun escutarAlertasEmTempoReal() {
        db.collection("alertas")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    android.util.Log.e("VinculoSucessoActivity", "Erro no Firestore: ${error.message}", error)
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
                                dispararNotificacaoLocal(disp, st)
                                prefs.edit().putString("ultimo_alerta_id", docId).apply()
                            }
                        }
                    }
                    primeiraCarga = false
                }
            }
    }

    private fun criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Canal de Alertas"
            val descriptionText = "Notificações de emergência em tempo real"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("ALERTA_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun dispararNotificacaoLocal(dispositivo: String, status: String) {
        if (!verificarSePodeNotificar()) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val builder = NotificationCompat.Builder(this, "ALERTA_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alerta!: $status")
            .setContentText("Dispositivo: $dispositivo acionou emergência.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}