package app.fyreplace.client.ui.widgets

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView
import ru.noties.markwon.image.AsyncDrawableScheduler

class MarkdownRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr), RecyclerView.OnChildAttachStateChangeListener {
    init {
        addOnChildAttachStateChangeListener(this)
    }

    override fun onChildViewAttachedToWindow(view: View) {
        (view as? TextView)?.run {
            movementMethod = LinkMovementMethod.getInstance()
            doOnLayout { it.post { AsyncDrawableScheduler.schedule(this) } }
        }
    }

    override fun onChildViewDetachedFromWindow(view: View) = Unit
}
