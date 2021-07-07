import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mepod.MainActivity
import com.mepod.R
import com.mepod.RootActivity
import com.mepod.api.ApiClient
import com.mepod.api.ApiInterface
import com.mepod.databinding.ActivityComposePostBinding
import com.mepod.util.*
import com.mepod.util.GenUtil.hideKeyboard
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class ComposePostActivity : RootActivity() {
    lateinit var binding: ActivityComposePostBinding
    var contentFilePath: String? = null
    var imagePath: String? = null
    var serviceStarted = false
    val TAG = "ComposePostAct"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_compose_post)
        contentFilePath = intent.getStringExtra(IntentUtil.CONTENT_PATH)

        binding.description.clearFocus()
        binding.description.hideKeyboard()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver,
            IntentFilter(ComposeService.SERVICE_BR_NAME)
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            cancelReceiver,
            IntentFilter(ComposeService.SERVICE_CANCEL_COMPOSE)
        )

        setContent()
    }

    private fun setContent() {
        binding.thumbnailLayout.setOnClickListener {

            CropImage.activity()
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setMaxZoom(1)
                .setActivityMenuIconColor(R.color.white)
                .setAllowRotation(false)
                .setAllowFlipping(false)
                .setAllowCounterRotation(false)
                .start(this)
        }

        binding.back.setOnClickListener {
            onBackPressed()
        }

        binding.upload.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startComposeService()
                serviceStarted = true
            } else {
                updateProfilePic()
            }
        }

        binding.cancel.setOnClickListener {
            cancelService()
        }
    }

    fun cancelService() {
        val serviceIntent = Intent(this, ComposeService::class.java)
        stopService(serviceIntent)
        binding.uploadLayout.visibility = View.GONE
    }

    private fun startComposeService() {

        if (contentFilePath == null || imagePath == null) {
            GenUtil.showShortToast("Please add thumbnail..")
            return
        }
        if (binding.description.toString().isEmpty()) {
            GenUtil.showShortToast("Please add description..")
            return
        }
        if (binding.hashtag.toString().isEmpty()) {
            GenUtil.showShortToast("Please add hashtag..")
            return
        }
        binding.uploadLayout.visibility = View.VISIBLE

        val serviceIntent = Intent(this, ComposeService::class.java)
        serviceIntent.putExtra(IntentUtil.CONTENT_PATH, contentFilePath)
        serviceIntent.putExtra(IntentUtil.IMAGE_PATH, imagePath)
        serviceIntent.putExtra(IntentUtil.DESCRIPTION, binding.description.toString())
        serviceIntent.putExtra(IntentUtil.HASHTAG, imagePath)
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private fun updateProfilePic() {

        binding.uploadLayout.visibility = View.VISIBLE

        var requestBody: RequestBody? = null
        var multipartBody: MultipartBody.Part? = null
        imagePath?.let {
            if (!TextUtils.isEmpty(it)) {
                var file = ImageUtil.resizeImage(it)
                if (file == null) {
                    file = File(it)
                }

                requestBody = file.asRequestBody("image/png".toMediaTypeOrNull())
                multipartBody = MultipartBody.Part.createFormData("media", "avatar", requestBody!!)
            }
        }

        val contentPartBody = prepareFilePart("media")

        val array = arrayListOf<String>("asdf", "asdf", "asdf")
        val descriptionString = "hello, this is description speaking"
        val description = RequestBody.create(
            MultipartBody.FORM, descriptionString
        )
        val hashtags = RequestBody.create(
            MultipartBody.FORM, "[ \"#Mepod\", \"#MJ\" ]"
        )
        val apiService = ApiClient.getClient().create(ApiInterface::class.java)
        val call = apiService.composePost(
            PreferenceStore.prefs.accessToken().get(),
            description,
            multipartBody ?: return,
            contentPartBody ?: return,
            hashtags
        )
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    GenUtil.showShortToast("Post Uploaded successfully")
                    moveToHome()
                    Log.d(TAG, "ressssss-")
                } else {
                    binding.uploadLayout.visibility = View.GONE
                    GenUtil.showShortToast("There is some problem in uploading... Try again")
                    Log.d(TAG, "ressssss-${response.body()}")
                }

            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                binding.uploadLayout.visibility = View.GONE
                GenUtil.showShortToast("There is some problem in uploading... Try again")
            }

        })


    }

    fun prepareFilePart(partName: String): MultipartBody.Part {

        val fileUri = Uri.fromFile(File(contentFilePath))
        val file = FileUtils.getFile(this, fileUri);
        val requestFile = RequestBody.create(
            contentResolver.getType(fileUri)?.toMediaTypeOrNull(),
            file
        )
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);


    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            val status = intent.getBooleanExtra(IntentUtil.UPLOAD_STATUS, false)
            if (status) {
                GenUtil.showShortToast("Post Uploaded successfully")
                moveToHome()
            } else {
                binding.uploadLayout.visibility = View.GONE
                GenUtil.showShortToast("There is some problem in uploading... Try again")
            }
        }
    }

    private val cancelReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            cancelService()
        }
    }

    fun moveToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
        super.onDestroy()
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        if (serviceStarted && !isMyServiceRunning(ComposeService::class.java)) {
            moveToHome()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                val resultUri = result.uri
                Log.d(TAG, "onActivityResult: " + resultUri)

                imagePath = ImageUtil.getImagePathFromUri(this, resultUri)

                Log.d(TAG, "onActivityResult: " + imagePath)
                ImageUtil.loadImage(binding.thumbnail, File(imagePath))


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
                Log.d(TAG, ": " + error);
            }
        }

    }
}
