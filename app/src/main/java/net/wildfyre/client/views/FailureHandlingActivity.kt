package net.wildfyre.client.views

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import net.wildfyre.client.R
import net.wildfyre.client.data.Failure
import net.wildfyre.client.data.FailureHandler
import net.wildfyre.client.viewmodels.FailureHandlingViewModel

abstract class FailureHandlingActivity : AppCompatActivity(), FailureHandler {
    protected abstract val viewModel: FailureHandlingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.lastFailure.observe(this, Observer { onFailure(it) })
    }

    @CallSuper
    override fun onFailure(failure: Failure) {
        Log.e(getString(R.string.app_name), failure.throwable.message)
        Toast.makeText(this, getString(failure.error), Toast.LENGTH_SHORT).show()
    }
}