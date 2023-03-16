/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.intentresolver.widget

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.util.AttributeSet
import android.util.PluralsMessageFormatter
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.intentresolver.R
import com.android.intentresolver.widget.ImagePreviewView.TransitionElementStatusCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.math.sign

private const val TRANSITION_NAME = "screenshot_preview_image"
private const val PLURALS_COUNT = "count"

class ScrollableImagePreviewView : RecyclerView, ImagePreviewView {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        adapter = Adapter(context)
        val spacing = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 5f, context.resources.displayMetrics
        ).toInt()
        addItemDecoration(SpacingDecoration(spacing))
    }

    private val previewAdapter get() = adapter as Adapter

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        setOverScrollMode(
            if (areAllChildrenVisible) View.OVER_SCROLL_NEVER else View.OVER_SCROLL_ALWAYS
        )
    }

    override fun setTransitionElementStatusCallback(callback: TransitionElementStatusCallback?) {
        previewAdapter.transitionStatusElementCallback = callback
    }

    fun setPreviews(previews: List<Preview>, otherItemCount: Int, imageLoader: ImageLoader) =
        previewAdapter.setPreviews(previews, otherItemCount, imageLoader)

    class Preview(val type: PreviewType, val uri: Uri)
    enum class PreviewType {
        Image, Video, File
    }

    private class Adapter(private val context: Context) : RecyclerView.Adapter<ViewHolder>() {
        private val previews = ArrayList<Preview>()
        private var imageLoader: ImageLoader? = null
        private var firstImagePos = -1
        var transitionStatusElementCallback: TransitionElementStatusCallback? = null
        private var otherItemCount = 0

        fun setPreviews(
            previews: List<Preview>, otherItemCount: Int, imageLoader: ImageLoader
        ) {
            this.previews.clear()
            this.previews.addAll(previews)
            this.imageLoader = imageLoader
            firstImagePos = previews.indexOfFirst { it.type == PreviewType.Image }
            this.otherItemCount = maxOf(0, otherItemCount)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(itemType, parent, false);
            return if (itemType == R.layout.image_preview_other_item) {
                OtherItemViewHolder(view)
            } else {
                PreviewViewHolder(view)
            }
        }

        override fun getItemCount(): Int = previews.size + otherItemCount.sign

        override fun getItemViewType(position: Int): Int {
            return if (position == previews.size) {
                R.layout.image_preview_other_item
            } else {
                R.layout.image_preview_image_item
            }
        }

        override fun onBindViewHolder(vh: ViewHolder, position: Int) {
            when (vh) {
                is OtherItemViewHolder -> vh.bind(otherItemCount)
                is PreviewViewHolder -> vh.bind(
                    previews[position],
                    imageLoader ?: error("ImageLoader is missing"),
                    if (position == firstImagePos && transitionStatusElementCallback != null) {
                        this::onTransitionElementReady
                    } else {
                        null
                    }
                )
            }
        }

        override fun onViewRecycled(vh: ViewHolder) {
            vh.unbind()
        }

        override fun onFailedToRecycleView(vh: ViewHolder): Boolean {
            vh.unbind()
            return super.onFailedToRecycleView(vh)
        }

        private fun onTransitionElementReady(name: String) {
            transitionStatusElementCallback?.apply {
                onTransitionElementReady(name)
                onAllTransitionElementsReady()
            }
            transitionStatusElementCallback = null
        }
    }

    private sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun unbind()
    }

    private class PreviewViewHolder(view: View) : ViewHolder(view) {
        private val image = view.requireViewById<ImageView>(R.id.image)
        private val badgeFrame = view.requireViewById<View>(R.id.badge_frame)
        private val badge = view.requireViewById<ImageView>(R.id.badge)
        private var scope: CoroutineScope? = null

        fun bind(
            preview: Preview,
            imageLoader: ImageLoader,
            previewReadyCallback: ((String) -> Unit)?
        ) {
            image.setImageDrawable(null)
            image.transitionName = if (previewReadyCallback != null) {
                TRANSITION_NAME
            } else {
                null
            }
            badgeFrame.visibility = when (preview.type) {
                PreviewType.Image -> View.GONE
                PreviewType.Video -> {
                    badge.setImageResource(R.drawable.ic_file_video)
                    View.VISIBLE
                }
                else -> {
                    badge.setImageResource(R.drawable.chooser_file_generic)
                    View.VISIBLE
                }
            }
            resetScope().launch {
                loadImage(preview.uri, imageLoader)
                if (preview.type == PreviewType.Image) {
                    previewReadyCallback?.let { callback ->
                        image.waitForPreDraw()
                        callback(TRANSITION_NAME)
                    }
                }
            }
        }

        private suspend fun loadImage(uri: Uri, imageLoader: ImageLoader) {
            val bitmap = runCatching {
                // it's expected for all loading/caching optimizations to be implemented by the
                // loader
                imageLoader(uri)
            }.getOrNull()
            image.setImageBitmap(bitmap)
        }

        private fun resetScope(): CoroutineScope =
            (MainScope() + Dispatchers.Main.immediate).also {
                scope?.cancel()
                scope = it
            }

        override fun unbind() {
            scope?.cancel()
            scope = null
        }
    }

    private class OtherItemViewHolder(view: View) : ViewHolder(view) {
        private val label = view.requireViewById<TextView>(R.id.label)

        fun bind(count: Int) {
            label.text = PluralsMessageFormatter.format(
                itemView.context.resources,
                mapOf(PLURALS_COUNT to count),
                R.string.other_files
            )
        }

        override fun unbind() = Unit
    }

    private class SpacingDecoration(private val margin: Int) : ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
            outRect.set(margin, 0, margin, 0)
        }
    }
}
