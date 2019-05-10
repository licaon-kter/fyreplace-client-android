package net.wildfyre.client.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import net.wildfyre.client.data.Notification
import net.wildfyre.client.data.NotificationRepository

class NotificationsFragmentViewModel(application: Application) : FailureHandlingViewModel(application),
    ItemsListViewModel<Notification> {
    override val itemCount: LiveData<Long> =
        Transformations.map(NotificationRepository.superNotification) { it.count ?: 0 }
    override val items: LiveData<List<Notification>> = NotificationRepository.notifications

    override fun fetchNextItems() = NotificationRepository.fetchNextNotifications(this, true)

    override fun resetItems() = NotificationRepository.resetNotifications()

    fun clearItems() = NotificationRepository.clearNotifications(this)
}