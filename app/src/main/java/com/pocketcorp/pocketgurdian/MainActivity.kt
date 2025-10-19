package com.pocketcorp.pocketguardian

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private val PERMISSION_REQUEST = 100
    private val PREFS = "pg_prefs"
    private val KEY_TRUSTED = "trusted_number"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val edtTrusted = findViewById<EditText>(R.id.edtTrustedNumber)
        val btnSave = findViewById<Button>(R.id.btnSaveNumber)
        val btnAlarm = findViewById<Button>(R.id.btnAlarm)
        val btnSendLoc = findViewById<Button>(R.id.btnSendLocation)

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer.isLooping = true

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        edtTrusted.setText(prefs.getString(KEY_TRUSTED, ""))

        btnSave.setOnClickListener {
            val num = edtTrusted.text.toString().trim()
            if (num.isBlank()) {
                Toast.makeText(this, "Enter a phone number", Toast.LENGTH_SHORT).show()
            } else {
                prefs.edit().putString(KEY_TRUSTED, num).apply()
                Toast.makeText(this, "Saved trusted number", Toast.LENGTH_SHORT).show()
            }
        }

        btnAlarm.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                btnAlarm.text = "Activate Alarm"
            } else {
                mediaPlayer.start()
                btnAlarm.text = "Stop Alarm"
            }
        }

        btnSendLoc.setOnClickListener {
            sendLocation()
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.SEND_SMS)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.RECEIVE_SMS)

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::mediaPlayer.isInitialized) {
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.release()
        }
    }

    private fun sendLocation() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val trusted = prefs.getString(KEY_TRUSTED, "") ?: ""
        if (trusted.isBlank()) {
            Toast.makeText(this, "Please save a trusted number first", Toast.LENGTH_SHORT).show()
            return
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                val msg = "📍 My location: https://www.google.com/maps/search/?api=1&query=${loc.latitude},${loc.longitude}"
                try {
                    SmsManager.getDefault().sendTextMessage(trusted, null, msg, null, null)
                    Toast.makeText(this, "Location sent!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "SMS failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Location unavailable. Try outdoors or enable GPS.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
