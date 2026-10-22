package com.example.qa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConectarcuidadorActivity : AppCompatActivity() {

    private lateinit var codigocuidador: EditText
    private lateinit var conectarcuidador: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Permissão de notificação concedida!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notificações desativadas.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_conectarcuidador)

        verificarEpedirPermissaoNotificacao()

        codigocuidador = findViewById(R.id.codigocuidador)
        conectarcuidador = findViewById(R.id.conectarcuidador)

        var botconectarcuidador = true

        conectarcuidador.setOnClickListener {
            if (botconectarcuidador) {
                botconectarcuidador = false

                val texto = codigocuidador.text.toString()

                if (texto.isEmpty()) {
                    Toast.makeText(this, "Digite o Token no campo acima", Toast.LENGTH_SHORT).show()
                    botconectarcuidador = true
                    return@setOnClickListener
                }

                if (texto in GlobalData.tokens) {
                    Toast.makeText(
                        this@ConectarcuidadorActivity,
                        "Validando Token...",
                        Toast.LENGTH_SHORT
                    ).show()

                    lifecycleScope.launch {
                        delay(2500)
                        val telavinculo = Intent(this@ConectarcuidadorActivity, VinculoSucessoActivity::class.java)
                        startActivity(telavinculo)
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Validando Token...", Toast.LENGTH_SHORT).show()

                    lifecycleScope.launch {
                        delay(2500)
                        Toast.makeText(
                            this@ConectarcuidadorActivity,
                            "Token invalido",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    botconectarcuidador = true
                }, 5000)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun verificarEpedirPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}