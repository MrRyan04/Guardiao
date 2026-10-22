package com.example.qa

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TokenActivity : AppCompatActivity() {

    private lateinit var token: TextView
    private lateinit var copiartoken: Button
    private lateinit var telainicial: Button
    private lateinit var fecharapp: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_token)

        var aleatorio = GlobalData.tokens.random()

        token = findViewById(R.id.token)
        copiartoken = findViewById(R.id.copiartoken)
        telainicial = findViewById(R.id.telainicial)
        fecharapp = findViewById(R.id.fecharapp)

        token.text = aleatorio

        copiartoken.setOnClickListener {
                val copiado = aleatorio

                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                val clip = ClipData.newPlainText("Token Copiado", copiado)

                clipboard.setPrimaryClip(clip)

                Toast.makeText(this, "Token Copiado para área de transferência", Toast.LENGTH_SHORT)
                    .show()
        }



        telainicial.setOnClickListener {
            val telainicial = Intent(this, MainActivity::class.java)
            startActivity(telainicial)

            finish()
        }

        fecharapp.setOnClickListener {
            finish()
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}