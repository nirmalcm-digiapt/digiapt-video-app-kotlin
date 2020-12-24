package com.digiapt.digiaptvideoapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.digiapt.digiaptvideoapp.R
import com.digiapt.digiaptvideoapp.models.Section
import com.digiapt.digiaptvideoapp.models.Video
import com.digiapt.digiaptvideoapp.listeners.SectionClickListener
import com.digiapt.digiaptvideoapp.listeners.VideoClickListener
import com.digiapt.digiaptvideoapp.activities.VideoPreviewActivity
import com.digiapt.digiaptvideoapp.util.Constants
import kotlinx.android.synthetic.main.include_animation_home.*
import android.view.Gravity
import android.content.Context
import android.util.Log
import android.widget.*
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.digiapt.digiaptvideoapp.activities.VdocipherDownloadActivity
import com.digiapt.digiaptvideoapp.activities.VdocipherPlayerActivity
import com.digiapt.digiaptvideoapp.models.Subcategory
import com.digiapt.digiaptvideoapp.adapters.SubcategoryAdapter
import com.digiapt.digiaptvideoapp.adapters.SectionAdapter
import com.digiapt.digiaptvideoapp.listeners.SubcategoryClickListener
import com.digiapt.digiaptvideoapp.viewmodels.HomeViewModel
import com.digiapt.digiaptvideoapp.viewmodels.HomeViewModelFactory
import com.loopj.android.http.AsyncHttpClient
import kotlinx.android.synthetic.main.activity_video_preview.*
import kotlinx.android.synthetic.main.activity_video_preview.id_play
import kotlinx.android.synthetic.main.activity_video_preview.id_video_name
import kotlinx.android.synthetic.main.activity_video_preview.video_image
import kotlinx.android.synthetic.main.fragment_home.*
import net.digiapt.util.Coroutines
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance


class HomeFragment : Fragment() ,
    SectionClickListener,
    VideoClickListener,
    SubcategoryClickListener, KodeinAware{

    override val kodein by kodein()

    private lateinit var viewModel: HomeViewModel
    private val factory: HomeViewModelFactory by instance()

//    private lateinit var factory: HomeViewModelFactory
//    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //startAnimation()

//        val api = Api()
//        val repository = HomeRepository(api)

//        factory = HomeViewModelFactory(repository)

        viewModel = ViewModelProviders.of(this, factory).get(HomeViewModel::class.java)

        Log.d("ncm","Inside view model get video high light")

        viewModel.getVideoHighlight("")

        viewModel.videoHighlight.observe(viewLifecycleOwner, Observer { videoHighlight ->

            Log.d("ncm MyLog Video Highlight", videoHighlight.title)

            //stopAnimation()

            Glide.with(this) //1
                .load(videoHighlight.poster)
                //.skipMemoryCache(true) //2
                .diskCacheStrategy(DiskCacheStrategy.ALL) //3
                //.transform(CircleCrop()) //4
                .into(video_image)

            id_play.setOnClickListener {
                val intent = Intent(context, VdocipherPlayerActivity::class.java)
                intent.putExtra("data", videoHighlight.id)
                startActivity(intent)
            }

            id_video_name.text = videoHighlight.title

            id_info.setOnClickListener {
                activity?.let {
                    val intent = Intent(it, VideoPreviewActivity::class.java)
                    intent.putExtra("video_id", videoHighlight.id)
                    intent.putExtra("video_image_path", videoHighlight.poster)
                    intent.putExtra("video_name", videoHighlight.title)
                    it.startActivity(intent)
                }
            }

            id_my_list.setOnClickListener{
                activity?.let {
                    val intent = Intent(it, VdocipherDownloadActivity::class.java)
                    intent.putExtra("video_id", videoHighlight.id)
                    intent.putExtra("video_image_path", videoHighlight.poster)
                    intent.putExtra("video_name", videoHighlight.title)
                    it.startActivity(intent)
                }
            }
        })

        viewModel.getSections("")

        viewModel.sections.observe(viewLifecycleOwner, Observer { sections ->
            //stopAnimation()
            recycler_view_categories.also {
                it.layoutManager = LinearLayoutManager(requireContext())
                it.setHasFixedSize(true)
                it.adapter = SectionAdapter(
                    this@HomeFragment,
                    viewLifecycleOwner,
                    sections,
                    this,
                    this
                )
            }
        })

    }

    fun startAnimation() {
        id_shimmer_layout.visibility = View.VISIBLE
        id_shimmer_layout.startShimmerAnimation()
    }

    fun stopAnimation() {
        id_shimmer_layout.visibility = View.GONE
        id_shimmer_layout.stopShimmerAnimation()
    }

    override fun onSubcategoryItemClick(view: View, subcategory: Subcategory) {

    }

    override fun onSectionItemClick(view: View, section: Section) {
        when (view.id) {
            else -> {
                showPopup(view)
            }
        }
    }

    override fun onVideoItemClick(view: View, video: Video) {
        when (view.id) {
            else -> {
                activity?.let {
                    val intent = Intent(it, VideoPreviewActivity::class.java)
                    intent.putExtra("video_id", video.id)
                    intent.putExtra("video_image_path", video.poster)
                    intent.putExtra("video_name", video.title)
                    it.startActivity(intent)
                }
            }
        }
    }

    fun showPopup(view: View){
        try {

            //We need to get the instance of the LayoutInflater, use the context of this activity
            val inflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            //Inflate the view from a predefined XML layout
            val layout = inflater.inflate(
                R.layout.include_popup_home, null
            )

            // create a 300px width and 470px height PopupWindow
            val popupWindow = PopupWindow(
                layout, FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT, true
            )

            // display the popup in the center
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
            val cancelButton = layout.findViewById(R.id.id_close_popup) as ImageButton

            val recyclerVieSubCategories = layout.findViewById(R.id.recycler_view_subcategories) as RecyclerView

            val subCategories = listOf( Subcategory("0","English"),
                                        Subcategory("1","Hindi"),
                                        Subcategory("1","Malayalam"),
                                        Subcategory("1","Kannada"),
                                        Subcategory("1","Tamil"),
                                        Subcategory("1","Telugu"),
                                        Subcategory("0","Adventure"),
                                        Subcategory("1","Thriller"),
                                        Subcategory("1","Comedy"),
                                        Subcategory("1","Drama"),
                                        Subcategory("1","Action"),
                                        Subcategory("1","Animations"),
                                        Subcategory("0","English"),
                                        Subcategory("1","Hindi"),
                                        Subcategory("1","Malayalam"),
                                        Subcategory("1","Kannada"),
                                        Subcategory("1","Tamil"),
                                        Subcategory("1","Telugu"),
                                        Subcategory("0","Adventure"),
                                        Subcategory("1","Thriller"),
                                        Subcategory("1","Comedy"),
                                        Subcategory("1","Drama"),
                                        Subcategory("1","Action"),
                                        Subcategory("1","Animations"))
            recyclerVieSubCategories.also {
                it.layoutManager = LinearLayoutManager(requireContext())
                it.setHasFixedSize(true)
                it.adapter =
                    SubcategoryAdapter(
                        this@HomeFragment,
                        viewLifecycleOwner,
                        subCategories,
                        this
                    )
            }

            cancelButton.setOnClickListener{
                popupWindow.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}