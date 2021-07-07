import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.CodeBoy.MediaFacer.mediaHolders.videoContent
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.mepod.databinding.ItemVideoListBinding
import com.mepod.util.GenUtil
import com.mepod.util.IntentUtil
import java.util.*

class VideoAdapter(var context: Context, var videoArrayList: ArrayList<videoContent>) :
    RecyclerView.Adapter<VideoAdapter.ViewHolder>(), Filterable {
    var videoArrayListFiltered: MutableList<videoContent> = ArrayList()
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)

        val binding = ItemVideoListBinding.inflate(inflater, viewGroup, false)
        return ViewHolder(
            binding
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, i: Int) {

        val video = videoArrayListFiltered[i]
        holder.binding.time.text = GenUtil.getFormattedTime(videoArrayListFiltered[i].videoDuration / 1000)
        Glide.with(context)
            .load(videoArrayListFiltered[i].assetFileStringUri)
            .apply(RequestOptions().centerCrop())
            .into(holder.binding.image)

        holder.binding.root.setOnClickListener {
            val intent = Intent(context, ComposePostActivity::class.java)
            intent.putExtra(IntentUtil.CONTENT_PATH, video.path)
            context.startActivity(intent)
        }
    }

    override fun getFilter(): Filter? {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults? {
                val charString = charSequence.toString()
                if (charString.isEmpty()) {
                    videoArrayListFiltered = videoArrayList
                } else {
                    val filteredList: MutableList<videoContent> = ArrayList()
                    for (audio in videoArrayListFiltered) {

                        if (audio.videoName.toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(audio)
                        }
                    }
                    videoArrayListFiltered = filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = videoArrayListFiltered
                return filterResults
            }

            override fun publishResults(
                charSequence: CharSequence?,
                filterResults: FilterResults
            ) {
                videoArrayListFiltered = filterResults.values as ArrayList<videoContent>
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return videoArrayListFiltered.size
    }

    fun setData() {
        videoArrayListFiltered = videoArrayList
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemVideoListBinding) :
        RecyclerView.ViewHolder(binding.root)

}
