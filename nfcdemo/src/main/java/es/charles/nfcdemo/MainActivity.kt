package es.charles.nfcdemo

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import com.google.gson.GsonBuilder
import es.charles.nfccardreader.model.EmvCard
import es.charles.nfccardreader.utils.CardNfcUtils
import es.charles.nfcdemo.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var context: Context

    private var mNfcAdapter: NfcAdapter? = null
    private var mCardNfcUtils: CardNfcUtils? = null
    private var mIntentFromCreate: Boolean = false

    private var task: GenericTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        context = this
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context)

    }

    override fun onResume() {
        super.onResume()
        setupView()
        mIntentFromCreate = false
        if (mNfcAdapter != null) {
            mCardNfcUtils?.enableDispatch()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mNfcAdapter != null) {
            mCardNfcUtils?.disableDispatch()
        }
    }

    private fun setupView() = with(binding){
        lifecycleOwner = this@MainActivity
        contentCard.visibility = android.view.View.GONE
        contentProblemNfc.visibility = android.view.View.VISIBLE
        when (checkNFCEnable()) {
            true -> {
                mCardNfcUtils = CardNfcUtils(this@MainActivity)
                mIntentFromCreate = true

                lottieView.setAnimation(R.raw.read_card)
                lottieView.playAnimation()
                tvStatusNfc.text = "Acerca tu tarjeta a la parte posterior"
                btnActivarNfc.visibility = android.view.View.GONE
            }
            false -> {
                lottieView.setAnimation(R.raw.error_anim)
                lottieView.playAnimation()
                tvStatusNfc.text = "NFC esta desconectado"
                btnActivarNfc.apply {
                    visibility = android.view.View.VISIBLE
                    setOnClickListener { startActivity(Intent(android.provider.Settings.ACTION_NFC_SETTINGS)) }
                }
            }
            null -> tvStatusNfc.text = "NFC NO DISPONIBLE EN EL TERMINAL"


        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        mNfcAdapter?.let { nfcAdapter ->
            if (checkNFCEnable() == true){
                // CREAR ASYNCTASK
                task = GenericTask(object : MyCardNfcInterface {
                    override fun startNfcReadCard() {

                    }

                    override fun cardIsReadyToRead(card: EmvCard) {
                        binding.apply {
                            contentCard.visibility = View.VISIBLE
                            contentProblemNfc.visibility = View.GONE
                            this.card = card
                            if (card.holderFirstname != null && card.holderLastname != null)
                                tvNombre.text = "${card.holderLastname}, ${card.holderFirstname}"
                            else tvNombre.text = "Informacion no disponible"
                            when (card.type.toString()) {
                                task?.CARD_VISA ,
                                task?.CARD_NAB_VISA-> {
                                    imgLogotipo.setImageResource(R.drawable.ic_visa)
                                }
                                task?.CARD_MASTER_CARD -> {
                                    imgLogotipo.setImageResource(R.drawable.ic_master_card)
                                }
                                else -> {
                                    //UNKNOWN
                                }
                            }
                        }
                        Log.w("DATA CARD", GsonBuilder().setPrettyPrinting().create().toJson(card))
                    }

                    override fun doNotMoveCardSoFast() {
                        longShowSnackBar(binding.tvStatusNfc,"Por favor, no mueva la tarjeta, ¡inténtelo de nuevo!")
                    }

                    override fun unknownEmvCard() {
                        longShowSnackBar(binding.tvStatusNfc,"No se ha podido reconocer la tarjeta")
                    }

                    override fun cardWithLockedNfc() {
                        longShowSnackBar(binding.tvStatusNfc,"NFC está bloqueado en esta tarjeta.")
                    }

                    override fun finishNfcReadCard() {
                        task = null
                    }
                }).also { t -> t.execute(Dispatchers.IO, intent) }
            }
        }

    }
    private fun checkNFCEnable(): Boolean? = if (mNfcAdapter == null) null else mNfcAdapter?.isEnabled == true


}