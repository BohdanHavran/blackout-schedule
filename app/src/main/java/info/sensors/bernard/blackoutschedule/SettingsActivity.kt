package info.sensors.bernard.blackoutschedule

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private var goToNewActivity = false
    private var musicService: MusicService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
            val sharedPreferences = getSharedPreferences("SoundState", Context.MODE_PRIVATE)
            if(sharedPreferences.getBoolean("boolKeyForSound", true)){
                onPlayButtonClick()
            }
            else{onPauseButtonClick()}
            val sharedPreferencesForMusic = getSharedPreferences("SoundState", Context.MODE_PRIVATE)
            val sound = sharedPreferencesForMusic.getInt("musicSound", 100)
            setVolume(sound)
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }
    private fun bindMusicService() {
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
    private fun onPlayButtonClick() {
        musicService?.play()
    }
    private fun onPauseButtonClick() {
        musicService?.pause()
    }
    fun setVolume(value: Int){
        musicService?.setMusicVolume(value)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferencesForLanguage = getSharedPreferences("Settings", Context.MODE_PRIVATE)
//        val language = sharedPreferencesForLanguage.getString("My_Lang", "en")
//        setLocale(this, language!!)
        val language = sharedPreferencesForLanguage.getString("My_Lang", "deff")
        if (language != "deff"){
            setLocale(this, language!!)
        }
        setContentView(R.layout.activity_settings)
        val click = MediaPlayer.create(this, R.raw.button_sound)
        bindMusicService()



        val imageBrightness: ImageView = findViewById(R.id.imageView2)

        val buttonQuest: ImageButton = findViewById(R.id.questButtonSettings)
        val buttonBack: ImageButton = findViewById(R.id.backButton)
        val buttonBrightness: ImageButton = findViewById(R.id.brightnesSwitchButton)
        val buttonUa: ImageButton = findViewById(R.id.uaFlagButton)
        val buttonPl: ImageButton = findViewById(R.id.plFlagButton)
        val buttonUsa: ImageButton = findViewById(R.id.usaFlagButton)

        val seekBarSound: SeekBar = findViewById(R.id.soundSeekBar)
        val seekBarMusic: SeekBar = findViewById(R.id.musicSeekBar)
        val seekBarBrightness: SeekBar = findViewById(R.id.brightnesSeekBar)
        val seekBarForFade: SeekBar = findViewById(R.id.smoothnesSeekBar)

        val sharedPreferencesForMusic = getSharedPreferences("SoundState", Context.MODE_PRIVATE)

        if (language == "en"){
            buttonUa.alpha = 0.5f
            buttonPl.alpha = 0.5f
            buttonUsa.alpha = 1f
        }
        else if (language == "pl"){
            buttonUa.alpha = 0.5f
            buttonPl.alpha = 1f
            buttonUsa.alpha = 0.5f
        }
        else{
            buttonUa.alpha = 1f
            buttonPl.alpha = 0.5f
            buttonUsa.alpha = 0.5f
        }
        val clickSound = sharedPreferencesForMusic.getFloat("buttonSound", 1f)
        click.setVolume(clickSound, clickSound)
        seekBarSound.progress = (clickSound * 100).toInt()
        seekBarSound.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val soundValue = seekBarSound.progress / 100f
                click.setVolume(soundValue, soundValue)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                click.start()
                click.seekTo(0)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val soundValue = seekBarSound.progress / 100f
                click.setVolume(soundValue, soundValue)
                val editor = sharedPreferencesForMusic.edit()
                editor.putFloat("buttonSound", soundValue)
                editor.apply()
            }
        })

        val sound = sharedPreferencesForMusic.getInt("musicSound", 100)
        setVolume(sound)
        seekBarMusic.progress = sound
        seekBarMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setVolume(seekBarMusic.progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                click.start()
                click.seekTo(0)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                setVolume(seekBarMusic.progress)
                val editor2 = sharedPreferencesForMusic.edit()
                editor2.putInt("musicSound", seekBarMusic.progress)
                editor2.apply()
            }
        })


        var sharedPreferencesBrightness = getSharedPreferences("Brightness", Context.MODE_PRIVATE)
        var brightnessSwitchState = sharedPreferencesBrightness.getBoolean("isUsingManualBrightness", false)
        if (!brightnessSwitchState) switchOff()
        else switchOn()
        buttonBrightness.setOnClickListener {
            brightnessSwitchState = sharedPreferencesBrightness.getBoolean("isUsingManualBrightness", false)
            brightnessSwitchState = if (brightnessSwitchState){
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val layoutParams = window.attributes
                layoutParams.screenBrightness = sharedPreferencesBrightness.getFloat("systemBrightnessValue", 1f)
                window.attributes = layoutParams
                switchOff(500)
            }
            else {
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val layoutParams = window.attributes
                val editor = sharedPreferencesBrightness.edit()
                editor.putFloat("systemBrightnessValue",  layoutParams.screenBrightness)
                editor.apply()
                sharedPreferencesBrightness = getSharedPreferences("Brightness", Context.MODE_PRIVATE)
                val brightnessValue = sharedPreferencesBrightness.getFloat("brightnessValue", 1f)
                layoutParams.screenBrightness = brightnessValue
                window.attributes = layoutParams
                switchOn(500)
            }
            val editor = sharedPreferencesBrightness.edit()
            editor.putBoolean("isUsingManualBrightness", brightnessSwitchState)
            editor.apply()
        }


        if (brightnessSwitchState){
            sharedPreferencesBrightness = getSharedPreferences("Brightness", Context.MODE_PRIVATE)
            val brightnessValue = sharedPreferencesBrightness.getFloat("brightnessValue", 1f)
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightnessValue
            window.attributes = layoutParams
        }

        seekBarBrightness.progress = (sharedPreferencesBrightness.getFloat("brightnessValue", 1f) * 100).toInt()
        seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val brightnessValue = seekBarBrightness.progress / 100f
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val layoutParams = window.attributes
                layoutParams.screenBrightness = brightnessValue
                window.attributes = layoutParams
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                click.start()
                click.seekTo(0)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val brightnessValue = seekBarBrightness.progress / 100f
                val editor = sharedPreferencesBrightness.edit()
                editor.putFloat("brightnessValue", brightnessValue)
                editor.apply()
            }
        })


        val sharedPreferencesForFade = getSharedPreferences("Fade", Context.MODE_PRIVATE)
        val fadeStatus = sharedPreferencesForFade.getInt("fadeStatus", 50)

        seekBarForFade.progress = fadeStatus
        seekBarForFade.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {


            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                click.start()
                click.seekTo(0)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val fadeNewStatus = seekBarForFade.progress


                val editor = sharedPreferencesForFade.edit()
                editor.putInt("fadeStatus", fadeNewStatus)
                editor.apply()

            }
        })



        buttonQuest.setOnClickListener {
            click.start()
            click.seekTo(0)
            // TODO тут мав би бути код для підказки в налаштуваннях
        }
        buttonBack.setOnClickListener {
            click.start()
            click.seekTo(0)
            goToNewActivity = true
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            when(sharedPreferencesForFade.getInt("fadeStatus", 50)){
                in 21..40 -> overridePendingTransition(R.anim.fade_in_realy_slow, R.anim.fade_out_realy_slow)
                in 41..60 -> overridePendingTransition(R.anim.fade_in_slow, R.anim.fade_out_slow)
                in 61..80 -> overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                in 81..100 -> overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
            }
            finish()
        }




        buttonUa.setOnClickListener {changeLanguage("uk") }
        buttonPl.setOnClickListener { changeLanguage("uk") }
        buttonUsa.setOnClickListener { changeLanguage("en") }



        hideUi()
    }
    private fun changeLanguage(language: String) {
        setLocale(this, language)
        saveLocale(language)
        startActivity(Intent(this, SettingsActivity::class.java))
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
    private fun setLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
    private fun saveLocale(language: String) {
        val sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("My_Lang", language)
        editor.apply()
    }
    private fun switchOn(animationDuration: Long = 1L): Boolean{
        val imageBrightness: ImageView = findViewById(R.id.imageView2)

        val seekBarBrightness: SeekBar = findViewById(R.id.brightnesSeekBar)

        val textBrightnessZero: TextView = findViewById(R.id.textView742)
        val textBrightnessHundred: TextView = findViewById(R.id.textView7244)

        val params = imageBrightness.layoutParams as ConstraintLayout.LayoutParams
        val animator = ValueAnimator.ofFloat(params.horizontalBias, 0.95f)
        animator.duration = animationDuration
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            params.horizontalBias = animatedValue
            imageBrightness.layoutParams = params
        }
        animator.start()

        val animator2 = ValueAnimator.ofFloat(seekBarBrightness.alpha, 1f)
        animator2.duration = animationDuration
        animator2.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float

            seekBarBrightness.alpha = animatedValue
            textBrightnessZero.alpha = animatedValue
            textBrightnessHundred.alpha = animatedValue
        }
        seekBarBrightness.isEnabled = true
        textBrightnessZero.isEnabled = true
        textBrightnessHundred.isEnabled = true
        animator2.start()

        return true
    }
    private fun switchOff(animationDuration: Long = 1L): Boolean{
        val imageBrightness: ImageView = findViewById(R.id.imageView2)

        val seekBarBrightness: SeekBar = findViewById(R.id.brightnesSeekBar)

        val textBrightnessZero: TextView = findViewById(R.id.textView742)
        val textBrightnessHundred: TextView = findViewById(R.id.textView7244)

        val params = imageBrightness.layoutParams as ConstraintLayout.LayoutParams
        val animator = ValueAnimator.ofFloat(params.horizontalBias, 0.05f)
        animator.duration = animationDuration
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            params.horizontalBias = animatedValue
            imageBrightness.layoutParams = params
        }
        animator.start()

        val animator2 = ValueAnimator.ofFloat(seekBarBrightness.alpha, 0.4f)
        animator2.duration = animationDuration
        animator2.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float

            seekBarBrightness.alpha = animatedValue
            textBrightnessZero.alpha = animatedValue
            textBrightnessHundred.alpha = animatedValue
        }
        seekBarBrightness.isEnabled = false
        textBrightnessZero.isEnabled = false
        textBrightnessHundred.isEnabled = false
        animator2.start()

        return false
    }

    override fun onPause() {
        super.onPause()
        hideUi()
        if(goToNewActivity) goToNewActivity = false
        else onPauseButtonClick()
    }
    override fun onResume() {
        super.onResume()
        hideUi()
        val sharedPreferences = getSharedPreferences("SoundState", Context.MODE_PRIVATE)
        if(sharedPreferences.getBoolean("boolKeyForSound", true))
            onPlayButtonClick()
    }
    private fun hideUi() {
        val uiOptions = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LOW_PROFILE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = ViewCompat.getWindowInsetsController(window.decorView)
            insetsController?.let {
                it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                it.hide(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            val decorView = window.decorView
            decorView.systemUiVisibility = uiOptions
        }
    }
}
