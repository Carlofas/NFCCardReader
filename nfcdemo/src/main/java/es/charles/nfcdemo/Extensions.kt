package es.charles.nfcdemo

import android.nfc.NfcAdapter
import android.view.View
import com.google.android.material.snackbar.Snackbar

fun formattedCardNumber(cardNumber: String?): String? {
    val div = " - "
    return cardNumber?.let { card ->
        card.substring(0, 4) + div +
                card.substring(4, 8) + div +
                card.substring(8, 12) + div + card.substring(12, 16)
    }
}

fun longShowSnackBar(view: View, message: String) {
    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
}