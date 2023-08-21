package es.charles.nfcdemo

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.util.Log
import es.charles.nfccardreader.GenericCoroutinesTask
import es.charles.nfccardreader.enums.EmvCardScheme
import es.charles.nfccardreader.model.EmvCard
import es.charles.nfccardreader.parser.EmvParser
import es.charles.nfccardreader.utils.Provider
import org.apache.commons.lang3.StringUtils
import java.io.IOException

class GenericTask(private val mInterface: MyCardNfcInterface) : GenericCoroutinesTask<Intent, Void, EmvCard>("PRUEBA TASK") {

    private var mProvider: Provider? = Provider()
    private var mException = false
    var mCard: EmvCard? = null
    private var mTag: Tag? = null
    val CARD_UNKNOWN = EmvCardScheme.UNKNOWN.toString()
    val CARD_VISA = EmvCardScheme.VISA.toString()
    val CARD_NAB_VISA = EmvCardScheme.NAB_VISA.toString()
    val CARD_MASTER_CARD = EmvCardScheme.MASTER_CARD.toString()

    override fun doInBackground(params: Intent?): EmvCard? {
            mTag =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    params?.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                else
                    params?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        mTag?.let {
            val mIsoDep = IsoDep.get(mTag)
            if (mIsoDep == null) {
                mInterface.doNotMoveCardSoFast()
                return null
            }
            mException = false

            try {
                // Open connection
                mIsoDep.connect()

                mProvider?.setmTagCom(mIsoDep)

                val parser = EmvParser(mProvider, true)
                mCard = parser.readEmvCard()
            } catch (e: IOException) {
                mException = true
            }
        }

        return mCard
    }


    override fun onPreExecute() {
        super.onPreExecute()
        mInterface.startNfcReadCard()
    }

    override fun onPostExecute(result: EmvCard?) {
        super.onPostExecute(result)
        if (!mException) {
            mCard?.let { card ->
                if (StringUtils.isNotBlank(card.cardNumber)) {
                    if ( card.type.toString() == EmvCardScheme.UNKNOWN.toString()) {
                        Log.d("creditCardNfcReader", "UNKNOWN_CARD_MESS")
                    }
                    mInterface.cardIsReadyToRead(card)
                } else if (card.isNfcLocked) {
                    mInterface.cardWithLockedNfc()
                }
            } ?: kotlin.run { mInterface.unknownEmvCard()}

        } else  mInterface.doNotMoveCardSoFast()
        mInterface.finishNfcReadCard()
        clearAll()
    }

    override fun onCancelled(result: EmvCard?) {
        super.onCancelled(result)
        clearAll()
    }


    private fun clearAll() {
        mProvider = null
        mCard = null
        mTag = null
    }
}


interface MyCardNfcInterface {
    fun startNfcReadCard()
    fun cardIsReadyToRead(card: EmvCard)
    fun doNotMoveCardSoFast()
    fun unknownEmvCard()
    fun cardWithLockedNfc()
    fun finishNfcReadCard()
}