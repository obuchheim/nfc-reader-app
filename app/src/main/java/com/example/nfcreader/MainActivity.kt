# app/src/main/java/com/example/nfcreader/MainActivity.kt
package com.example.nfcreader

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.nfcreader.databinding.ActivityMainBinding
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFilters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null
    private var detectedTag: Tag? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupNFC()
        setupUI()
        handleIntent(intent)
    }
    
    private fun setupNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        if (nfcAdapter == null) {
            updateStatus("❌ NFC wird auf diesem Gerät nicht unterstützt", android.R.color.holo_red_light)
            return
        }
        
        if (!nfcAdapter!!.isEnabled) {
            updateStatus("⚠️ NFC ist deaktiviert. Bitte aktivieren Sie NFC in den Einstellungen.", android.R.color.holo_orange_light)
            binding.enableNfcButton.visibility = android.view.View.VISIBLE
            return
        }
        
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        
        val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val tagFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        
        try {
            ndefFilter.addDataType("*/*")
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("Fehler beim Setup der Intent-Filter", e)
        }
        
        intentFilters = arrayOf(ndefFilter, techFilter, tagFilter)
        
        techLists = arrayOf(
            arrayOf(Ndef::class.java.name),
            arrayOf(NdefFormatable::class.java.name),
            arrayOf(IsoDep::class.java.name),
            arrayOf(NfcA::class.java.name),
            arrayOf(NfcB::class.java.name),
            arrayOf(NfcF::class.java.name),
            arrayOf(NfcV::class.java.name),
            arrayOf(MifareClassic::class.java.name),
            arrayOf(MifareUltralight::class.java.name)
        )
        
        updateStatus("✅ Bereit zum Scannen. Halten Sie ein NFC-Tag an das Gerät.", android.R.color.holo_green_light)
    }
    
    private fun setupUI() {
        binding.enableNfcButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }
        
        binding.readTagButton.setOnClickListener {
            detectedTag?.let { tag ->
                readTagInformation(tag)
            } ?: run {
                Toast.makeText(this, "Kein NFC-Tag erkannt", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.clearButton.setOnClickListener {
            clearResults()
        }
    }
    
    private fun updateStatus(message: String, colorRes: Int) {
        binding.statusText.text = message
        binding.statusText.setTextColor(ContextCompat.getColor(this, colorRes))
    }
    
    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists)
        
        if (nfcAdapter?.isEnabled == true) {
            updateStatus("✅ Bereit zum Scannen. Halten Sie ein NFC-Tag an das Gerät.", android.R.color.holo_green_light)
            binding.enableNfcButton.visibility = android.view.View.GONE
        }
    }
    
    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        val action = intent.action
        
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == action) {
            
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                detectedTag = it
                showTagDetectedDialog(it)
                vibratePhone()
            }
        }
    }
    
    private fun showTagDetectedDialog(tag: Tag) {
        val technologies = tag.techList.joinToString(", ") { 
            it.substringAfterLast('.') 
        }
        val tagId = bytesToHex(tag.id)
        
        AlertDialog.Builder(this)
            .setTitle("🎉 NFC-Tag erkannt!")
            .setMessage("Tag-ID: $tagId\n\nUnterstützte Technologien:\n$technologies\n\nMöchten Sie die Tag-Informationen auslesen?")
            .setPositiveButton("Ja, auslesen") { _, _ ->
                readTagInformation(tag)
            }
            .setNegativeButton("Abbrechen") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun readTagInformation(tag: Tag) {
        val stringBuilder = StringBuilder()
        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        
        stringBuilder.append("🏷️ NFC-TAG INFORMATIONEN\n")
        stringBuilder.append("═══════════════════════════\n\n")
        stringBuilder.append("⏰ Gescannt: $timestamp\n")
        stringBuilder.append("🆔 Tag-ID: ${bytesToHex(tag.id)}\n")
        stringBuilder.append("🔧 Technologien: ${tag.techList.joinToString(", ") { it.substringAfterLast('.') }}\n\n")
        
        // Detaillierte Technologie-Informationen
        for (tech in tag.techList) {
            when {
                tech.contains("Ndef") -> stringBuilder.append(readNdefTechnology(tag))
                tech.contains("NfcA") -> stringBuilder.append(readNfcA(tag))
                tech.contains("IsoDep") -> stringBuilder.append(readIsoDep(tag))
                tech.contains("MifareClassic") -> stringBuilder.append(readMifareClassic(tag))
                tech.contains("MifareUltralight") -> stringBuilder.append(readMifareUltralight(tag))
            }
        }
        
        binding.tagInfoText.text = stringBuilder.toString()
        binding.readTagButton.visibility = android.view.View.VISIBLE
        binding.clearButton.visibility = android.view.View.VISIBLE
    }
    
    private fun readNdefTechnology(tag: Tag): String {
        val result = StringBuilder()
        result.append("📄 NDEF INFORMATIONEN\n")
        result.append("────────────────────\n")
        
        try {
            val ndef = Ndef.get(tag)
            ndef?.let {
                it.connect()
                
                result.append("Typ: ${it.type}\n")
                result.append("Max. Größe: ${it.maxSize} Bytes\n")
                result.append("Verwendet: ${it.cachedNdefMessage?.toByteArray()?.size ?: 0} Bytes\n")
                result.append("Schreibbar: ${if (it.isWritable) "Ja" else "Nein"}\n")
                
                val ndefMessage = it.ndefMessage ?: it.cachedNdefMessage
                ndefMessage?.let { message ->
                    result.append("\n📝 NDEF Records (${message.records.size}):\n")
                    
                    message.records.forEachIndexed { index, record ->
                        result.append("  ${index + 1}. ${parseNdefPayload(record)}\n")
                    }
                }
                
                it.close()
            }
        } catch (e: Exception) {
            result.append("❌ Fehler: ${e.message}\n")
        }
        
        result.append("\n")
        return result.toString()
    }
    
    private fun readNfcA(tag: Tag): String {
        val result = StringBuilder()
        result.append("🅰️ NFC-A (ISO14443-A)\n")
        result.append("─────────────────────\n")
        
        try {
            val nfcA = NfcA.get(tag)
            nfcA?.let {
                it.connect()
                result.append("ATQA: ${bytesToHex(it.atqa)}\n")
                result.append("SAK: ${it.sak}\n")
                result.append("Max. Transfer: ${it.maxTransceiveLength} Bytes\n")
                it.close()
            }
        } catch (e: Exception) {
            result.append("❌ Fehler: ${e.message}\n")
        }
        
        result.append("\n")
        return result.toString()
    }
    
    private fun readIsoDep(tag: Tag): String {
        val result = StringBuilder()
        result.append("💳 ISO-DEP (Kreditkarte/Zahlung)\n")
        result.append("────────────────────────────────\n")
        
        try {
            val isoDep = IsoDep.get(tag)
            isoDep?.let {
                it.connect()
                result.append("Historical Bytes: ${bytesToHex(it.historicalBytes ?: byteArrayOf())}\n")
                result.append("HI Layer: ${bytesToHex(it.hiLayerResponse ?: byteArrayOf())}\n")
                result.append("Timeout: ${it.timeout} ms\n")
                it.close()
            }
        } catch (e: Exception) {
            result.append("❌ Fehler: ${e.message}\n")
        }
        
        result.append("\n")
        return result.toString()
    }
    
    private fun readMifareClassic(tag: Tag): String {
        val result = StringBuilder()
        result.append("🎫 MIFARE Classic\n")
        result.append("─────────────────\n")
        
        try {
            val mifareClassic = MifareClassic.get(tag)
            mifareClassic?.let {
                it.connect()
                result.append("Typ: ${getMifareClassicType(it.type)}\n")
                result.append("Größe: ${it.size} Bytes\n")
                result.append("Sektoren: ${it.sectorCount}\n")
                result.append("Blöcke: ${it.blockCount}\n")
                it.close()
            }
        } catch (e: Exception) {
            result.append("❌ Fehler: ${e.message}\n")
        }
        
        result.append("\n")
        return result.toString()
    }
    
    private fun readMifareUltralight(tag: Tag): String {
        val result = StringBuilder()
        result.append("🏷️ MIFARE Ultralight\n")
        result.append("─────────────────────\n")
        
        try {
            val mifareUltralight = MifareUltralight.get(tag)
            mifareUltralight?.let {
                it.connect()
                result.append("Typ: ${getMifareUltralightType(it.type)}\n")
                
                try {
                    val page0 = it.readPages(0)
                    result.append("UID: ${bytesToHex(page0.sliceArray(0..6))}\n")
                } catch (e: Exception) {
                    result.append("UID: Nicht lesbar\n")
                }
                
                it.close()
            }
        } catch (e: Exception) {
            result.append("❌ Fehler: ${e.message}\n")
        }
        
        result.append("\n")
        return result.toString()
    }
    
    private fun parseNdefPayload(record: NdefRecord): String {
        return when (record.tnf) {
            NdefRecord.TNF_WELL_KNOWN -> {
                when {
                    record.type.contentEquals(NdefRecord.RTD_TEXT) -> parseTextRecord(record)
                    record.type.contentEquals(NdefRecord.RTD_URI) -> parseUriRecord(record)
                    else -> "Unbekannter Well-Known Type"
                }
            }
            NdefRecord.TNF_MIME_MEDIA -> "MIME: ${String(record.type, StandardCharsets.UTF_8)}"
            NdefRecord.TNF_ABSOLUTE_URI -> "URI: ${String(record.payload, StandardCharsets.UTF_8)}"
            else -> "Typ: ${getTnfString(record.tnf)}"
        }
    }
    
    private fun parseTextRecord(record: NdefRecord): String {
        val payload = record.payload
        val textEncoding = if ((payload[0].toInt() and 0x80) == 0) "UTF-8" else "UTF-16"
        val languageCodeLength = payload[0].toInt() and 0x3F
        val text = String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, 
                         if (textEncoding == "UTF-8") StandardCharsets.UTF_8 else StandardCharsets.UTF_16)
        return "📝 Text: \"$text\""
    }
    
    private fun parseUriRecord(record: NdefRecord): String {
        val payload = record.payload
        val prefixCode = payload[0].toInt() and 0xFF
        val prefix = when (prefixCode) {
            0x01 -> "http://www."
            0x02 -> "https://www."
            0x03 -> "http://"
            0x04 -> "https://"
            0x05 -> "tel:"
            0x06 -> "mailto:"
            else -> ""
        }
        val uri = prefix + String(payload, 1, payload.size - 1, StandardCharsets.UTF_8)
        return "🔗 URI: $uri"
    }
    
    private fun getTnfString(tnf: Short): String {
        return when (tnf) {
            NdefRecord.TNF_EMPTY -> "Empty"
            NdefRecord.TNF_WELL_KNOWN -> "Well Known"
            NdefRecord.TNF_MIME_MEDIA -> "MIME Media"
            NdefRecord.TNF_ABSOLUTE_URI -> "Absolute URI"
            NdefRecord.TNF_EXTERNAL_TYPE -> "External Type"
            NdefRecord.TNF_UNKNOWN -> "Unknown"
            NdefRecord.TNF_UNCHANGED -> "Unchanged"
            else -> "Reserved"
        }
    }
    
    private fun getMifareClassicType(type: Int): String {
        return when (type) {
            MifareClassic.TYPE_CLASSIC -> "Classic"
            MifareClassic.TYPE_PLUS -> "Plus"
            MifareClassic.TYPE_PRO -> "Pro"
            else -> "Unknown"
        }
    }
    
    private fun getMifareUltralightType(type: Int): String {
        return when (type) {
            MifareUltralight.TYPE_ULTRALIGHT -> "Ultralight"
            MifareUltralight.TYPE_ULTRALIGHT_C -> "Ultralight C"
            else -> "Unknown"
        }
    }
    
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
    
    private fun vibratePhone() {
        try {
            @Suppress("DEPRECATION")
            (getSystemService(VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(100)
        } catch (e: Exception) {
            // Vibration nicht verfügbar
        }
    }
    
    private fun clearResults() {
        binding.tagInfoText.text = ""
        binding.readTagButton.visibility = android.view.View.GONE
        binding.clearButton.visibility = android.view.View.GONE
        detectedTag = null
    }
}
