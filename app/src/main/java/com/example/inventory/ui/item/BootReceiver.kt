package com.example.inventory.ui.item


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Dispositivo reiniciado, restaurando alarmas...")

            // Recuperar alarmas almacenadas
            val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
            val alarmEntries = sharedPreferences.all

            alarmEntries.forEach { (title, timestamp) ->
                val triggerAtMillis = (timestamp as? Long) ?: return@forEach

                // Restaurar cada alarma
                setAlarm(context, triggerAtMillis, title)
            }
        }
    }
}

