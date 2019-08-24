package app.fyreplace.client.ui.fragments

import android.content.ClipDescription
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import app.fyreplace.client.R

abstract class SharingFragment(contentLayoutId: Int) : FailureHandlingFragment(contentLayoutId) {
    abstract val menuShareContent: CharSequence
    abstract val menuShareTitle: CharSequence

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
        inflater.inflate(R.menu.actions_fragment_sharing, menu)

    override fun onOptionsItemSelected(item: MenuItem) = super.onOptionsItemSelected(item).also {
        if (item.itemId == R.id.action_share) {
            shareText(menuShareContent, menuShareTitle)
        }
    }

    protected fun shareText(text: CharSequence, title: CharSequence) = startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = ClipDescription.MIMETYPE_TEXT_PLAIN
                putExtra(Intent.EXTRA_TEXT, text)
            },
            title
        )
    )
}
