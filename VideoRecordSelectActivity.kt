import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.mepod.R
import com.mepod.databinding.ActivityVideoRecordSelectBinding
import com.mepod.util.IntentUtil
import com.video.trimmer.utils.FileUtils

class VideoRecordSelectActivity : AppCompatActivity() {

    lateinit var binding: ActivityVideoRecordSelectBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_record_select)

        binding.cameraButton.setOnClickListener {
            openVideoCapture()
        }

        binding.galleryButton.setOnClickListener {
            pickFromGallery()
        }

    }

    private fun openVideoCapture() {
        setupPermissions {
            val videoCapture = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            startActivityForResult(videoCapture, VideoRecordSelectActivity.REQUEST_VIDEO_TRIMMER)
        }
    }

    private fun pickFromGallery() {
        setupPermissions {
            val intent = Intent()
            intent.setTypeAndNormalize("video/*")
            intent.action = Intent.ACTION_GET_CONTENT
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(
                Intent.createChooser(intent, "Select Video"),
                REQUEST_VIDEO_TRIMMER
            )
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_VIDEO_TRIMMER) {
                val selectedUri = data!!.data
                if (selectedUri != null) {
                    startOptionActivity(selectedUri)
                } else {
                    Toast.makeText(
                        this@VideoRecordSelectActivity,
                        "Problem in retrieving video",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun startOptionActivity(uri: Uri) {
        val intent = Intent(this, VideoOptionChooseActivity::class.java)
        intent.putExtra(IntentUtil.EXTRA_VIDEO_PATH, FileUtils.getPath(this, uri))
        startActivity(intent)
    }

    companion object {
        private const val REQUEST_VIDEO_TRIMMER = 0x01
        private const val REQUEST_VIDEO_CROPPER = 0x02
    }

    lateinit var doThis: () -> Unit
    private fun setupPermissions(doSomething: () -> Unit) {
        val writePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val readPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        doThis = doSomething
        if (writePermission != PackageManager.PERMISSION_GRANTED && readPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 101
            )
        } else doThis()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            101 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    PermissionsDialog(
                        this,
                        "To continue, give Zoho Social access to your Photos."
                    ).show()
                } else doThis()
            }
        }
    }
}
