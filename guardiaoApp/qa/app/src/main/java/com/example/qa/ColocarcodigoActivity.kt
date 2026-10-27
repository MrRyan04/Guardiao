package com.example.qa

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ColocarcodigoActivity : AppCompatActivity() {

    private lateinit var codesp32: EditText
    private lateinit var conectarmeubotao: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_colocarcodigo)

        val codesp32 = findViewById<EditText>(R.id.codesp32)
        val conectarmeubotao = findViewById<Button>(R.id.conectarmeubotao)

        var botconect = true

        conectarmeubotao.setOnClickListener {

            if (botconect){
                botconect = false

           val codigo = codesp32.text.toString()

            if (codigo == ""){
                Toast.makeText(
                    this,
                    "Digite o código do botão no campo acima",
                    Toast.LENGTH_SHORT
                ).show()
                botconect = true
                return@setOnClickListener
            }

            if (codigo == GlobalData.esp32){
                Toast.makeText(
                    this,
                    "Buscando...",
                    Toast.LENGTH_SHORT
                ).show()

                lifecycleScope.launch {
                    delay(1500)
                    Toast.makeText(
                        this@ColocarcodigoActivity,
                        "Conectando...",
                        Toast.LENGTH_SHORT
                    ).show()

                    delay(3500)
                    val telabotaoconectado = Intent(this@ColocarcodigoActivity, BotaoconectadoActivity::class.java)
                    startActivity(telabotaoconectado)

                    finish()
                }
            } else {

                lifecycleScope.launch {
                    delay(1500)
                    Toast.makeText(
                        this@ColocarcodigoActivity,
                        "Código invalido",
                        Toast.LENGTH_SHORT
                    ).show()}

                Toast.makeText(
                    this,
                    "Buscando...",
                    Toast.LENGTH_SHORT
                ).show()
            }
                Handler(Looper.getMainLooper()).postDelayed({
                    botconect = true
                }, 5000)
            }
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}