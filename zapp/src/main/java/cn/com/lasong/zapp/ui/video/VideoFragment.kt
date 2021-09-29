package cn.com.lasong.zapp.ui.video

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import cn.com.lasong.base.BaseFragment
import cn.com.lasong.widget.adapterview.adapter.ZRecyclerViewAdapter
import cn.com.lasong.zapp.R
import cn.com.lasong.zapp.database.VideoEntity
import cn.com.lasong.zapp.databinding.FragmentVideoBinding
import cn.com.lasong.zapp.utils.*
import java.util.concurrent.TimeUnit

class VideoFragment : BaseFragment() {
    private lateinit var videoModel: VideoViewModel

    private lateinit var binding: FragmentVideoBinding

    private lateinit var adapter: ZRecyclerViewAdapter<VideoEntity>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        videoModel = ViewModelProvider(this).get(VideoViewModel::class.java)
        binding = FragmentVideoBinding.inflate(layoutInflater, container, false)
        adapter = object : ZRecyclerViewAdapter<VideoEntity>(
            videoModel.data,
            R.layout.item_video,
            { view, position ->
                when (view.id) {
                    R.id.iv_favorite -> {
                        videoModel.clickFavorite(position)
                    }
                    R.id.iv_delete -> {
                        val video = videoModel.data[position]
                        val dialog =
                            AlertDialog.Builder(requireActivity()).setTitle(R.string.title_default)
                                .setMessage(
                                    getString(
                                        R.string.video_delete_tip_content,
                                        video.title
                                    )
                                )
                                .setPositiveButton(R.string.ok) { _, _ ->
                                    videoModel.deleteVideo(position)
                                }
                                .setNegativeButton(R.string.cancel, null)
                                .create()
                        dialog.show()
                    }

                    R.id.tv_title -> {
                        val video = videoModel.data[position]
                        EditTitleDialog(requireContext(), video.title) { text ->
                            if (null != text) {
                                videoModel.updateTitle(position, text)
                            }
                        }.show()
                    }

                    R.id.iv_share -> {
                        val video = videoModel.data[position]
                        val shareIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(video.uri))
                            type = "video/mp4"
                        }
                        startActivity(Intent.createChooser(shareIntent, getText(R.string.video_send_to)))
                    }
                }
            }) {
            override fun onBindViewHolder(
                holder: AdapterViewHolder,
                position: Int,
                payloads: MutableList<Any>
            ) {
                if (payloads.isNotEmpty()) {
                    when (payloads.first()) {
                        VideoViewModel.PAYLOAD_FAVORITE -> {
                            val item = videoModel.data[position]
                            holder.setImageResource(
                                R.id.iv_favorite,
                                if (item.favorite) R.drawable.ic_favorite_s else R.drawable.ic_favorite
                            )
                        }
                        VideoViewModel.PAYLOAD_TITLE -> {
                            val item = videoModel.data[position]
                            holder.setText(R.id.tv_title, item.title)
                        }
                    }
                    return
                }
                super.onBindViewHolder(holder, position, payloads)
            }

            override fun bind(holder: AdapterViewHolder?, item: VideoEntity?, position: Int) {
                val screenshot = item?.screenshot
                GlideApp.with(requireContext()).load(screenshot).miniThumb(400)
                    .into(holder?.getView(R.id.iv_screen_shot)!!)
                val createTime = item?.createTime ?: 0
                val duration = item?.duration ?: 0
                holder.setVisible(createTime > 0, R.id.tv_create)
                holder.setVisible(duration > 0, R.id.tv_duration)
                holder.setText(R.id.tv_create, createTime.formatTime())
                holder.setText(R.id.tv_duration, duration.formatDuration(TimeUnit.SECONDS))
                holder.setText(R.id.tv_title, item?.title)
                holder.setImageResource(
                    R.id.iv_favorite,
                    if (item?.favorite == true) R.drawable.ic_favorite_s else R.drawable.ic_favorite
                )
                holder.setOnClickListener(
                    R.id.iv_favorite,
                    R.id.iv_clip,
                    R.id.iv_share,
                    R.id.iv_delete,
                    R.id.iv_screen_shot,
                    R.id.tv_title
                )
                holder.setAlphaClick(
                    R.id.iv_favorite,
                    R.id.iv_clip,
                    R.id.iv_share,
                    R.id.iv_delete,
                    R.id.tv_title
                )
            }
        }
        binding.rvVideos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVideos.setLmAdapter(adapter)
        binding.rvVideos.enableLoadMore()
        binding.rvVideos.loadMoreFinish(false, true)
        binding.rvVideos.setLoadMoreHandler {
            videoModel.loadMore()
        }
        videoModel.changer.observe(viewLifecycleOwner, {
            val changer = it.first
            val positionStart = it.second.first
            val itemCount = it.second.second
            when (changer) {
                ZAdapterChanger.ADD -> {
                    adapter.notifyItemRangeInserted(positionStart, itemCount)
                    binding.rvVideos.loadMoreFinish(itemCount == 0, itemCount > 0)
                }
                ZAdapterChanger.CHANGE -> {
                    val payloads = it.second.third
                    adapter.notifyItemRangeChanged(positionStart, itemCount, payloads)
                }
                ZAdapterChanger.REMOVE -> {
                    adapter.notifyItemRangeRemoved(positionStart, itemCount)
                }
                else -> {

                }
            }
        })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        videoModel.loadMore()
    }
}