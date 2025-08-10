package com.richardlenin.momonastic

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import com.richardlenin.momonastic.ui.theme.MomonasticTheme
import org.json.JSONObject

data class AppInfo(
    val name: String = "", // name is the display name of the app
    val packageName: String = "", // package name is the unique identifier for the app
    val icon: Drawable? = null, // icon can be a drawable resource or a URL to an image
    val openedCount: Int = 0,
    val appType: String = "unknown" // type is like "game", "social", "utility", "payment", etc.
)

object AppCountTracker {
    private const val PREF_NAME = "app_count_prefs"
    private const val KEY_APP_COUNTS = "app_counts"

    private lateinit var prefs: SharedPreferences
    private val appCounts = mutableMapOf<String, Int>()

    /** Must be called once in Application or MainActivity */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadData()
    }

    fun incrementAppCount(packageName: String) {
        appCounts[packageName] = appCounts.getOrDefault(packageName, 0) + 1
        saveData()
    }

    fun getAppCount(packageName: String): Int {
        return appCounts.getOrDefault(packageName, 0)
    }

    private fun saveData() {
        val json = JSONObject(appCounts as Map<*, *>).toString()
        prefs.edit { putString(KEY_APP_COUNTS, json) }
    }

    private fun loadData() {
        val json = prefs.getString(KEY_APP_COUNTS, null) ?: return
        val jsonObject = JSONObject(json)
        appCounts.clear()
        jsonObject.keys().forEach { key ->
            appCounts[key] = jsonObject.getInt(key)
        }
    }
}

class MomoLauncherActivity : ComponentActivity() {
    lateinit var applist: List<AppInfo>

    val gpayPackageName = "com.google.android.apps.nbu.paisa.user" // Google Pay package name

    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCountTracker.init(this) // Initialize the app count tracker
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        applist = packageManager.queryIntentActivities(intent, 0)
            .map {
                AppInfo(
                    name = it.loadLabel(packageManager).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(packageManager),
                    openedCount = AppCountTracker.getAppCount(it.activityInfo.packageName), // Get the count of how many times the app was opened
                    appType = "unknown" // You can categorize apps here if needed
                )
            }

        onBackPressedDispatcher.addCallback(this) {}

        enableEdgeToEdge()
        setContent {
            var appListState by remember { mutableStateOf(applist) }
            val gpayApp = applist.find { it.packageName == gpayPackageName } ?: AppInfo()
            val painter = remember(gpayApp.icon) {
                gpayApp.icon?.let { BitmapPainter(drawableToBitmap(it).asImageBitmap()) }
            }
            MomonasticTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                onAppClick(gpayApp)
                            }
                        ) {
                            if (painter != null) {
                                Icon(
                                    painter = painter,
                                    contentDescription = "Open GPay",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                Text(
                                    text = "Open GPay",
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    // Clock widget at the top
                    val currentTime = remember { mutableLongStateOf(System.currentTimeMillis()) }
                    val date = remember { mutableStateOf("") }
                    val day = remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        while (true) {
                            currentTime.longValue = System.currentTimeMillis()
                            date.value = java.text.SimpleDateFormat(
                                "dd MMMM yyyy",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date(currentTime.longValue))
                            day.value = java.text.SimpleDateFormat(
                                "EEEE",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date(currentTime.longValue))
                            kotlinx.coroutines.delay(1000)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = java.text.SimpleDateFormat(
                                    "HH:mm:ss",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date(currentTime.longValue)),
                                fontSize = 46.sp,
                                modifier = Modifier
                                    .background(Color.Black)
                                    .clickable {
                                        //open the clock app
                                        val clockIntent =
                                            packageManager.getLaunchIntentForPackage("com.google.android.deskclock")
                                        if (clockIntent != null) {
                                            startActivity(clockIntent)
                                        } else {
                                            // Handle the case where the clock app cannot be launched
                                            // For example, show a toast or a dialog
                                        }
                                    },
                                textAlign = TextAlign.Center,
                                color = Color.White,
                            )
                            // date
                            Column {
                                Text(
                                    text = date.value,
                                    fontSize = 24.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp)
                                        .background(Color.Black)
                                        .clickable {
                                            //open the calendar app
                                            val calendarIntent =
                                                packageManager.getLaunchIntentForPackage("com.google.android.calendar")
                                            if (calendarIntent != null) {
                                                startActivity(calendarIntent)
                                            } else {
                                                // Handle the case where the calendar app cannot be launched
                                                // For example, show a toast or a dialog
                                            }
                                        },
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                )
                                Text(
                                    text = day.value,
                                    fontSize = 24.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp)
                                        .background(Color.Black)
                                        .clickable {
                                            //open the calendar app
                                            val calendarIntent =
                                                packageManager.getLaunchIntentForPackage("com.google.android.calendar")
                                            if (calendarIntent != null) {
                                                startActivity(calendarIntent)
                                            } else {
                                                // Handle the case where the calendar app cannot be launched
                                                // For example, show a toast or a dialog
                                            }
                                        },
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                )
                            }
                        }

                        AppListUI(
                            applist = appListState,
                            onAppClick = { appInfo ->
                                onAppClick(appInfo)
                                // Update the app list state to reflect the new count
                                applist = applist.map {
                                    if (it.packageName == appInfo.packageName) {
                                        it.copy(openedCount = AppCountTracker.getAppCount(it.packageName))
                                    } else {
                                        it
                                    }
                                }
                                appListState = applist
                            }
                        )
                    }
                }
            }
        }
    }

    fun onAppClick(appInfo: AppInfo) {
        AppCountTracker.incrementAppCount(appInfo.packageName)
       // open the app using its package name
        val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            // Handle the case where the app cannot be launched
            // For example, show a toast or a dialog
        }
    }

}

fun List<AppInfo>.sortAppListByCount(): List<AppInfo> {
    return sortedBy { it.name }
}

@Composable
fun AppListUI(modifier: Modifier = Modifier, applist: List<AppInfo> = listOf(), onAppClick: (AppInfo) -> Unit = {}) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scrollState),
    ){
        applist.sortAppListByCount().forEachIndexed { i, appName ->
            // crate an card with app name and add click listener
            Text(
                text = appName.name,
                fontSize = 36.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        if (i % 2 == 0) {
                            Color.DarkGray
                        } else {
                            Color.Black
                        }
                    )
                    .clickable {
                    onAppClick(appName)
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    AppListUI(
        modifier = modifier,
        applist = listOf(
            AppInfo(name = "App 1", packageName = "com.example.app1", icon = null),
            AppInfo(name = "App 2", packageName = "com.example.app2", icon = null)
        ),
        onAppClick = { appInfo ->
            // Handle app click
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MomonasticTheme {
        Greeting("Android")
    }
}


fun drawableToBitmap(drawable: Drawable): Bitmap {
    return when (drawable) {
        is BitmapDrawable -> drawable.bitmap
        is AdaptiveIconDrawable -> {
            val size = 108 // or any dp you want
            val bitmap = createBitmap(size, size)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
        else -> {
            val bitmap = createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1)
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }
}