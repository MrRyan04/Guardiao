package com.example.qa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BotaoconectadoActivity : AppCompatActivity() {

    private lateinit var gerartoken: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_botaoconectado)

        gerartoken = findViewById(R.id.gerartoken)

        gerartoken.setOnClickListener {
            lifecycleScope.launch {
                delay(1800)
                val telatoken = Intent(this@BotaoconectadoActivity, TokenActivity::class.java)
                startActivity(telatoken)

                finish()
            }

            Toast.makeText(
                this@BotaoconectadoActivity,
                "Gerando Token",
                Toast.LENGTH_SHORT
            ).show()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}