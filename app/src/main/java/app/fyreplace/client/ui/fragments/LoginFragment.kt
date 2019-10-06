package app.fyreplace.client.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import app.fyreplace.client.app.R
import app.fyreplace.client.app.databinding.FragmentLoginBinding
import app.fyreplace.client.ui.hideSoftKeyboard
import app.fyreplace.client.viewmodels.CentralViewModel
import app.fyreplace.client.viewmodels.LoginFragmentViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import retrofit2.HttpException

/**
 * [androidx.fragment.app.Fragment] showing a login screen to the user.
 */
class LoginFragment : FailureHandlingFragment(R.layout.fragment_login) {
    override val viewModel by viewModel<LoginFragmentViewModel>()
    override lateinit var bd: FragmentLoginBinding
    private val centralViewModel by sharedViewModel<CentralViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.authToken.observe(this) {
            if (it.isNotEmpty()) {
                centralViewModel.login()
                findNavController().navigate(LoginFragmentDirections.actionFragmentLoginToFragmentHome())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentLoginBinding.inflate(inflater, container, false).run {
        model = viewModel
        lifecycleOwner = viewLifecycleOwner
        bd = this
        return@run root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // For both the username and the password fields, require the input to not be empty
        mapOf(viewModel.username to bd.username, viewModel.password to bd.password).forEach {
            it.key.observe(viewLifecycleOwner) { content ->
                it.value.error =
                    if (content.isEmpty()) getString(R.string.login_error_field_required)
                    else null
            }
        }

        bd.password.setOnEditorActionListener { _, id, _ ->
            // Allow the user to use the keyboard "done" button to trigger a login attempt
            if (id == EditorInfo.IME_ACTION_DONE) true.also { bd.login.callOnClick() } else false
        }

        bd.login.setOnClickListener { attemptLogin() }
        bd.register.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.login_register_link))
                )
            )
        }
    }

    private fun attemptLogin() {
        val usernameStr = bd.username.text.toString()
        val passwordStr = bd.password.text.toString()
        var cancel = false
        var focusView: View? = null

        if (usernameStr.isEmpty()) {
            focusView = bd.username
            cancel = true
        }

        if (passwordStr.isEmpty()) {
            focusView = bd.password
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else launch {
            hideSoftKeyboard(bd.login)

            try {
                viewModel.setLoginAllowed(false)
                viewModel.attemptLogin(usernameStr, passwordStr)
            } catch (e: HttpException) {
                if (e.code() == 400) {
                    Toast.makeText(context, R.string.login_failure_login, Toast.LENGTH_SHORT).show()
                } else {
                    throw e
                }
            } finally {
                viewModel.setLoginAllowed(true)
            }
        }
    }
}
