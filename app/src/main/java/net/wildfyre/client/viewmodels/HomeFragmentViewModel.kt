package net.wildfyre.client.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.wildfyre.client.data.models.Post
import net.wildfyre.client.data.repositories.PostRepository

class HomeFragmentViewModel(application: Application) : PostFragmentViewModel(application) {
    private val postReserve: MutableList<Post> = mutableListOf()
    private var postReserveJob: Job? = null
    private var endOfPosts = false
    private var lastAreaName: String? = null

    fun nextPostAsync(areaName: String? = null) = viewModelScope.launch {
        if (areaName == lastAreaName) {
            return@launch
        }

        if (areaName != null) {
            lastAreaName = areaName
            postReserve.clear()
        }

        if (postReserve.isEmpty()) {
            setPost(null)
            fillReserve()
        }

        if (endOfPosts) {
            return@launch
        }

        if (hasContent.value != true) {
            _hasContent.postValue(true)
        }

        setPost(postReserve.removeAt(0))

        if (postReserve.size <= RESERVE_SIZE / 2) {
            queueJob()
        }
    }

    fun spreadAsync(spread: Boolean) = launchCatching(Dispatchers.IO) {
        PostRepository.spread(postAreaName, postId, spread)
        nextPostAsync().join()
    }

    private suspend fun fillReserve() {
        endOfPosts = false
        postReserveJob?.join()

        while (!endOfPosts && postReserve.isEmpty()) {
            queueJob()
            postReserveJob?.join()
        }
    }

    private fun queueJob() {
        postReserveJob = launchCatching {
            val superPost = withContext(Dispatchers.IO) { PostRepository.getNextPosts(RESERVE_SIZE) }

            if (superPost.count == 0) {
                _hasContent.postValue(false)
                endOfPosts = true
            } else {
                postReserve.addAll(superPost.results.filter { p -> postReserve.find { it.id == p.id } == null })
            }

            postReserveJob = null
        }
    }

    private companion object {
        const val RESERVE_SIZE = 6
    }
}
