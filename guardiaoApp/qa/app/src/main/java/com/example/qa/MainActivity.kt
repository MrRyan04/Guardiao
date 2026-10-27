package com.example.qa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var cuidador: CardView
    private lateinit var dependente: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val notificacoesLiberadas = sharedPreferences.getBoolean("notificacoes_liberadas", false)

        if (notificacoesLiberadas) {
            val intent = Intent(this, HistoricoActivity::class.java)
            startActivity(intent)
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        cuidador = findViewById(R.id.cardContainer2)
        dependente = findViewById(R.id.cardContainer)

        cuidador.setOnClickListener {
            val telaconectarcuidador = Intent(this, ConectarcuidadorActivity::class.java)
            startActivity(telaconectarcuidador)
            finish()
        }

        dependente.setOnClickListener {
            val telacolocarcodigo = Intent(this, ColocarcodigoActivity::class.java)
            startActivity(telacolocarcodigo)
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}