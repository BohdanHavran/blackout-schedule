package info.sensors.bernard.blackoutschedule

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
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

class AccountActivity : AppCompatActivity() {
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

    private lateinit var regionsContainer: LinearLayout
    private var imageViews = mutableListOf<ImageView>()
    private var selectedRegions: MutableList<String> = mutableListOf()
    private val maxRegions = 7
    var regions = arrayOf<String>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)
        val click = MediaPlayer.create(this, R.raw.button_sound)
//        val serviceIntent = Intent(this, MusicService::class.java)
//        startService(serviceIntent)
        bindMusicService()
        val sharedPreferencesForMusic = getSharedPreferences("SoundState", Context.MODE_PRIVATE)
        val clickSound = sharedPreferencesForMusic.getFloat("buttonSound", 1f)
        val sharedPreferencesForFade = getSharedPreferences("Fade", Context.MODE_PRIVATE)
        click.setVolume(clickSound, clickSound)

        val sharedPreferencesBrightness = getSharedPreferences("Brightness", Context.MODE_PRIVATE)
        if (sharedPreferencesBrightness.getBoolean("isUsingManualBrightness", false)){
            val brightnessValue = sharedPreferencesBrightness.getFloat("brightnessValue", 1f)
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightnessValue
            window.attributes = layoutParams
        }

        val inputPassword: EditText = findViewById(R.id.AccPasswordInput)

        val showPasswordButton: ImageButton = findViewById(R.id.AccShowPasswordButton)
        val hidePasswordButton: ImageButton = findViewById(R.id.AccHidePasswordButton)

        showPasswordButton.setOnClickListener {
            showPasswordButton.isVisible = false
            hidePasswordButton.isVisible = true

            inputPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        }

        hidePasswordButton.setOnClickListener {
            hidePasswordButton.isVisible = false
            showPasswordButton.isVisible = true

            inputPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
        }


        val addRegionButton: ImageButton = findViewById(R.id.AccEditRegionButton)

        val sharedPreferencesForUser = getSharedPreferences("CurrentUser", Context.MODE_PRIVATE)
        val currentUsrID = sharedPreferencesForUser.getString("userData", " ")
        val currentUsrEmail = sharedPreferencesForUser.getString("userEmail", " ")
        val currentUsrPassword = sharedPreferencesForUser.getString("userPassword", " ")
        val currentUsrRegionCount = sharedPreferencesForUser.getInt("userRegionCount", 0)

        val inputEmail: EditText = findViewById(R.id.AccEmailInput)

        inputEmail.setText(currentUsrEmail)
        inputPassword.setText(currentUsrPassword)

        val spinner: Spinner = findViewById(R.id.AccSelectRegionSpinner)
        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, regions)
        spinner.adapter = arrayAdapter

        getRegions { response ->
            response?.let {
                Log.d("DEBUG", "Starting getRegions request")
                try {
                    // Parse the response string into a JSONObject
                    val jsonResponse = JSONObject(it)
                    val regionsList = mutableListOf<String>()

                    // Iterate over the keys of the JSONObject
                    val keys = jsonResponse.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val regionObject = jsonResponse.getJSONObject(key)
                        val region = regionObject.getString("region")
                        regionsList.add(region)
                    }

                    // Convert the MutableList to an array
                    regions = regionsList.toTypedArray()

                    addRegionButton.isEnabled = regions.isNotEmpty()

                    // Update the spinner's adapter
                    runOnUiThread {
                        val newArrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, regions)
                        spinner.adapter = newArrayAdapter
                    }

                } catch (e: Exception) {
                    // Handle JSON parsing exceptions
                    e.printStackTrace()
                    Log.e("TAG", "Failed to parse JSON response")
                }
            } ?: run {
                // Handle the case where the response is null
                Log.e("TAG", "Received null response")
            }
        }

        getUserRegion(currentUsrID) { response ->
            response?.let {
                try {
                    // Parse the response string into a JSONObject
                    val jsonResponse = JSONObject(it)
                    val selectedRegions = jsonResponse.getJSONArray("selected_regions")

                    // Iterate over the JSONArray of selected regions
                    for (i in 0 until selectedRegions.length()) {
                        val regionObject = selectedRegions.getJSONObject(i)
                        val regionName = regionObject.getString("region_name")
                        addRegionView(regionName)
                    }

                } catch (e: Exception) {
                    // Handle JSON parsing exceptions
                    e.printStackTrace()
                    Log.e("TAG", "Failed to parse JSON response")
                }
            } ?: run {
                // Handle the case where the response is null
                Log.e("TAG", "Received null response")
            }
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Toast.makeText(applicationContext, "Ви вибрали область " + regions[position], Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }

        regionsContainer = findViewById(R.id.AccRegionsContainer)

        addRegionButton.setOnClickListener {
            val selectedRegion = spinner.selectedItem.toString()
            Log.d("AccountActivity", "Обрані регіони: $selectedRegion")

            if (selectedRegions.size < maxRegions && !selectedRegions.contains(selectedRegion)) {
                selectedRegions.add(selectedRegion)

                Log.d("Region Modify", "Try add: addRegion(2, ${1 + regions.indexOf(selectedRegion)})")

                addRegion(currentUsrID, 1 + regions.indexOf(selectedRegion)) { response ->
                    Log.d("TAG", "onCreate: $response")
                    response?.let {
                        try {

                        } catch (e: Exception) {
                            // Handle JSON parsing exceptions
                            e.printStackTrace()
                            Log.e("TAG", "Failed to parse JSON response")
                        }

                    } ?: run {
                        // Handle the case where the response is null
                        Log.e("TAG", "Received null response")
                    }
                }

                val newCurrentUsrRegionCount = sharedPreferencesForUser.getInt("userRegionCount", 0)
                val editor = sharedPreferencesForUser.edit()
                editor.putInt("userRegionCount", newCurrentUsrRegionCount + 1)
                editor.apply()


                addRegionView(selectedRegion)

            } else {
                Toast.makeText(this, "Ви не можете добавити більше областей або ця область вже добавлена", Toast.LENGTH_SHORT).show()
            }
        }
        val scrollView: ScrollView = findViewById(R.id.ScrollView)
        var myX = 0f
        var myY = 0f

        var myButtonX = 0f
        var myButtonY = 0f

        var myTextX = 0f
        var myTextY = 0f

        val backgroundActive: ImageView = findViewById(R.id.regionImageViewActive)
        val textActive: TextView = findViewById(R.id.regionTextViewActive)
        val deleteActive: ImageButton = findViewById(R.id.deleteRegionButtonActive)

        lateinit var regionImageView: ImageView
        lateinit var regionButtonDelete: ImageButton
        lateinit var regionText: TextView

        var isRegionImageViewInitialized = false
        var isRegionButtonDeleteInitialized = false
        var isRegionTextInitialized = false

        scrollView.setOnTouchListener(View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {

                    Log.d("tag", "React imageView!")

                    imageViews.clear()

                    val pointX = event.rawX
                    val pointY = event.rawY

                    // Отримання прокрутки ScrollView
                    val scrollX = scrollView.scrollX
                    val scrollY = scrollView.scrollY

                    // Врахування прокрутки
                    val img1Collision = GameObject(
                        (pointX + scrollX).toInt(), (pointY + scrollY).toInt(),
                        45, 45
                    )

                    var collisionCheck = false

                    for (i in 0 until regionsContainer.childCount) {
                        Log.d("tag", "Processing child at index: $i")

                        val regionView = regionsContainer.getChildAt(i)

                        regionImageView = regionView.findViewById<ImageView>(R.id.regionImageView)
                        regionButtonDelete = regionView.findViewById<ImageButton>(R.id.deleteRegionButton)
                        regionText = regionView.findViewById<TextView>(R.id.regionTextView)
                        isRegionImageViewInitialized = true
                        isRegionButtonDeleteInitialized = true
                        isRegionTextInitialized = true

                        // Отримання глобальних координат для regionImageView
                        val location = IntArray(2)
                        val locationButton = IntArray(2)
                        val locationText = IntArray(2)

                        regionImageView.getLocationOnScreen(location)
                        regionButtonDelete.getLocationOnScreen(locationButton)
                        regionText.getLocationOnScreen(locationText)

                        val img2Collision = GameObject(
                            location[0], location[1],
                            regionImageView.width, regionImageView.height
                        )

                        Log.d("tag", "Coord: $pointX $pointY | ${location[0]} ${location[1]} | scroll: $scrollX $scrollY")
                        Log.d("tag", "img1Collision: $img1Collision")
                        Log.d("tag", "img2Collision: $img2Collision")
                        Log.d("tag", "regionImageView: $regionImageView")

                        collisionCheck = isCollidingW(img1Collision, img2Collision)

                        if (collisionCheck) {
                            imageViews.add(regionImageView)

                            backgroundActive.x = location[0].toFloat()
                            backgroundActive.y = location[1].toFloat()

                            textActive.x = locationText[0].toFloat()
                            textActive.y = locationText[1].toFloat()

                            deleteActive.x = locationButton[0].toFloat()
                            deleteActive.y = locationButton[1].toFloat()

                            myX = backgroundActive.x - event.rawX
                            myY = backgroundActive.y - event.rawY

                            myButtonX = deleteActive.x - event.rawX
                            myButtonY = deleteActive.y - event.rawY

                            myTextX = textActive.x - event.rawX
                            myTextY = textActive.y - event.rawY

                            Log.d("tag", "Added imageView: $regionImageView")

//                            oldX = location[0].toFloat()
//                            oldY = location[1].toFloat()

                            textActive.text = regionText.text

                            regionImageView.isVisible = false
                            regionButtonDelete.isVisible = false
                            regionText.isVisible = false

                            backgroundActive.isVisible = true
                            textActive.isVisible = true
                            deleteActive.isVisible = true

                            break  // Exit loop after the first collision is found
                        }
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    // Currently commented out logic for moving the image

                    textActive.animate()
                        .x(event.rawX + myTextX)
                        .y(event.rawY + myTextY)
                        .setDuration(0)
                        .start()

                    deleteActive.animate()
                        .x(event.rawX + myButtonX)
                        .y(event.rawY + myButtonY)
                        .setDuration(0)
                        .start()

                    backgroundActive.animate()
                        .x(event.rawX + myX)
                        .y(event.rawY + myY)
                        .setDuration(0)
                        .start()

                }
                MotionEvent.ACTION_UP -> {

                    if (isRegionImageViewInitialized && isRegionTextInitialized && isRegionButtonDeleteInitialized) {
                        val dropX = event.rawX
                        val dropY = event.rawY

                        var newIndex = -1
                        for (i in 0 until regionsContainer.childCount) {
                            val regionView = regionsContainer.getChildAt(i)
                            val location = IntArray(2)
                            regionView.getLocationOnScreen(location)

                            val rect = Rect(
                                location[0], location[1],
                                location[0] + regionView.width,
                                location[1] + regionView.height
                            )

                            if (rect.contains(dropX.toInt(), dropY.toInt())) {
                                newIndex = i
                                break
                            }

                        }

                        regionsContainer.removeViewAt(regionsContainer.indexOfChild(regionImageView.parent as View))
                        regionsContainer.addView(regionImageView.parent as View, newIndex)

                        regionImageView.isVisible = true
                        regionButtonDelete.isVisible = true
                        regionText.isVisible = true
                    }

                    backgroundActive.isVisible = false
                    textActive.isVisible = false
                    deleteActive.isVisible = false

                }
                else -> return@OnTouchListener false
            }
            true
        })

        regionsContainer.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                updateSelectedRegionsOrder()
                Log.d("AccountActivity", "Regions List: $selectedRegions")
            }

            override fun onChildViewRemoved(parent: View?, child: View?) {
                updateSelectedRegionsOrder()
                Log.d("AccountActivity", "Regions List: $selectedRegions")
            }
        })

        val backButton: ImageButton = findViewById(R.id.AccBackButton)

        backButton.setOnClickListener{
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

        val exitButton: ImageButton = findViewById(R.id.AccExitButton)

        exitButton.setOnClickListener {
            sharedPreferencesForUser.edit().apply {
                remove("userData")
                remove("userEmail")
                remove("userRegionCount")
                remove("CurrentSelectedRegion")
                apply()
            }
            Log.d("AccountActivity", "sharedPreferences after clear, id: $currentUsrID | count: $currentUsrRegionCount")

            goToNewActivity = true
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        }

        val editEmailButton: ImageButton = findViewById(R.id.editEmailButton)
        val editor = sharedPreferencesForUser.edit()

        editEmailButton.setOnClickListener{
            val email = inputEmail.text.toString()
            if (email == sharedPreferencesForUser.getString("userEmail", " ")) {
                Toast.makeText(this, "Ви не можете використовувати таку саму пошту", Toast.LENGTH_SHORT).show()
            } else {
                changeEmail(currentUsrID, email) { response ->
                    response?.let {
                        try {

                        } catch (e: Exception) {
                            // Handle JSON parsing exceptions
                            e.printStackTrace()
                            Log.e("TAG", "Failed to parse JSON response")
                        }

                        Toast.makeText(
                            this,
                            "Ви успішно змінили електронну пошту",
                            Toast.LENGTH_SHORT
                        ).show()
                        editor.putString("userEmail", email)
                        editor.apply()

                    } ?: run {
                        // Handle the case where the response is null
                        Log.e("TAG", "Received null response")
                    }
                }
            }
        }

        val editPasswordButton: ImageButton = findViewById(R.id.AccEditPasswordButton)

        editPasswordButton.setOnClickListener{
            val password = inputPassword.text.toString()
            if (password == sharedPreferencesForUser.getString("userPassword", " ")) {
                Toast.makeText(this, "Ви не можете використовувати такий самий пароль", Toast.LENGTH_SHORT).show()
            } else {
                changePassword(currentUsrID, password) { response ->
                    response?.let {
                        try {

                        } catch (e: Exception) {
                            // Handle JSON parsing exceptions
                            e.printStackTrace()
                            Log.e("TAG", "Failed to parse JSON response")
                        }

                        Toast.makeText(this, "Ви успішно змінили пароль", Toast.LENGTH_SHORT).show()
                        editor.putString("userPassword", password)
                        editor.apply()

                    } ?: run {
                        // Handle the case where the response is null
                        Log.e("TAG", "Received null response")
                    }
                }
            }
        }



        hideUi()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(100, TimeUnit.SECONDS)
        .writeTimeout(100, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS)
        .build()

    fun getRegions(callback: (String?) -> Unit) { // email: String, password: String,
        CoroutineScope(Dispatchers.IO).launch {
            val response = doInBackground()
            withContext(Dispatchers.Main) {
                callback(response)
            }
        }
    }

    private suspend fun doInBackground(): String? { // email: String, password: String

        val request = Request.Builder()
            .url("http://34.159.225.88/Get_all_groups_regions") // Replace with the actual URL of your server
            .build()

        return try {
            val response: Response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Log.d("TAG", "Regions doInBackground: ALl Works")
                response.body?.string()
            } else {
                Log.d("TAG", "Regions doInBackground: ALl Works not in the right direction")
                null
            }

        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("TAG", "Regions doInBackground: ALl Not Works")
            null
        }
    }

    fun getUserRegion(userId: String?, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = doInBackgroundGetUserRegion(userId)
            withContext(Dispatchers.Main) {
                callback(response)
            }
        }
    }

    private suspend fun doInBackgroundGetUserRegion(userId: String?): String? {
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
            response.use {
                if (response.isSuccessful) {
                    Log.d("TAG", "doInBackground (Check Region): ALl Works")
                    response.body?.string()
                } else {
                    Log.d(
                        "TAG",
                        "doInBackground (Check Region): ALl Works not in the right direction"
                    )
                    null
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("TAG", "doInBackground (Check Region): ALl Not Works")
            null
        }
    }

    fun addRegion(userId: String?, regionId: Int, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = doInBackgroundAddRegion(userId, regionId)
            withContext(Dispatchers.Main) {
                callback(response)
            }
        }
    }

    private suspend fun doInBackgroundAddRegion(userId: String?, regionId: Int): String? {
        val json = JSONObject().apply {
            put("user_id", userId)
            put("region_id", regionId)
        }

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url("http://34.159.225.88/Add_user_region") // Replace with the actual URL of your server
            .post(body)
            .build()

        return try {
            val response: Response = client.newCall(request).execute()
            response.use {
                if (response.isSuccessful) {
                    Log.d("TAG", "doInBackground (Add Region): ALl Works")
                    response.body?.string()
                } else {
                    Log.d(
                        "TAG",
                        "doInBackground (Add Region): ALl Works not in the right direction"
                    )
                    null
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("TAG", "doInBackground (Add Region): ALl Not Works")
            null
        }
    }

    fun changePassword(userId: String?, newPassword: String, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = doInBackgroundChangePassword(userId, newPassword)
            withContext(Dispatchers.Main) {
                callback(response)
            }
        }
    }

    private suspend fun doInBackgroundChangePassword(userId: String?, newPassword: String): String? {
        val json = JSONObject().apply {
            put("user_id", userId)
            put("new_password", newPassword)
        }

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url("http://34.159.225.88/Change_user_pass") // Replace with the actual URL of your server
            .post(body)
            .build()

        return try {
            val response: Response = client.newCall(request).execute()
            response.use {
                if (response.isSuccessful) {
                    Log.d("TAG", "doInBackground (Add Region): ALl Works")
                    response.body?.string()
                } else {
                    Log.d(
                        "TAG",
                        "doInBackground (Add Region): ALl Works not in the right direction"
                    )
                    null
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("TAG", "doInBackground (Add Region): ALl Not Works")
            null
        }
    }

    fun changeEmail(userId: String?, newEmail: String, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = doInBackgroundChangeEmail(userId, newEmail)
            withContext(Dispatchers.Main) {
                callback(response)
            }
        }
    }

    private suspend fun doInBackgroundChangeEmail(userId: String?, newEmail: String): String? {
        val json = JSONObject().apply {
            put("user_id", userId)
            put("new_mail", newEmail)
        }

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url("http://34.159.225.88/Change_user_mail") // Replace with the actual URL of your server
            .post(body)
            .build()

        return try {
            val response: Response = client.newCall(request).execute()
            response.use {
                if (response.isSuccessful) {
                    Log.d("TAG", "doInBackground (Add Region): ALl Works")
                    response.body?.string()
                } else {
                    Log.d(
                        "TAG",
                        "doInBackground (Add Region): ALl Works not in the right direction"
                    )
                    null
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("TAG", "doInBackground (Add Region): ALl Not Works")
            null
        }
    }


    fun removeRegion(userId: Int, regionId: Int, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = doInBackgroundRemoveRegion(userId, regionId)
            withContext(Dispatchers.Main) {
                callback(response)
            }
        }
    }

    private suspend fun doInBackgroundRemoveRegion(userId: Int, regionId: Int): String? {
        val json = JSONObject().apply {
            put("user_id", userId)
            put("region_id", regionId)
        }

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url("http://34.159.225.88/Remove_user_region") // Replace with the actual URL of your server
            .post(body)
            .build()

        return try {
            val response: Response = client.newCall(request).execute()
            response.use {
                if (response.isSuccessful) {
                    Log.d("TAG", "doInBackground (Remove Region): ALl Works")
                    response.body?.string()
                } else {
                    Log.d(
                        "TAG",
                        "doInBackground (Remove Region): ALl Works not in the right direction"
                    )
                    null
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("TAG", "doInBackground (Remove Region): ALl Not Works")
            null
        }
    }

    fun updateSelectedRegionsOrder() {
        selectedRegions.clear()
        for (i in 0 until regionsContainer.childCount) {
            val regionView = regionsContainer.getChildAt(i)
            val regionTextView = regionView.findViewById<TextView>(R.id.regionTextView)
            selectedRegions.add(regionTextView.text.toString())
        }
    }
    private fun addRegionView(region: String) {
        try {

            val regionView = LayoutInflater.from(this).inflate(R.layout.region_item, regionsContainer, false)
            val regionTextView = regionView.findViewById<TextView>(R.id.regionTextView)
            val deleteButton = regionView.findViewById<ImageButton>(R.id.deleteRegionButton)

            val sharedPreferencesForUser = getSharedPreferences("CurrentUser", Context.MODE_PRIVATE)
            val currentUsrID = sharedPreferencesForUser.getString("userData", " ")

            regionTextView.text = region
            regionsContainer.addView(regionView)

            deleteButton.setOnClickListener {
                regionsContainer.removeView(regionView)
                selectedRegions.remove(region)

                Log.d("Region Modify", "Try remove: removeRegion(2, ${1 + regions.indexOf(region)})")

                removeRegion(currentUsrID.toString().toInt(), 1 + regions.indexOf(region)) { response ->
                    response?.let {
                        try {

                        } catch (e: Exception) {
                            // Handle JSON parsing exceptions
                            e.printStackTrace()
                            Log.e("TAG", "Failed to parse JSON response")
                        }
                    } ?: run {
                        // Handle the case where the response is null
                        Log.e("TAG", "Received null response")
                    }
                }

                val newCurrentUsrRegionCount = sharedPreferencesForUser.getInt("userRegionCount", 0)
                val editor = sharedPreferencesForUser.edit()
                editor.putInt("userRegionCount", newCurrentUsrRegionCount - 1)
                editor.apply()
            }

        } catch (e: Exception) {
            Log.e("AccountActivity", "Помилка при додаванні регіону", e)
            Toast.makeText(this, "Помилка при додаванні регіону", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSelectedRegionImageViews(): List<ImageView> {
        val imageViews = mutableListOf<ImageView>()
        for (i in 0 until regionsContainer.childCount) {
            val regionView = regionsContainer.getChildAt(i)
            val regionImageView = regionView.findViewById<ImageView>(R.id.regionImageView)
            imageViews.add(regionImageView)
        }
        return imageViews
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

    class GameObject(var x: Int, var y: Int, var width: Int, var height: Int) { }

    private fun checkImageCollision(img1: ImageView, img2: ImageView) : Boolean {
        val img1Collision = GameObject(
            (img1.x).toInt(), (img1.y).toInt(),
            img1.width, img1.height)
        val img2Collision = GameObject(
            (img2.x).toInt(), (img2.y).toInt(),
            img2.width, img2.height)
        return isCollidingW(img1Collision, img2Collision)
    }

    private fun isCollidingW(object1: GameObject, object2: GameObject): Boolean {
        val rect1 = Rect(object1.x, object1.y, object1.x + object1.width, object1.y + object1.height)
        val rect2 = Rect(object2.x, object2.y, object2.x + object2.width, object2.y + object2.height)
        val isIntersecting = rect1.intersect(rect2)
        Log.d("tag", "Checking collision between $rect1 and $rect2: $isIntersecting")
        return isIntersecting
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