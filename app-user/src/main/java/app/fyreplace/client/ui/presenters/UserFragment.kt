package app.fyreplace.client.ui.presenters

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import app.fyreplace.client.AppGlide
import app.fyreplace.client.app.user.R
import app.fyreplace.client.app.user.databinding.FragmentUserBinding
import app.fyreplace.client.data.models.Author
import app.fyreplace.client.ui.*
import app.fyreplace.client.viewmodels.UserFragmentViewModel
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class UserFragment : Fragment(R.layout.fragment_user), Presenter {
    override val viewModel by viewModel<UserFragmentViewModel>()
    override lateinit var bd: FragmentUserBinding
    private val fragmentArgs by inject<Args> { parametersOf(this) }
    private val markdown by lazyMarkdown()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val author = fragmentArgs.author
        val userId = fragmentArgs.userId

        when {
            author != null -> viewModel.setAuthor(author)
            userId != -1L -> launch { viewModel.setUserId(userId) }
            else -> throw IllegalStateException("Cannot start UserFragment without a user to show")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentUserBinding.inflate(inflater, container, false).run {
        lifecycleOwner = viewLifecycleOwner
        bd = this
        return@run root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.author.observe(viewLifecycleOwner) {
            bd.userName.text = it.name

            with(bd.userBioWrapper) {
                if (it.banned) {
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.error))
                } else if (it.bio.isBlank()) {
                    isVisible = false
                }
            }

            if (it.banned) with(bd.userBio) {
                setText(R.string.user_banned)
                setTextColor(ContextCompat.getColor(context, R.color.onError))
                setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_error, 0, 0, 0)
            } else {
                markdown.setMarkdown(bd.userBio, it.bio)
            }

            AppGlide.with(view)
                .loadAvatar(requireContext(), it)
                .transform(
                    CenterCrop(),
                    RoundedCorners(resources.getDimensionPixelOffset(R.dimen.user_picture_rounding))
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(bd.userPicture)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.actions_fragment_sharing, menu)
        viewModel.author.observe(viewLifecycleOwner) {
            with(menu.findItem(R.id.action_share)) {
                isVisible = !it.banned
                intent = getShareIntent(
                    userShareUrl(it.user),
                    getString(R.string.user_action_share_title)
                )
            }
        }
    }

    interface Args {
        val author: Author?
        val userId: Long
    }
}
