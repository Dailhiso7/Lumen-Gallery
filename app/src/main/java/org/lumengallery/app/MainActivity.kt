package org.lumengallery.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.lumengallery.app.ui.albums.AlbumDetailScreen
import org.lumengallery.app.ui.albums.AlbumsScreen
import org.lumengallery.app.ui.albums.AlbumsViewModel
import org.lumengallery.app.ui.favorites.FavoritesScreen
import org.lumengallery.app.ui.navigation.Screen
import org.lumengallery.app.ui.settings.SettingsScreen
import org.lumengallery.app.ui.theme.LumenGalleryTheme
import org.lumengallery.app.ui.timeline.TimelineScreen
import org.lumengallery.app.ui.timeline.TimelineViewModel
import org.lumengallery.app.ui.trash.TrashScreen
import org.lumengallery.app.ui.trash.TrashViewModel
import org.lumengallery.app.ui.viewer.MediaViewerScreen
import org.lumengallery.app.ui.viewer.MediaViewerViewModel
import org.lumengallery.app.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private val timelineViewModel: TimelineViewModel by viewModels()
    private val albumsViewModel: AlbumsViewModel by viewModels()
    private val viewerViewModel: MediaViewerViewModel by viewModels()
    private val trashViewModel: TrashViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentLang by LocaleHelper.currentLanguage.collectAsState()
            val context = LocalContext.current
            val configuration = remember(currentLang) {
                val locale = LocaleHelper.getLocaleForLanguage(currentLang)
                Locale.setDefault(locale)
                val config = Configuration(context.resources.configuration)
                config.setLocale(locale)
                config.setLayoutDirection(locale)
                @Suppress("DEPRECATION")
                context.resources.updateConfiguration(config, context.resources.displayMetrics)
                config
            }

            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalActivityResultRegistryOwner provides this
            ) {
                LumenGalleryTheme {
                    LumenApp(
                        timelineViewModel = timelineViewModel,
                        albumsViewModel = albumsViewModel,
                        viewerViewModel = viewerViewModel,
                        trashViewModel = trashViewModel,
                        intent = intent,
                        checkPermission = ::hasRequiredStoragePermission
                    )
                }
            }
        }
    }

    private fun hasRequiredStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_VIDEO
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumenApp(
    timelineViewModel: TimelineViewModel,
    albumsViewModel: AlbumsViewModel,
    viewerViewModel: MediaViewerViewModel,
    trashViewModel: TrashViewModel,
    intent: Intent?,
    checkPermission: () -> Boolean
) {
    val navController = rememberNavController()
    var hasPermission by remember { mutableStateOf(checkPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val imgGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            val vidGranted = permissions[Manifest.permission.READ_MEDIA_VIDEO] ?: false
            imgGranted || vidGranted
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        }
        hasPermission = granted
        timelineViewModel.setPermissionGranted(granted)
    }

    LaunchedEffect(Unit) {
        val currentPermission = checkPermission()
        hasPermission = currentPermission
        timelineViewModel.setPermissionGranted(currentPermission)
        if (!currentPermission) {
            val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionLauncher.launch(required)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isFullscreenViewer = currentRoute?.startsWith("viewer/") == true
    val isMainScreen = currentRoute == Screen.Timeline.route ||
            currentRoute == Screen.Albums.route ||
            currentRoute == Screen.Favorites.route

    val screenTitle = when (currentRoute) {
        Screen.Albums.route -> stringResource(R.string.nav_albums)
        Screen.Favorites.route -> stringResource(R.string.nav_favorites)
        else -> stringResource(R.string.app_name)
    }

    Scaffold(
        topBar = {
            if (isMainScreen) {
                TopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        // Trash Shortcut Button
                        IconButton(onClick = { navController.navigate(Screen.Trash.route) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.trash_title),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Settings Button
                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isMainScreen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
                    // Timeline Tab
                    val isTimeline = currentRoute == Screen.Timeline.route
                    NavigationBarItem(
                        selected = isTimeline,
                        onClick = {
                            navController.navigate(Screen.Timeline.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isTimeline) Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary,
                                contentDescription = stringResource(R.string.nav_timeline)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_timeline)) }
                    )

                    // Albums Tab
                    val isAlbums = currentRoute == Screen.Albums.route
                    NavigationBarItem(
                        selected = isAlbums,
                        onClick = {
                            navController.navigate(Screen.Albums.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isAlbums) Icons.Filled.Folder else Icons.Outlined.Folder,
                                contentDescription = stringResource(R.string.nav_albums)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_albums)) }
                    )

                    // Favorites Tab
                    val isFavorites = currentRoute == Screen.Favorites.route
                    NavigationBarItem(
                        selected = isFavorites,
                        onClick = {
                            navController.navigate(Screen.Favorites.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isFavorites) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(R.string.nav_favorites)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_favorites)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreenViewer) androidx.compose.foundation.layout.PaddingValues() else innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Timeline.route
            ) {
                // Timeline
                composable(Screen.Timeline.route) {
                    TimelineScreen(
                        viewModel = timelineViewModel,
                        onRequestPermission = {
                            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    Manifest.permission.READ_MEDIA_IMAGES,
                                    Manifest.permission.READ_MEDIA_VIDEO
                                )
                            } else {
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            permissionLauncher.launch(perms)
                        },
                        onMediaClick = { mediaId ->
                            navController.navigate(Screen.Viewer.createRoute(mediaId))
                        }
                    )
                }

                // Albums
                composable(Screen.Albums.route) {
                    AlbumsScreen(
                        viewModel = albumsViewModel,
                        onAlbumClick = { bucketId, albumName ->
                            navController.navigate(Screen.AlbumDetail.createRoute(bucketId, albumName))
                        }
                    )
                }

                // Album Detail
                composable(
                    route = Screen.AlbumDetail.route,
                    arguments = listOf(
                        navArgument("bucketId") { type = NavType.LongType },
                        navArgument("albumName") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val bucketId = backStackEntry.arguments?.getLong("bucketId") ?: -1L
                    val rawName = backStackEntry.arguments?.getString("albumName") ?: ""
                    val albumName = Uri.decode(rawName).ifBlank { "Album" }
                    AlbumDetailScreen(
                        bucketId = bucketId,
                        albumName = albumName,
                        viewModel = albumsViewModel,
                        onBackClick = { navController.popBackStack() },
                        onMediaClick = { mediaId ->
                            navController.navigate(Screen.Viewer.createRoute(mediaId, bucketId))
                        }
                    )
                }

                // Favorites
                composable(Screen.Favorites.route) {
                    FavoritesScreen(
                        timelineViewModel = timelineViewModel,
                        onMediaClick = { mediaId ->
                            navController.navigate(Screen.Viewer.createRoute(mediaId, isFavorites = true))
                        }
                    )
                }

                // Settings Screen
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onBackClick = { navController.popBackStack() },
                        onOpenTrash = { navController.navigate(Screen.Trash.route) }
                    )
                }

                // Trash Screen
                composable(Screen.Trash.route) {
                    TrashScreen(
                        viewModel = trashViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Fullscreen Viewer
                composable(
                    route = Screen.Viewer.route,
                    arguments = listOf(
                        navArgument("mediaId") { type = NavType.LongType },
                        navArgument("bucketId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                        navArgument("isFavorites") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { backStackEntry ->
                    val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
                    val bucketId = backStackEntry.arguments?.getLong("bucketId") ?: -1L
                    val isFav = backStackEntry.arguments?.getBoolean("isFavorites") ?: false

                    MediaViewerScreen(
                        mediaId = mediaId,
                        bucketId = bucketId,
                        isFavorites = isFav,
                        viewModel = viewerViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
