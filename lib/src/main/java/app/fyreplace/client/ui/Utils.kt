package app.fyreplace.client.ui

import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import app.fyreplace.client.GlideRequests
import app.fyreplace.client.data.models.Author
import app.fyreplace.client.data.models.Post
import app.fyreplace.client.lib.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.movement.MovementMethodPlugin

val IMAGE_REGEX = Regex("\n*\\[img:\\s*(\\d+)]\n*", RegexOption.MULTILINE)
val YOUTUBE_REGEX =
    Regex("(?:https?://)?(?:www\\.)?youtu(?:be\\.(?:\\w+)/watch\\?v=|\\.be/)([\\w\\-]+)")

fun showSoftKeyboard(view: View?) {
    view?.let {
        ContextCompat.getSystemService(it.context, InputMethodManager::class.java)
            ?.showSoftInput(it, 0)
    }
}

fun hideSoftKeyboard(view: View?) {
    view?.let {
        ContextCompat.getSystemService(it.context, InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

fun Fragment.lazyMarkdown() = lazy {
    requireActivity().let {
        Markwon.builder(it)
            .usePlugin(CorePlugin.create())
            .usePlugin(MovementMethodPlugin.create())
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(PostPlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(ImagesPlugin.create())
            .usePlugin(GlideImagesPlugin.create(PostGlideStore(it)))
            .build()
    }
}

fun Post.toMarkdown(content: String = text) =
    (image?.let { "![]($it)\n\n" } ?: "") + content.replace(IMAGE_REGEX) {
        val imageNum = it.groupValues[1].toInt()
        val image = additionalImages.firstOrNull { img -> img.num == imageNum }
        return@replace image?.run { "\n![${image.comment}](${image.image})\n" } ?: it.groupValues[0]
    }

fun GlideRequests.loadAvatar(context: Context, author: Author?) =
    load(if (author?.banned != true) author?.avatar ?: R.drawable.default_avatar else "")
        .error(context.getDrawable(if (author?.banned == true) R.drawable.banned_avatar else R.drawable.ic_image))
        .placeholder(android.R.color.transparent)

var FloatingActionButton.shown: Boolean
    get() = isOrWillBeShown
    set(value) {
        if (value) {
            show()
        } else {
            hide()
        }
    }

fun getShareIntent(text: CharSequence, title: CharSequence): Intent =
    Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = ClipDescription.MIMETYPE_TEXT_PLAIN
            putExtra(Intent.EXTRA_TEXT, text)
        },
        title
    )

fun postShareUrl(areaName: String, postId: Long) =
    "https://client.wildfyre.net/areas/$areaName/$postId"

fun postShareUrl(areaName: String, postId: Long, selectedCommentId: Long) =
    "https://client.wildfyre.net/areas/$areaName/$postId/$selectedCommentId"

fun userShareUrl(userId: Long) =
    "https://client.wildfyre.net/user/$userId"

fun youtubeThumbnail(videoId: String) =
    "https://img.youtube.com/vi/$videoId/0.jpg"
