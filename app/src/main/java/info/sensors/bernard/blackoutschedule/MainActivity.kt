package info.sensors.bernard.blackoutschedule

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

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
        val sharedPreferencesForLanguage = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val language = sharedPreferencesForLanguage.getString("My_Lang", "deff")
        if (language != "deff"){
            setLocale(this, language!!)
        }
        setContentView(R.layout.activity_main)

        val click = MediaPlayer.create(this, R.raw.button_sound)
        val serviceIntent = Intent(this, MusicService::class.java)

        startService(serviceIntent)
        bindMusicService()

        val sharedPreferencesForMusic = getSharedPreferences("SoundState", Context.MODE_PRIVATE)
        val sharedPreferencesBrightness = getSharedPreferences("Brightness", Context.MODE_PRIVATE)
        val clickSound = sharedPreferencesForMusic.getFloat("buttonSound", 1f)

        click.setVolume(clickSound, clickSound)
        val sharedPreferencesForFade = getSharedPreferences("Fade", Context.MODE_PRIVATE)

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
        val buttonAccount: ImageButton = findViewById(R.id.accountButton)

        val textData: TextView = findViewById(R.id.textView2)





        val currentDate = Calendar.getInstance().time
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val formattedDate = formatter.format(currentDate)
        textData.text = formattedDate


        buttonQuest.setOnClickListener {
            click.start()
            click.seekTo(0)
            goToNewActivity = true
            val intent = Intent(this, AccountActivity::class.java)
            startActivity(intent)
            // TODO тут мав би бути код для підказки
        }

        buttonSettings.setOnClickListener {
            click.start()
            click.seekTo(0)
            goToNewActivity = true
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            when(sharedPreferencesForFade.getInt("fadeStatus", 50)){
                in 21..40 -> overridePendingTransition(R.anim.fade_in_realy_slow, R.anim.fade_out_realy_slow)
                in 41..60 -> overridePendingTransition(R.anim.fade_in_slow, R.anim.fade_out_slow)
                in 61..80 -> overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                in 81..100 -> overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
            }
            finish()
        }
        buttonAccount.setOnClickListener {
            click.start()
            click.seekTo(0)
            goToNewActivity = true
            val intent = Intent(this, AccountActivity::class.java)
            startActivity(intent)
            when(sharedPreferencesForFade.getInt("fadeStatus", 50)){
                in 21..40 -> overridePendingTransition(R.anim.fade_in_realy_slow, R.anim.fade_out_realy_slow)
                in 41..60 -> overridePendingTransition(R.anim.fade_in_slow, R.anim.fade_out_slow)
                in 61..80 -> overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                in 81..100 -> overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
            }
            finish()
        }
        buttonFirstGroup.setOnClickListener {
            click.start()
            click.seekTo(0)
            buttonFirstGroup.alpha = 1f
            buttonSecondGroup.alpha = 0.5f
            buttonThirdGroup.alpha = 0.5f
            // TODO тут мав би бути код для вибору 1 групи
            setRegionNewData("12", "Група 1.1")
            changeUserRegion("Група 1.1")

        }

        buttonSecondGroup.setOnClickListener {
            click.start()
            click.seekTo(0)
            buttonFirstGroup.alpha = 0.5f
            buttonSecondGroup.alpha = 1f
            buttonThirdGroup.alpha = 0.5f
            // TODO тут мав би бути код для вибору 2 групи
            setRegionNewData("12", "Група 2.1")
            changeUserRegion("Група 2.1")
        }

        buttonThirdGroup.setOnClickListener {
            click.start()
            click.seekTo(0)
            buttonFirstGroup.alpha = 0.5f
            buttonSecondGroup.alpha = 0.5f
            buttonThirdGroup.alpha = 1f
            // TODO тут мав би бути код для вибору 3 групи
            setRegionNewData("12", "Група 3.1")
            changeUserRegion("Група 3.1")

        }



        getUserRegion()

        //setRegionNewData()

        hideUi()
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(100, TimeUnit.SECONDS)
        .writeTimeout(100, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS)
        .build()
    fun getUserRegion(groupCount: Int = 0){
        val sharedPreferencesForUser = getSharedPreferences("CurrentUser", Context.MODE_PRIVATE)
        val currentUsrID = sharedPreferencesForUser.getString("userData", " ")

        if (currentUsrID != null) {
            checkUserRegion(currentUsrID) { response ->
                response?.let {
                    try {
                        Log.d("TAG", "getUserRegion: $response")

                        val jsonResponse = JSONObject(it)

                        var groups = jsonResponse.getJSONArray("selected_regions")

                        val reversedGroups = JSONArray()
                        for (i in groups.length() - 1 downTo 0) {
                            reversedGroups.put(groups.getJSONObject(i))
                        }

                        groups = reversedGroups
                        Log.d("TAG", "getUserRegion: $groups")


                        val groupData = groups.getJSONObject(groupCount)
                        val groupName = groupData.getString("group_name")
                        val groupId = groupData.getInt("region_id")
                        Log.d("TAG", "getUserRegion: $groupName,   $groupId")
                        setRegionNewData(groupId.toString(), groupName)

                    } catch (e: Exception) {
                        // Handle JSON parsing exceptions
                        e.printStackTrace()
                        Log.e("TAG", "Failed to parse JSON response")
                    }



                } ?: run {
                    getUserRegion(groupCount)
                    Log.e("TAG", "Received null response")
                }
            }
        }
    }
    fun setRegionNewData(region: String = "12", group: String = "Група 1.1"){
        getAllGroups() { response ->
            response?.let {
                try {
                    val jsonResponse = JSONObject(it)
                    val regionData = jsonResponse.getJSONObject(region)
                    val groups = regionData.getJSONArray("groups")
                    val regionName = regionData.getString("region")
                    Log.d("TAG", "Response message: ${groups}")
                    Log.d("TAG", "Response message: ${regionName}")
                    for (i in 0 until groups.length()) {
                        val groupData = groups.getJSONObject(i)
                        val groupName = groupData.getString("group")
                        val scheduleId = groupData.getInt("schedule_id")
                        val scheduleImage = groupData.getString("schedule_image")

                        Log.d("TAG", "Group Name: $groupName, Schedule ID: $scheduleId, Schedule Image: $scheduleImage")

                        if (groupName == group) {
                            // Do something with the specific group
                            Log.d("TAG", "Found group: $groupName with Schedule Image: $scheduleImage")


                            val textRegionForReplace: TextView = findViewById(R.id.textView3)
                            textRegionForReplace.text =  getString(R.string.Львівська)

                            val imageForReplace: ImageView = findViewById(R.id.imageView9)
                            Picasso.get()
                                .load("http://34.159.225.88/photo/$scheduleImage")
                                .fit()
                                .placeholder(R.drawable.black_button_delete_region)  // Опціонально: зображення-заповнювач, поки завантажується фото
                                .error(R.drawable.black_button_delete_region)  // Опціонально: зображення для показу у випадку помилки
                                .into(imageForReplace)

                            break
                        }
                    }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("TAG", "Failed to parse JSON response")
                    }
            } ?: run {
                Log.e("TAG", "Received null response")
                setRegionNewData(region, group)
            }
        }
    }
    fun changeUserRegion(region: String  = "Група 1.1"){
        val sharedPreferencesForUser = getSharedPreferences("CurrentUser", Context.MODE_PRIVATE)
        val currentUsrID = sharedPreferencesForUser.getString("userData", " ")
        val currentSelectedRegionCount = sharedPreferencesForUser.getInt("selectedRegionCount", 0)

        if (currentUsrID != null) {
            checkUserRegion(currentUsrID) { response ->
                response?.let {
                    try {
                        Log.d("TAG", "getUserRegion: $response")

                        val jsonResponse = JSONObject(it)

                        var groups = jsonResponse.getJSONArray("selected_regions")

                        val reversedGroups = JSONArray()
                        for (i in groups.length() - 1 downTo 0) {
                            reversedGroups.put(groups.getJSONObject(i))
                        }

                        groups = reversedGroups
                        Log.d("TAG", "getUserRegion: $groups")


                        val groupData = groups.getJSONObject(currentSelectedRegionCount)
                        val groupName = groupData.getString("group_name")
                        val groupId = groupData.getInt("region_id")
                        Log.d("TAG", "getUserRegionuytre: |$currentUsrID|\t|$groupId|\t|$groupName|\t|$region|")

                        changeUserRegion(currentUsrID, groupId.toString(), groupName, region) {response ->

                        }


                    } catch (e: Exception) {
                        // Handle JSON parsing exceptions
                        e.printStackTrace()
                        Log.e("TAG", "Failed to parse JSON response")
                    }



                } ?: run {
                    changeUserRegion(region)
                    Log.e("TAG", "Received null response")
                }
            }
        }





    }
    fun getAllGroups(callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = doInBackground()
            withContext(Dispatchers.Main) {
                callback(response)
            }
        }
    }
    fun checkUserRegion(userId: String, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = doInBackground2(userId)
            withContext(Dispatchers.Main) {
                callback(response)
            }
        }
    }
    fun changeUserRegion(userId: String, regionId: String, currentGroup: String, newGroup: String, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = doInBackground3(userId, regionId, currentGroup, newGroup)
            withContext(Dispatchers.Main) {
                callback(response)
            }
        }
    }

    private suspend fun doInBackground(): String? {
        //val json = JSONObject().apply {}
        //val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url("http://34.159.225.88/Get_all_groups_regions")
            .build()
        return try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d("TAG", "doInBackground: ALl Works")
                response.body?.string()
            } else {
                Log.d("TAG", "doInBackground: ALl Works not in the right direction")
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("TAG", "doInBackground: ALl Not Works")
            null
        }
    }
    private suspend fun doInBackground2(userId: String): String? {
        val json = JSONObject().apply {
            put("user_id", userId)
        }
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url("http://34.159.225.88/Check_user_region") // Replace with the actual URL of your server
            .post(body)
            .build()
        return try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d("TAG", "doInBackground2: doInBackground2 Works")
                response.body?.string()
            } else {
                Log.d("TAG", "doInBackground2: ALl Works not in the right direction")
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("TAG", "doInBackground2: ALl Not Works")
            null
        }
    }
    private suspend fun doInBackground3(userId: String, regionId: String, currentGroup: String, newGroup: String): String? {
        val json = JSONObject().apply {
            put("user_id", userId)
            put("region_id", regionId)
            put("grup_current", currentGroup)
            put("grup_new", newGroup)
        }
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())

        Log.d("TAG", "doInBackground3: $json")

        val request = Request.Builder()
            .url("http://34.159.225.88/Change_user_region")
            .post(body)
            .build()
        return try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d("TAG", "doInBackground2: doInBackground2 Works")
                response.body?.string()
            } else {
                Log.d("TAG", "doInBackground2: ALl Works not in the right direction")
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("TAG", "doInBackground2: ALl Not Works")
            null
        }
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