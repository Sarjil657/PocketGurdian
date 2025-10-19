package com.pocketcorp.pocketguardian

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.SmsMessage
import com.google.android.gms.location.LocationServices

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras ?: return
        val pdus = bundle["pdus"] as Array<*>? ?: return
        val format = bundle["format"] as String?

        for (pdu in pdus) {
            val sms = SmsMessage.createFromPdu(pdu as ByteArray, format)
            val sender = sms.displayOriginatingAddress ?: continue
            val body = sms.displayMessageBody.trim().uppercase()
            if (body == "WHEREAREYOU") {
                val fused = LocationServices.getFusedLocationProviderClient(context)
                fused.lastLocation.addOnSuccessListener { loc ->
                    val reply = if (loc != null) {
                        "📍 Device location: https://www.google.com/maps/search/?api=1&query=${loc.latitude},${loc.longitude}"
                    } else {
                        "❗ Unable to get location right now."
                    }
                    try {
                        SmsManager.getDefault().sendTextMessage(sender, null, reply, null, null)
                    } catch (_: Exception) { /* ignore for prototype */ }
                }
            }
        }
    }
}
