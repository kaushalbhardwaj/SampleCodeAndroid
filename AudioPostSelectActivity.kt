import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.CodeBoy.MediaFacer.AudioGet
import com.CodeBoy.MediaFacer.MediaFacer
import com.CodeBoy.MediaFacer.VideoGet
import com.CodeBoy.MediaFacer.mediaHolders.audioContent
import com.CodeBoy.MediaFacer.mediaHolders.videoContent


class AudioPostSelectActivity : RootActivity() {

    lateinit var binding: ActivityAudioPostSelectBinding
    private val REQUEST_VIDEO_TRIMMER = 0x01
    var isAudioSelected = true
    var mediaPlayer: MediaPlayer = MediaPlayer()
    val PERMISSION_READ = 0
    var currentMusicId: Long? = null
    var audioArrayList: ArrayList<audioContent> = ArrayList()
    var videoArrayList: ArrayList<videoContent> = ArrayList()
    var audioAdapter: AudioAdapter? = null
    var videoAdapter: VideoAdapter? = null
    var currentPlayIcon: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_audio_post_select)
        if (checkPermission()) {
            setAudio()
        }

    }

    fun checkPermission(): Boolean {
        val READ_EXTERNAL_PERMISSION: Int =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (READ_EXTERNAL_PERMISSION != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_READ
            )
            return false
        }
        return true
    }

    fun setAudio() {
        binding.audioList.setLayoutManager(
            LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false
            )
        )
        binding.audioList.setItemAnimator(DefaultItemAnimator())
        getAudioFiles()
        getVideoContent()

        setAudioTab()
        setOnClickListeners()

    }

    private fun setOnClickListeners() {

        binding.audioLayout.setOnClickListener {
            hideSearchBar()
            setAudioTab()
        }

        binding.videoLayout.setOnClickListener {
            hideSearchBar()
            setVideoTab()
            stopAudio()
        }

        binding.back.setOnClickListener {
            onBackPressed()
        }

        binding.add.setOnClickListener {
            if (isAudioSelected) {
                startActivity(Intent(this, AudioRecordActivity::class.java))
            } else {
                openVideoCapture()
            }
        }

        binding.search.setOnClickListener {
            showSearchBar()
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {

                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {

                Log.d("TAG", "queryyyy-- $p0")
                if (isAudioSelected) {
                    audioAdapter?.getFilter()?.filter(p0)
                } else {
                    videoAdapter?.getFilter()?.filter(p0)
                }

                return true
            }

        })

    }

    fun playAudio(uri: Uri, musicId: Long, play: AppCompatImageView) {
        try {
            currentPlayIcon?.setImageResource(R.drawable.ic_group_play)
            currentPlayIcon = play
            currentMusicId = musicId
            mediaPlayer.reset()
            mediaPlayer.setDataSource(this, uri)
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("TAG", "audiooooo-- ${e.message}")
        }
    }

    fun stopAudio() {
        currentPlayIcon?.setImageResource(R.drawable.ic_group_play)
        currentMusicId = null
        mediaPlayer.stop()
        mediaPlayer.reset()
    }

    fun showSearchBar() {
        binding.toolbar.visibility = View.GONE
        binding.searchLayout.visibility = View.VISIBLE
        binding.searchView.requestFocus()
        binding.searchView.showKeyboard(this)
    }

    fun hideSearchBar() {
        binding.toolbar.visibility = View.VISIBLE
        binding.searchLayout.visibility = View.GONE
        binding.searchView.clearFocus()
        binding.searchView.hideKeyboard()
    }

    private fun openVideoCapture() {
        val videoCapture = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivityForResult(videoCapture, REQUEST_VIDEO_TRIMMER)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_VIDEO_TRIMMER) {
                val selectedUri = data!!.data
                if (selectedUri != null) {
                    GenUtil.showShortToast("Video Recorded Successfully")
                } else {
                    Toast.makeText(
                        this,
                        "Problem in retrieving video",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun getAudioFiles() {

        val audioContents = MediaFacer
            .withAudioContex(this)
            .getAllAudioContent(AudioGet.externalContentUri)
        audioArrayList.addAll(audioContents)

        audioAdapter = AudioAdapter(this, audioArrayList)
        binding.audioList.adapter = audioAdapter
        audioAdapter?.setData()
    }

    fun getVideoContent() {

        binding.videoList.layoutManager = GridLayoutManager(this, 3)
        val allVideos = MediaFacer
            .withVideoContex(this)
            .getAllVideoContent(VideoGet.externalContentUri)

        videoArrayList.addAll(allVideos)

        videoAdapter = VideoAdapter(this, videoArrayList)
        binding.videoList.adapter = videoAdapter
        videoAdapter?.setData()

    }

    fun setAudioTab() {
        isAudioSelected = true
        binding.viewAudio.visibility = View.VISIBLE
        binding.viewVideo.visibility = View.INVISIBLE
        binding.audioList.visibility = View.VISIBLE
        binding.videoList.visibility = View.GONE

        binding.add.setImageResource(R.drawable.ic_microphone)
    }

    fun setVideoTab() {
        isAudioSelected = false
        binding.viewAudio.visibility = View.INVISIBLE
        binding.viewVideo.visibility = View.VISIBLE
        binding.audioList.visibility = View.GONE
        binding.videoList.visibility = View.VISIBLE

        binding.add.setImageResource(R.drawable.ic_video_camera)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_READ -> {
                if (grantResults.size > 0 && permissions[0] == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(
                            applicationContext,
                            "Please allow storage permission",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()
                    } else {
                        setAudio()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopAudio()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            mediaPlayer?.release()
        }
    }

}
