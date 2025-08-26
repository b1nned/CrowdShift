package com.example.crowdshift

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.google.zxing.ResultPoint

class QRScannerActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private var isFlashOn = false

    companion object {
        const val EXTRA_QR_RESULT = "qr_result"

        fun startForResult(activity: Activity) {
            val intent = Intent(activity, QRScannerActivity::class.java)
            activity.startActivityForResult(intent, 1001)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        barcodeView = findViewById(R.id.barcode_scanner)

        setupScanner()
        setupButtons()
    }

    private fun setupScanner() {
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                // Got a result - return it
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_QR_RESULT, result.text)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {
                // Not needed
            }
        })
    }

    private fun setupButtons() {
        // Flash button
        findViewById<Button>(R.id.flash_toggle).setOnClickListener {
            toggleFlash()
        }

        // Close button
        findViewById<Button>(R.id.close_scanner).setOnClickListener {
            finish()
        }
    }

    private fun toggleFlash() {
        try {
            if (isFlashOn) {
                barcodeView.setTorchOff()
                isFlashOn = false
                Toast.makeText(this, "Flash OFF", Toast.LENGTH_SHORT).show()
            } else {
                barcodeView.setTorchOn()
                isFlashOn = true
                Toast.makeText(this, "Flash ON", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Flash not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}