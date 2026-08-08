package com.gameperf.assistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InstalledAppAdapter(
    private val apps: List<AppEntry>,
    private val selectedPackages: MutableSet<String>,
    private val onSelectionChanged: (Set<String>) -> Unit
) : RecyclerView.Adapter<InstalledAppAdapter.ViewHolder>() {

    data class AppEntry(val packageName: String, val label: String, val isSystemApp: Boolean)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.textAppLabel)
        val subtitle: TextView = view.findViewById(R.id.textAppPackage)
        val checkBox: CheckBox = view.findViewById(R.id.checkboxSelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_installed_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = apps[position]
        holder.label.text = entry.label
        holder.subtitle.text = entry.packageName
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedPackages.contains(entry.packageName)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedPackages.add(entry.packageName)
            else selectedPackages.remove(entry.packageName)
            onSelectionChanged(selectedPackages.toSet())
        }
    }

    override fun getItemCount(): Int = apps.size
}
