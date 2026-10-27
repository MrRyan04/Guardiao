package com.example.qa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlertaAdapter(
    private var listaAlertas: List<Alerta> = emptyList()
) : RecyclerView.Adapter<AlertaAdapter.AlertaViewHolder>() {

    class AlertaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textDispositivo: TextView = itemView.findViewById(R.id.textDispositivo)
        val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        val textCoordenadas: TextView = itemView.findViewById(R.id.textCoordenadas)
        val textHorario: TextView = itemView.findViewById(R.id.textHorario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alerta, parent, false)
        return AlertaViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertaViewHolder, position: Int) {
        val alerta = listaAlertas[position]

        holder.textDispositivo.text = "Dispositivo: ${alerta.dispositivo}"
        holder.textStatus.text = (alerta.status ?: "").uppercase()
        holder.textCoordenadas.text = "Lat: ${alerta.latitude} | Long: ${alerta.longitude}"
        holder.textHorario.text = alerta.horario
    }

    override fun getItemCount(): Int = listaAlertas.size

    fun atualizarLista(novaLista: List<Alerta>) {
        listaAlertas = novaLista
        notifyDataSetChanged()
    }
}