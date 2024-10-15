package com.example.geoble.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.geoble.R

class ServicesAdapter(context: Context, services: List<String>) : ArrayAdapter<String>(context, 0, services) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the data item for this position
        val service = getItem(position)

        // Check if an existing view is being reused, otherwise inflate the view
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_service, parent, false)

        // Lookup view for data population
        val serviceName: TextView = view.findViewById(R.id.service_name)

        // Populate the data into the template view using the data object
        serviceName.text = service

        // Return the completed view to render on screen
        return view
    }
}
