
class UploadAudioForegroundService : Service() {
    var imagePath: String? = null
    var contentFilePath: String? = null
    val TAG = "ComposeService"
    var description: String = ""
    var hashtag: String = ""
    var call: Call<Void>? = null

    override fun onCreate() {
        super.onCreate()
    }

    companion object {
        val SERVICE_BR_NAME = "compose-upload-event"
        val SERVICE_CANCEL_COMPOSE = "cancel_upload"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val input = intent.getStringExtra("inputExtra")

        imagePath = intent.getStringExtra(IntentUtil.IMAGE_PATH)
        contentFilePath = intent.getStringExtra(IntentUtil.CONTENT_PATH)
        description = intent.getStringExtra(IntentUtil.DESCRIPTION) ?: ""
        hashtag = intent.getStringExtra(IntentUtil.HASHTAG) ?: ""

        val broadcastIntent = Intent(this, CancelBroadcast::class.java)
        broadcastIntent.putExtra("toastMessage", message)
        val actionIntent = PendingIntent.getBroadcast(
            this,
            0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = Intent(SERVICE_BR_NAME)
        cancelIntent.putExtra(IntentUtil.UPLOAD_STATUS, false)
        val pIntent = PendingIntent.getBroadcast(this, 1, intent, 0)
        val notification: Notification = Notification.Builder(this, MyApplication.CHANNEL_ID)
            .setContentTitle("Uploading Post")
            .setContentText(input)
            .setProgress(100, 0, true)
            .setSmallIcon(R.drawable.ic_logo_mepod)
            .addAction(R.drawable.ic_icon_audio_clip, "Cancel", actionIntent)
            .build()
        startForeground(1, notification)

        sendPost()

        return START_NOT_STICKY
    }

    fun sendPost() {

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
        val descriptionString = description
        val description = RequestBody.create(
            MultipartBody.FORM, descriptionString
        )
        val hashtags = RequestBody.create(
            MultipartBody.FORM, "[ \"$hashtag\" ]"
        )
        val apiService = ApiClient.getClient().create(ApiInterface::class.java)
        call = apiService.composePost(
            PreferenceStore.prefs.accessToken().get(),
            description,
            multipartBody ?: return,
            contentPartBody ?: return,
            hashtags
        )

        call?.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
//                    GenUtil.showShortToast("success")
                    Log.d(TAG, "ressssss-")
                    sendMessage(true)
                    stopSelf()
                } else {
                    sendMessage(false)
//                    GenUtil.showShortToast("failure1111")
                    Log.d(TAG, "ressssss-${response.body()}")
                    stopSelf()
                }

            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                sendMessage(false)
//                GenUtil.showShortToast("failure")
                stopSelf()
            }

        })

    }

    fun prepareFilePart(partName: String): MultipartBody.Part {

        if (contentFilePath == null) {
            GenUtil.showShortToast("nullllll")
        }

        val fileUri = Uri.fromFile(File(contentFilePath))
        val file = FileUtils.getFile(this, fileUri);
        val requestFile = RequestBody.create(
            contentResolver.getType(fileUri)?.toMediaTypeOrNull(),
            file
        )
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);


    }

    private fun sendMessage(status: Boolean) {
        Log.d("sender", "Broadcasting message")
        val intent = Intent(SERVICE_BR_NAME)
        intent.putExtra(IntentUtil.UPLOAD_STATUS, status)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        call?.cancel()
        super.onDestroy()
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
