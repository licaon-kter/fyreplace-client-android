package app.fyreplace.client.data.sources

import app.fyreplace.client.data.DataLoadingListener
import app.fyreplace.client.data.models.Post
import app.fyreplace.client.data.repositories.DraftRepository
import app.fyreplace.client.data.repositories.PostRepository

class ArchiveDataSourceFactory(
    private val listener: DataLoadingListener,
    private val postRepository: PostRepository
) :
    ItemsDataSourceFactory<Post>() {
    override fun newSource() = ArchiveDataSource(listener, postRepository)
}

class OwnPostsDataSourceFactory(
    private val listener: DataLoadingListener,
    private val postRepository: PostRepository
) :
    ItemsDataSourceFactory<Post>() {
    override fun newSource() = OwnPostsDataSource(listener, postRepository)
}

class DraftsDataSourceFactory(
    private val listener: DataLoadingListener,
    private val postRepository: DraftRepository
) :
    ItemsDataSourceFactory<Post>() {
    override fun newSource() = DraftsDataSource(listener, postRepository)
}
