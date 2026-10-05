package org.lumengallery.app.ui.navigation

sealed class Screen(val route: String) {
    data object Timeline : Screen("timeline")
    data object Albums : Screen("albums")
    data object Favorites : Screen("favorites")
    data object Settings : Screen("settings")
    data object Trash : Screen("trash")

    data object AlbumDetail : Screen("album_detail/{bucketId}?albumName={albumName}") {
        fun createRoute(bucketId: Long, albumName: String) =
            "album_detail/$bucketId?albumName=${android.net.Uri.encode(albumName)}"
    }

    data object Viewer : Screen("viewer/{mediaId}?bucketId={bucketId}&isFavorites={isFavorites}") {
        fun createRoute(mediaId: Long, bucketId: Long? = null, isFavorites: Boolean = false): String {
            val bId = bucketId ?: -1L
            return "viewer/$mediaId?bucketId=$bId&isFavorites=$isFavorites"
        }
    }
}
