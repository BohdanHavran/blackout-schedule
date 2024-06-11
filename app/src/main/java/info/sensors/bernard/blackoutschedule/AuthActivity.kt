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
import android.util.Patterns
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import androidx.core.view.isVisible


class AuthActivity : AppCompatActivity() {
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
        setContentView(R.layout.activity_auth)
        val click = MediaPlayer.create(this, R.raw.button_sound)
//        val serviceIntent = Intent(this, MusicService::class.java)
//        startService(serviceIntent)
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

        val buttonRegister: ImageButton = findViewById(R.id.registerButton)
        val buttonAuth: ImageButton = findViewById(R.id.authButton)

        val buttonCreateAccount: ImageButton = findViewById(R.id.createAccountButton)
        val buttonBackAuth: ImageButton = findViewById(R.id.backAuthButton)

        val inputLogin: EditText = findViewById(R.id.loginInput)
        val inputPassword: EditText = findViewById(R.id.passwordInput)

        val textButtonRegister: TextView = findViewById(R.id.registerButtonText)
        val textButtonAuth: TextView = findViewById(R.id.authButtonText)
        val textButtonCreateAccount: TextView = findViewById(R.id.createAccountButtonText)
        val textButtonBackAuth: TextView = findViewById(R.id.backAuthButtonText)



        val sharedPreferencesForUser = getSharedPreferences("CurrentUser", Context.MODE_PRIVATE)
        val currentUsrID = sharedPreferencesForUser.getString("userData", " ")
        val currentUsrRegionCount = sharedPreferencesForUser.getInt("userRegionCount", 0)
        if (currentUsrID != " "){
            goToNewActivity = true
            val intent = Intent(this, RegionsActivity::class.java)
            startActivity(intent)
            finish()
        }



        //-----
        val buttonSkip: ImageButton = findViewById(R.id.skipButton)
        buttonSkip.setOnClickListener {
            click.start()
            click.seekTo(0)
            goToNewActivity = true
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            val sharedPreferencesForFade = getSharedPreferences("Fade", Context.MODE_PRIVATE)
            when(sharedPreferencesForFade.getInt("fadeStatus", 50)){
                in 21..40 -> overridePendingTransition(R.anim.fade_in_realy_slow, R.anim.fade_out_realy_slow)
                in 41..60 -> overridePendingTransition(R.anim.fade_in_slow, R.anim.fade_out_slow)
                in 61..80 -> overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                in 81..100 -> overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
            }
            finish()
        }

        buttonAuth.setOnClickListener {
            val email = inputLogin.text.toString()
            val password = inputPassword.text.toString()

            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (password.isNotEmpty()) {
                    authenticate(email, password) { response ->
                        response?.let {
                            try {
                                val jsonResponse = JSONObject(it)
                                val message = jsonResponse.getString("message")
                                val role = jsonResponse.getString("role")
                                val userID = jsonResponse.getString("user_id")

                                Log.d("TAG", "Response message: $message, role: $role, id $userID")
                                if (role == "1") {
                                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                                    Toast.makeText(this, "Welcome Admin $email", Toast.LENGTH_SHORT).show()
                                }
                                else {
                                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                                    Toast.makeText(this, "Welcome $email", Toast.LENGTH_SHORT).show()
                                    if (userID.isNotEmpty()) {
                                        checkUserRegion(userID) { response ->
                                            response?.let {
                                                try {
                                                    val jsonResponse = JSONObject(it)
                                                    val message = jsonResponse.getString("region_count")
                                                    if (message.toInt() > 0){
                                                        val editor = sharedPreferencesForUser.edit()
                                                        editor.putString("userData", userID)
                                                        editor.putString("userEmail", email)
                                                        editor.putInt("userRegionCount", message.toInt())
                                                        editor.apply()
                                                        goToNewActivity = true
                                                        val intent = Intent(this, MainActivity::class.java)
                                                        startActivity(intent)
                                                        val sharedPreferencesForFade = getSharedPreferences("Fade", Context.MODE_PRIVATE)
                                                        when(sharedPreferencesForFade.getInt("fadeStatus", 50)){
                                                            in 21..40 -> overridePendingTransition(R.anim.fade_in_realy_slow, R.anim.fade_out_realy_slow)
                                                            in 41..60 -> overridePendingTransition(R.anim.fade_in_slow, R.anim.fade_out_slow)
                                                            in 61..80 -> overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                                                            in 81..100 -> overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                                                        }
                                                        finish()
                                                    }
                                                    else{
                                                        val editor = sharedPreferencesForUser.edit()
                                                        editor.putString("userData", userID)
                                                        editor.putString("userEmail", email)
                                                        editor.putInt("userRegionCount", message.toInt())
                                                        editor.apply()
                                                        goToNewActivity = true
                                                        val intent = Intent(this, RegionsActivity::class.java)
                                                        startActivity(intent)
                                                        val sharedPreferencesForFade = getSharedPreferences("Fade", Context.MODE_PRIVATE)
                                                        when(sharedPreferencesForFade.getInt("fadeStatus", 50)){
                                                            in 21..40 -> overridePendingTransition(R.anim.fade_in_realy_slow, R.anim.fade_out_realy_slow)
                                                            in 41..60 -> overridePendingTransition(R.anim.fade_in_slow, R.anim.fade_out_slow)
                                                            in 61..80 -> overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                                                            in 81..100 -> overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                                                        }
                                                        finish()
                                                        //TODO Перехід на вибір графіків
                                                        Log.d("TAG", "onCreate: Перехід на вибір графіків")
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Log.e("TAG", "Failed to parse JSON response")
                                                }
                                            } ?: run {
                                                Log.e("TAG", "Received null response")
                                            }
                                        }
                                    } else {
                                        inputPassword.error = "Error width user id"
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e("TAG", "Failed to parse JSON response")
                            }
                        } ?: run {
                            Log.e("TAG", "Received null response")
                        }
                    }
                } else {
                    inputPassword.error = "Empty fields are not allowed"
                }

            } else if (email.isEmpty()) {
                inputLogin.error = "Empty fields are not allowed"
            } else {
                inputLogin.error = "Please enter correct email"
            }
        }

        //-----


        buttonRegister.setOnClickListener {

            inputLogin.setHint("Створіть email")
            inputPassword.setHint("Створіть пароль")

//            buttonRegister.alpha = 0f
//            buttonAuth.alpha = 0f
//            textButtonRegister.alpha = 0f
//            textButtonAuth.alpha = 0f

            buttonCreateAccount.isVisible = true
            buttonBackAuth.isVisible = true
            textButtonCreateAccount.isVisible = true
            textButtonBackAuth.isVisible = true

        }

        buttonCreateAccount.setOnClickListener {
            val email = inputLogin.text.toString()
            val password = inputPassword.text.toString()
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (password.isNotEmpty()) {
                    createAccount(email, password) { response ->
                        Log.d("TAG", "onCreate: $response")
//                        response?.let {
//                            try {
//                                // Parse the response string into a JSONObject
//                                val jsonResponse = JSONObject(it)
//
//                                // Extract values for "message" and "role"
//                                val message = jsonResponse.getString("message")
//                                val role = jsonResponse.getString("role")
//
//                                // Log the extracted values
//                                Log.d("TAG", "Response message: $message, role: $role")
//
//                                // Show a Toast message with the response message
////                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//
//                                // Perform an action based on the role
//                                if (role == "1") {
//                                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//                                    Toast.makeText(this, "Welcome Admin $email", Toast.LENGTH_SHORT).show()
//                                    // Navigate to a different activity or perform a specific action for role 1
//                                    // Example: startActivity(Intent(this, Role1Activity::class.java))
//                                }
//                                else {
//                                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//                                    Toast.makeText(this, "Welcome $email", Toast.LENGTH_SHORT).show()
//                                }
//                            } catch (e: Exception) {
//                                // Handle JSON parsing exceptions
//                                e.printStackTrace()
//                                Log.e("TAG", "Failed to parse JSON response")
//                            }
//                        } ?: run {
//                            // Handle the case where the response is null
//                            Log.e("TAG", "Received null response")
//                        }
                    }
                } else {
                    inputPassword.error = "Empty fields are not allowed"
                }
            } else if (email.isEmpty()) {
                inputLogin.error = "Empty fields are not allowed"
            } else {
                inputLogin.error = "Please enter correct email"
            }
        }

        buttonBackAuth.setOnClickListener {
            buttonCreateAccount.isVisible = false
            buttonBackAuth.isVisible = false
            textButtonCreateAccount.isVisible = false
            textButtonBackAuth.isVisible = false
        }



        hideUi()
    }

//
//class AuthenticationTask {
    private val client = OkHttpClient.Builder()
        .connectTimeout(50, TimeUnit.SECONDS)
        .writeTimeout(50, TimeUnit.SECONDS)
        .readTimeout(50, TimeUnit.SECONDS)
        .build()
    fun createAccount(email: String, password: String, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = doInBackground1(email, password)
            withContext(Dispatchers.Main) {
                callback(response)
            }
        }
    }

    private suspend fun doInBackground1(email: String, password: String): String? {
        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
            put("role",0)
            put("name","user")
        }

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url("http://34.159.225.88/Create_account") // Replace with the actual URL of your server
            .post(body)
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
    fun authenticate(email: String, password: String, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = doInBackground(email, password)
            withContext(Dispatchers.Main) {
                callback(response)
            }
        }
    }

    private suspend fun doInBackground(email: String, password: String): String? {
        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url("http://34.159.225.88/Check_account") // Replace with the actual URL of your server
            .post(body)
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

    fun checkUserRegion(userId: String, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = doInBackground2(userId)
            withContext(Dispatchers.Main) {
                callback(response)
            }
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
//}
//


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