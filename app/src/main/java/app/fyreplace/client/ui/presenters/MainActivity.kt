package app.fyreplace.client.ui.presenters

import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.view.ActionMode
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import app.fyreplace.client.AppGlide
import app.fyreplace.client.app.NavigationMainDirections.Companion.actionGlobalFragmentDraft
import app.fyreplace.client.app.NavigationMainDirections.Companion.actionGlobalFragmentLogin
import app.fyreplace.client.app.NavigationMainDirections.Companion.actionGlobalFragmentLoginStartup
import app.fyreplace.client.app.NavigationMainDirections.Companion.actionGlobalFragmentPost
import app.fyreplace.client.app.NavigationMainDirections.Companion.actionGlobalFragmentUser
import app.fyreplace.client.app.R
import app.fyreplace.client.app.databinding.*
import app.fyreplace.client.data.models.Author
import app.fyreplace.client.data.models.ImageData
import app.fyreplace.client.ui.ImageSelector
import app.fyreplace.client.ui.Presenter
import app.fyreplace.client.ui.loadAvatar
import app.fyreplace.client.viewmodels.AreaSelectorViewModel
import app.fyreplace.client.viewmodels.CentralViewModel
import app.fyreplace.client.viewmodels.MainActivityViewModel
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.ByteArrayInputStream

/**
 * The central activity that hosts the different fragments.
 */
class MainActivity : AppCompatActivity(R.layout.activity_main), Presenter,
    NavController.OnDestinationChangedListener, ImageSelector {
    override val viewModel by viewModel<MainActivityViewModel>()
    override lateinit var bd: ActivityMainBinding
    override val maxImageSize = 0.5f
    private val centralViewModel by viewModel<CentralViewModel>()
    private val areaSelectorViewModel by viewModel<AreaSelectorViewModel>()
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var toolbarInset = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(null)
        bd = ActivityMainBinding.bind(findViewById(R.id.drawer_layout)).apply {
            lifecycleOwner = this@MainActivity
            content.model = viewModel
            content.centralModel = centralViewModel
        }

        window.navigationBarColor = ActivityCompat.getColor(this, R.color.navigation)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

            if (nightMode == Configuration.UI_MODE_NIGHT_NO) {
                bd.drawerLayout.systemUiVisibility =
                    bd.drawerLayout.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }

        reportFullyDrawn()

        val navController = findNavController(R.id.navigation_host)
        val navHeaderBinding = MainNavHeaderBinding.bind(bd.navigationView.getHeaderView(0))
            .apply { lifecycleOwner = this@MainActivity; model = centralViewModel }
        ActionMainNavFragmentNotificationsBinding.bind(bd.navigationView.menu.findItem(R.id.fragment_notifications).actionView)
            .run { lifecycleOwner = this@MainActivity; model = centralViewModel }
        ActionMainNavFragmentDraftsBinding.bind(bd.navigationView.menu.findItem(R.id.fragment_drafts).actionView)
            .run { lifecycleOwner = this@MainActivity; model = centralViewModel }
        ActionMainNavSettingsThemeSelectorBinding.bind(bd.navigationView.menu.findItem(R.id.settings_theme_selector).actionView)
            .run { lifecycleOwner = this@MainActivity; model = viewModel }
        ActionMainNavSettingsBadgeToggleBinding.bind(bd.navigationView.menu.findItem(R.id.settings_badge_toggle).actionView)
            .run { lifecycleOwner = this@MainActivity; model = viewModel }

        navHeaderBinding.edit.setOnClickListener { editProfile() }

        bd.navigationView.menu.findItem(R.id.fragment_drafts).actionView
            .findViewById<View>(R.id.button)
            .setOnClickListener {
                launch {
                    navController.navigate(
                        actionGlobalFragmentDraft(
                            draft = viewModel.createDraft(),
                            showHint = true
                        )
                    )
                }
            }

        viewModel.uiRefreshTick.observe(this) { launch { centralViewModel.updateNotificationCount() } }

        viewModel.selectedThemeIndex.observe(this) {
            AppCompatDelegate.setDefaultNightMode(viewModel.getTheme(it))
            delegate.applyDayNight()
        }

        centralViewModel.isLogged.observe(this) {
            if (it) {
                viewModel.login()
                launch { centralViewModel.updateProfileInfo() }
            } else if (navController.currentDestination?.id != R.id.fragment_login) {
                navController.navigate(
                    if (viewModel.startupLogin)
                        actionGlobalFragmentLoginStartup()
                    else
                        actionGlobalFragmentLogin().also {
                            viewModel.logout()
                            areaSelectorViewModel.setPreferredAreaName("")
                        }
                )
            }
        }

        centralViewModel.self.observe(this) {
            AppGlide.with(this)
                .loadAvatar(this, it)
                .transform(
                    CenterCrop(),
                    RoundedCorners(resources.getDimensionPixelOffset(R.dimen.nav_header_user_picture_rounding))
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(navHeaderBinding.userPicture)
        }

        val hostFragment = supportFragmentManager
            .findFragmentById(R.id.navigation_host) as NavHostFragment

        centralViewModel.postInfo.observe(this) { info ->
            if (hostFragment.navController.currentDestination?.id in NO_TITLE_DESTINATIONS) {
                setTitleInfo(info)
            }
        }

        setSupportActionBar(bd.content.toolbar)
        appBarConfiguration = AppBarConfiguration(TOP_LEVEL_DESTINATIONS, bd.drawerLayout)
        toolbarInset = bd.content.toolbar.contentInsetStartWithNavigation

        setupActionBarWithNavController(hostFragment.navController, appBarConfiguration)
        bd.navigationView.setupWithNavController(hostFragment.navController)
        hostFragment.navController.addOnDestinationChangedListener(this)

        MainActivityViewModel.NAVIGATION_LINKS.forEach { pair ->
            bd.navigationView.menu.findItem(pair.key).setOnMenuItemClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(pair.value))))
                return@setOnMenuItemClickListener true
            }
        }

        bd.navigationView.menu.findItem(R.id.fyreplace_licenses).setOnMenuItemClickListener {
            showLicenses()
            return@setOnMenuItemClickListener true
        }

        bd.navigationView.menu.findItem(R.id.fyreplace_logout).setOnMenuItemClickListener {
            centralViewModel.logout()
            return@setOnMenuItemClickListener true
        }

        bd.content.toolbar.doOnLayout {
            bd.content.badge.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = bd.content.toolbar.height / 2
                topMargin = bd.content.toolbar.height / 2 -
                    resources.getDimensionPixelOffset(R.dimen.margin_medium)
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (intent != null) {
            handleSendIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null) {
            handleViewIntent(intent)
            handleSendIntent(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        item.onNavDestinationSelected(findNavController(R.id.navigation_host))

    override fun onBackPressed() = when {
        bd.drawerLayout.isDrawerOpen(GravityCompat.START) -> bd.drawerLayout.closeDrawer(
            GravityCompat.START
        )
        currentFragmentAs<BackHandlingFragment>()?.onGoBack(BackHandlingFragment.Method.BACK_BUTTON) == false -> Unit
        else -> super.onBackPressed()
    }

    override fun onSupportNavigateUp() = when {
        currentFragmentAs<BackHandlingFragment>()?.onGoBack(BackHandlingFragment.Method.UP_BUTTON) == false -> false
        findNavController(R.id.navigation_host).navigateUp(appBarConfiguration) -> true
        else -> super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super<AppCompatActivity>.onActivityResult(requestCode, resultCode, data)
        super<ImageSelector>.onActivityResult(requestCode, resultCode, data)
    }

    override fun getContext() = this

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        updateDrawer(destination)

        when {
            destination.id == R.id.fragment_login -> bd.content.toolbar.navigationIcon = null
            bd.content.toolbar.navigationIcon == null -> bd.content.toolbar.navigationIcon =
                DrawerArrowDrawable(this).apply { isSpinEnabled = true }
        }

        centralViewModel.setNotificationBadgeVisible(
            destination.id != R.id.fragment_notifications
                && destination.id in TOP_LEVEL_DESTINATIONS
        )

        if (destination.id in NO_TITLE_DESTINATIONS
            && bd.content.toolbar.title.toString() == getString(R.string.app_name)
        ) {
            bd.content.toolbar.title = ""
        }

        bd.content.toolbar.run {
            subtitle = ""
            logo = null
            contentInsetStartWithNavigation = toolbarInset
            setTitleTextAppearance(
                this@MainActivity,
                R.style.AppTheme_TextAppearance_ActionBar_Title
            )
        }
    }

    override fun onSupportActionModeStarted(mode: ActionMode) {
        super.onSupportActionModeStarted(mode)
        bd.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun onSupportActionModeFinished(mode: ActionMode) {
        super.onSupportActionModeFinished(mode)
        findNavController(R.id.navigation_host).currentDestination?.let { updateDrawer(it) }
    }

    override suspend fun onImage(image: ImageData) = centralViewModel.setPendingProfileAvatar(image)


    override suspend fun onImageRemoved() = Unit

    private fun handleViewIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) {
            return
        }

        for (pair in REGEX_TO_DIRECTIONS) {
            pair.key.matchEntire(intent.data?.path.orEmpty())?.let {
                findNavController(R.id.navigation_host).navigate(pair.value(it))
                return
            }
        }
    }

    private fun handleSendIntent(intent: Intent) {
        if (intent.action !in listOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) {
            return
        }

        val navController = findNavController(R.id.navigation_host)

        when {
            intent.type?.startsWith("text/") == true -> launch {
                val draft = viewModel.createDraft(intent.getStringExtra(Intent.EXTRA_TEXT))
                navController.navigate(actionGlobalFragmentDraft(draft = draft))
            }

            intent.type?.startsWith("image/") == true -> launch {
                val uris =
                    intent.getParcelableExtra<Uri?>(Intent.EXTRA_STREAM)?.let { listOf(it) }
                        ?: intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                        ?: intent.clipData?.run { (0..itemCount).map { getItemAt(it).uri } }

                navController.navigate(
                    actionGlobalFragmentDraft(
                        draft = viewModel.createDraft(),
                        imageUris = uris.orEmpty().toTypedArray()
                    )
                )
            }
        }
    }

    private fun updateDrawer(destination: NavDestination) {
        bd.drawerLayout.setDrawerLockMode(
            if (destination.id in TOP_LEVEL_DESTINATIONS)
                DrawerLayout.LOCK_MODE_UNLOCKED
            else
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        )
    }

    private fun setTitleInfo(info: CentralViewModel.PostInfo?) {
        if (currentFragmentAs<ToolbarUsingFragment>() == null) {
            return
        }

        if (info == null) {
            bd.content.toolbar.run {
                title = ""
                subtitle = ""
                logo = null
                return
            }
        }

        bd.content.toolbar.run {
            title = " " + (info.author?.name ?: getString(R.string.main_author_anonymous))
            subtitle = " " + info.date
            contentInsetStartWithNavigation = 0
            setTitleTextAppearance(
                this@MainActivity,
                R.style.AppTheme_TextAppearance_ActionBar_Title_Condensed
            )
        }

        val size = resources.getDimensionPixelOffset(R.dimen.toolbar_logo_picture_size)

        info.author?.let {
            AppGlide.with(this)
                .loadAvatar(this, it)
                .transform(
                    CenterCrop(),
                    RoundedCorners(resources.getDimensionPixelOffset(R.dimen.toolbar_logo_picture_rounding))
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ToolbarTarget(it, size))
        }

        if (info.author == null) {
            bd.content.toolbar.logo = null
        }
    }

    private fun editProfile() {
        lateinit var dialog: AlertDialog

        dialog = MaterialAlertDialogBuilder(this)
            .setView(R.layout.main_profile_editor)
            .setNegativeButton(R.string.cancel) { _, _ ->
                centralViewModel.resetPendingProfileAvatar()
            }
            .setPositiveButton(R.string.ok) { _, _ ->
                launch {
                    val bio = dialog.findViewById<TextView>(R.id.user_bio)?.text ?: ""
                    centralViewModel.sendProfile(bio.toString())
                }
            }
            .setOnDismissListener {
                centralViewModel.newUserAvatar.removeObservers(this)
                centralViewModel.selfBio.removeObservers(this)
            }
            .create()
            .apply { show() }

        val avatar = dialog.findViewById<ImageView>(R.id.user_picture)!!
        val avatarTransform = MultiTransformation(
            CenterCrop(),
            RoundedCorners(resources.getDimensionPixelOffset(R.dimen.dialog_user_picture_rounding))
        )
        val imageTransition = DrawableTransitionOptions.withCrossFade()

        AppGlide.with(this)
            .loadAvatar(this, centralViewModel.self.value)
            .transition(imageTransition)
            .transform(avatarTransform)
            .into(avatar)

        dialog.findViewById<View>(R.id.edit_user_picture)?.setOnClickListener {
            showImageChooser(R.string.main_profile_editor_dialog_title, false)
        }

        centralViewModel.newUserAvatar.observe(this) {
            it?.run {
                AppGlide.with(this@MainActivity)
                    .load(Drawable.createFromStream(ByteArrayInputStream(bytes), "avatar"))
                    .transform(avatarTransform)
                    .transition(imageTransition)
                    .into(avatar)
            }
        }

        centralViewModel.selfBio.observe(this) {
            dialog.findViewById<EditText>(R.id.user_bio)?.run {
                setText(it)

                if (it.isNotEmpty()) {
                    minLines = 0
                }
            }
        }
    }

    private fun showLicenses() {
        val licenses = resources.getStringArray(R.array.dependencies_names_base)
        val links = resources.getStringArray(R.array.dependencies_links_base)

        MaterialAlertDialogBuilder(this)
            .setItems(licenses) { _, i ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(links[i])))
            }
            .setTitle(R.string.main_nav_fyreplace_licenses_dialog_title)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private inline fun <reified T> currentFragmentAs(): T? {
        val destinationFragments = supportFragmentManager.fragments
            .firstOrNull { it is NavHostFragment }
            ?.childFragmentManager
            ?.fragments
            ?.filterIsInstance<Presenter>()

        destinationFragments?.let {
            val last = it.last()

            if (it.isNotEmpty() && last as? T != null) {
                return last
            }
        }

        return null
    }

    private companion object {
        val TOP_LEVEL_DESTINATIONS = setOf(
            R.id.fragment_home,
            R.id.fragment_notifications,
            R.id.fragment_archive,
            R.id.fragment_own_posts,
            R.id.fragment_drafts
        )
        val NO_TITLE_DESTINATIONS = setOf(
            R.id.fragment_home,
            R.id.fragment_post,
            R.id.fragment_user,
            R.id.fragment_draft
        )

        val POST_REGEX = Regex("/areas/(\\w+)/(\\d+)(?:/(\\d+))?")
        val USER_REGEX = Regex("/user/(\\d+)")
        val REGEX_TO_DIRECTIONS = mapOf(
            POST_REGEX to { result: MatchResult ->
                actionGlobalFragmentPost(
                    areaName = result.groupValues[1],
                    postId = result.groupValues[2].toLong(),
                    selectedCommentId = result.groupValues[3]
                        .takeIf { it.isNotEmpty() }
                        ?.toLong()
                        ?: -1
                )
            },
            USER_REGEX to { result: MatchResult ->
                actionGlobalFragmentUser(userId = result.groupValues[1].toLong())
            }
        )
    }

    private inner class ToolbarTarget(private val author: Author, private val size: Int) :
        CustomTarget<Drawable>(size, size) {
        override fun onLoadCleared(placeholder: Drawable?) = Unit

        override fun onLoadFailed(errorDrawable: Drawable?) {
            if (errorDrawable != null) {
                load(errorDrawable)
            }
        }

        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) =
            load(resource)

        private fun load(drawable: Drawable) {
            bd.content.toolbar.logo = drawable
            bd.content.toolbar.children
                .filterIsInstance<ImageView>()
                .find { it.drawable == drawable }
                ?.run {
                    updateLayoutParams<ViewGroup.LayoutParams> {
                        width = size
                        height = size
                    }

                    setOnClickListener {
                        findNavController(R.id.navigation_host)
                            .navigate(actionGlobalFragmentUser(author = author))
                    }
                }
        }
    }
}
