package com.example.qa

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class HistoricoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var alertaAdapter: AlertaAdapter
    private val db = FirebaseFirestore.getInstance()
    private var primeiraCarga = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historico)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        criarCanalNotificacao()

        recyclerView = findViewById(R.id.recyclerViewHistorico)
        recyclerView.layoutManager = LinearLayoutManager(this)

        alertaAdapter = AlertaAdapter(emptyList())
        recyclerView.adapter = alertaAdapter

        escutarAlertasEmTempoReal()
    }

    private fun escutarAlertasEmTempoReal() {
        db.collection("alertas")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Erro Firestore: ${error.message}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("HistoricoActivity", "Erro no Firestore: ${error.message}", error)
                    return@addSnapshotListener
                }

                val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                val ultimoIdProcessado = prefs.getString("ultimo_alerta_id", "")

                if (snapshots != null) {
                    val listaAlertas = mutableListOf<Alerta>()

                    for (documento in snapshots) {
                        try {
                            val dispositivo = documento.getString("dispositivo")
                                ?: documento.getString("device")
                                ?: "Dispositivo"

                            val status = documento.getString("status") ?: "Alerta"

                            val lat = documento.getDouble("latitude")
                                ?: documento.getLong("latitude")?.toDouble()
                                ?: documento.getString("latitude")?.toDoubleOrNull()
                                ?: 0.0

                            val long = documento.getDouble("longitude")
                                ?: documento.getLong("longitude")?.toDouble()
                                ?: documento.getString("longitude")?.toDoubleOrNull()
                                ?: 0.0

                            val rawHorario = documento.get("horario") ?: documento.get("timestamp")
                            val horario = when (rawHorario) {
                                is String -> rawHorario
                                is com.google.firebase.Timestamp -> {
                                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy - HH:mm", java.util.Locale.getDefault())
                                    sdf.format(rawHorario.toDate())
                                }
                                else -> rawHorario?.toString() ?: ""
                            }

                            val millis = extrairTimestampMillis(rawHorario)

                            val alerta = Alerta(
                                id = documento.id,
                                dispositivo = dispositivo,
                                status = status,
                                latitude = lat,
                                longitude = long,
                                horario = horario,
                                timestampMillis = millis
                            )
                            listaAlertas.add(alerta)
                        } catch (e: Exception) {
                            android.util.Log.e("HistoricoActivity", "Erro ao ler documento ${documento.id}", e)
                        }
                    }

                    for (dc in snapshots.documentChanges) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            val docId = dc.document.id

                            if (docId == ultimoIdProcessado) {
                                continue
                            }

                            val disp = dc.document.getString("dispositivo") ?: "Dispositivo"
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

                    listaAlertas.sortByDescending { it.timestampMillis }

                    if (listaAlertas.isEmpty()) {
                        Toast.makeText(this, "Nenhum alerta encontrado no banco", Toast.LENGTH_SHORT).show()
                    }

                    alertaAdapter.atualizarLista(listaAlertas)
                }
            }
    }

    private fun extrairTimestampMillis(rawHorario: Any?): Long {
        when (rawHorario) {
            is com.google.firebase.Timestamp -> return rawHorario.toDate().time
            is Long -> return rawHorario
            is Double -> return rawHorario.toLong()
            is String -> {
                val formatos = listOf(
                    "dd/MM/yyyy - HH:mm:ss",
                    "dd/MM/yyyy - HH:mm",
                    "dd/MM/yyyy HH:mm:ss",
                    "dd/MM/yyyy HH:mm",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss"
                )
                for (formato in formatos) {
                    try {
                        val sdf = java.text.SimpleDateFormat(formato, java.util.Locale.getDefault())
                        val data = sdf.parse(rawHorario)
                        if (data != null) return data.time
                    } catch (_: Exception) { }
                }
            }
        }
        return 0L
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