package info.sensors.bernard.blackoutschedule

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
        setContentView(R.layout.activity_settings)
        val click = MediaPlayer.create(this, R.raw.button_sound)
        bindMusicService()

//        val sharedPreferencesForBrigthness = getSharedPreferences("Brigthness", Context.MODE_PRIVATE)
//        val Brigthness = sharedPreferencesForBrigthness.getFloat("brigthnessValue", 1f)
//        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        val layoutParams = window.attributes
//        layoutParams.screenBrightness = Brigthness
//        window.attributes = layoutParams
        val imageBrightness: ImageView = findViewById(R.id.imageView2)

        val buttonQuest: ImageButton = findViewById(R.id.questButtonSettings)
        val buttonBack: ImageButton = findViewById(R.id.backButton)
        val buttonBrightness: ImageButton = findViewById(R.id.brightnesSwitchButton)

        val seekBarSound: SeekBar = findViewById(R.id.soundSeekBar)
        val seekBarMusic: SeekBar = findViewById(R.id.musicSeekBar)
        val seekBarBrightness: SeekBar = findViewById(R.id.brightnesSeekBar)

        val textBrightnessZero: TextView = findViewById(R.id.textView742)
        val textBrightnessHundred: TextView = findViewById(R.id.textView7244)

        val sharedPreferencesForMusic = getSharedPreferences("SoundState", Context.MODE_PRIVATE)

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


        val sharedPreferencesBrightness = getSharedPreferences("Brightness", Context.MODE_PRIVATE)
        var brightnessSwitchState = sharedPreferencesBrightness.getBoolean("isUsingManualBrightness", false)
        if (!brightnessSwitchState){
            val params = imageBrightness.layoutParams as ConstraintLayout.LayoutParams
            params.horizontalBias = 0.05f
            imageBrightness.layoutParams = params
            seekBarBrightness.alpha = 0.5f
            textBrightnessZero.alpha = 0.5f
            textBrightnessHundred.alpha = 0.5f
            seekBarBrightness.isEnabled = false
            textBrightnessZero.isEnabled = false
            textBrightnessHundred.isEnabled = false

        }
        else{
            val params = imageBrightness.layoutParams as ConstraintLayout.LayoutParams
            params.horizontalBias = 0.95f
            imageBrightness.layoutParams = params
            seekBarBrightness.alpha = 1f
            textBrightnessZero.alpha = 1f
            textBrightnessHundred.alpha = 1f
            seekBarBrightness.isEnabled = true
            textBrightnessZero.isEnabled = true
            textBrightnessHundred.isEnabled = true
        }
        buttonBrightness.setOnClickListener {
            brightnessSwitchState = sharedPreferencesBrightness.getBoolean("isUsingManualBrightness", false)
            brightnessSwitchState = if (brightnessSwitchState){
                switchOff()
                seekBarBrightness.alpha = 0.5f
                textBrightnessZero.alpha = 0.5f
                textBrightnessHundred.alpha = 0.5f
                seekBarBrightness.isEnabled = false
                textBrightnessZero.isEnabled = false
                textBrightnessHundred.isEnabled = false
                false
            }
            else{
                switchOn()
                seekBarBrightness.alpha = 1f
                textBrightnessZero.alpha = 1f
                textBrightnessHundred.alpha = 1f
                seekBarBrightness.isEnabled = true
                textBrightnessZero.isEnabled = true
                true
            }
            val editor = sharedPreferencesBrightness.edit()
            editor.putBoolean("isUsingManualBrightness", brightnessSwitchState)
            editor.apply()
        }







        buttonQuest.setOnClickListener {
            click.start()
            click.seekTo(0)
            // TODO тут мав би бути код для підказки в налаштуваннях
        }
        buttonBack.setOnClickListener {
            click.start()
            click.seekTo(0)
            goToNewActivity = true
            //val intent = Intent(this, MainActivity::class.java)
            //startActivity(intent)
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        hideUi()
    }

    private fun switchOn(animationDuration: Long = 1L){
        val imageBrightness: ImageView = findViewById(R.id.imageView2)

        val params = imageBrightness.layoutParams as ConstraintLayout.LayoutParams
        val animator = ValueAnimator.ofFloat(params.horizontalBias, 0.95f)
        animator.duration = animationDuration
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            params.horizontalBias = animatedValue
            imageBrightness.layoutParams = params
        }
        animator.start()


    }
    private fun switchOff(animationDuration: Long = 1L){
        val imageBrightness: ImageView = findViewById(R.id.imageView2)

        val params = imageBrightness.layoutParams as ConstraintLayout.LayoutParams
        val animator = ValueAnimator.ofFloat(params.horizontalBias, 0.05f)
        animator.duration = animationDuration
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            params.horizontalBias = animatedValue
            imageBrightness.layoutParams = params
        }
        animator.start()

//        val params2 = imageBrightness.layoutParams as ConstraintLayout.LayoutParams
//        val animator2 = ValueAnimator.ofFloat(params.horizontalBias, 0.05f)
//        animator2.duration = animationDuration
//        animator2.addUpdateListener { animation ->
//            val animatedValue = animation.animatedValue as Float
//        }
//        animator2.start()

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
