package info.sensors.bernard.blackoutschedule

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {
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
        setContentView(R.layout.activity_main)
        val click = MediaPlayer.create(this, R.raw.button_sound)
        val serviceIntent = Intent(this, MusicService::class.java)
        startService(serviceIntent)
        bindMusicService()
        val sharedPreferencesForMusic = getSharedPreferences("SoundState", Context.MODE_PRIVATE)
        val clickSound = sharedPreferencesForMusic.getFloat("buttonSound", 1f)
        click.setVolume(clickSound, clickSound)

        val sharedPreferencesBrightness = getSharedPreferences("Brightness", Context.MODE_PRIVATE)
        if (sharedPreferencesBrightness.getBoolean("isUsingManualBrightness", false)){
            val brightnessValue = sharedPreferencesBrightness.getFloat("brightnessValue", 1f)
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightnessValue
            window.attributes = layoutParams
        }
        val buttonQuest: ImageButton = findViewById(R.id.questButton)
        val buttonSettings: ImageButton = findViewById(R.id.settingsButton)
        val buttonFirstGroup: ImageButton = findViewById(R.id.groupFirstButton)
        val buttonSecondGroup: ImageButton = findViewById(R.id.groupSecondButton)
        val buttonThirdGroup: ImageButton = findViewById(R.id.groupThirdButton)












        buttonQuest.setOnClickListener {
            click.start()
            click.seekTo(0)
            goToNewActivity = true
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            // TODO тут мав би бути код для підказки
        }
        buttonSettings.setOnClickListener {
            click.start()
            click.seekTo(0)
            goToNewActivity = true
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        buttonFirstGroup.setOnClickListener {
            click.start()
            click.seekTo(0)
            buttonFirstGroup.alpha = 1f
            buttonSecondGroup.alpha = 0.5f
            buttonThirdGroup.alpha = 0.5f
            // TODO тут мав би бути код для вибору 1 групи
        }
        buttonSecondGroup.setOnClickListener {
            click.start()
            click.seekTo(0)
            buttonFirstGroup.alpha = 0.5f
            buttonSecondGroup.alpha = 1f
            buttonThirdGroup.alpha = 0.5f
            // TODO тут мав би бути код для вибору 2 групи
        }
        buttonThirdGroup.setOnClickListener {
            click.start()
            click.seekTo(0)
            buttonFirstGroup.alpha = 0.5f
            buttonSecondGroup.alpha = 0.5f
            buttonThirdGroup.alpha = 1f
            // TODO тут мав би бути код для вибору 3 групи
        }


        hideUi()
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