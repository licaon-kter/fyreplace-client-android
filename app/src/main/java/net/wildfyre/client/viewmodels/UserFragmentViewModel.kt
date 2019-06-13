package net.wildfyre.client.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import net.wildfyre.client.data.Author
import net.wildfyre.client.data.repositories.AuthorRepository

class UserFragmentViewModel(application: Application) : FailureHandlingViewModel(application) {
    private val _author = MutableLiveData<Author>()

    val author: LiveData<Author> = _author

    fun setAuthor(a: Author) = _author.postValue(a)

    fun setUserIdAsync(id: Long) = launchCatching(Dispatchers.IO) { _author.postValue(AuthorRepository.getUser(id)) }
}
