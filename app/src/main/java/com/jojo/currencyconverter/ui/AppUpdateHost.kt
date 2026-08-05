package com.jojo.currencyconverter.ui

import android.app.DownloadManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jojo.currencyconverter.data.AppRelease
import com.jojo.currencyconverter.data.AppUpdateRepository
import com.jojo.currencyconverter.data.isNewerVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

private const val UPDATE_PREFERENCES = "app_updates"
private const val IGNORED_VERSION = "ignored_version"
private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

private data class ActiveDownload(
    val id: Long,
    val release: AppRelease,
    val file: File,
    val progress: Int? = null,
)

private data class DownloadSnapshot(
    val status: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val failureReason: Int,
)

@Composable
fun AppUpdateHost(content: @Composable () -> Unit) {
    content()

    val context = LocalContext.current
    val repository = remember { AppUpdateRepository() }
    val preferences = remember {
        context.getSharedPreferences(UPDATE_PREFERENCES, Context.MODE_PRIVATE)
    }
    val currentVersion = remember(context) { currentVersionName(context) }
    var availableRelease by remember { mutableStateOf<AppRelease?>(null) }
    var permissionRelease by remember { mutableStateOf<AppRelease?>(null) }
    var activeDownload by remember { mutableStateOf<ActiveDownload?>(null) }
    var hideDownloadProgress by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }

    fun beginDownload(release: AppRelease) {
        runCatching { enqueueUpdate(context, release) }
            .onSuccess { download ->
                availableRelease = null
                hideDownloadProgress = false
                activeDownload = download
            }
            .onFailure {
                updateError = "无法开始下载，请稍后重试。"
            }
    }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        val release = permissionRelease
        permissionRelease = null
        if (release != null && context.packageManager.canRequestPackageInstalls()) {
            beginDownload(release)
        } else if (release != null) {
            updateError = "需要允许 Currency 安装更新文件。"
        }
    }

    LaunchedEffect(Unit) {
        val release = runCatching { repository.latestRelease() }.getOrNull()
            ?: return@LaunchedEffect
        val ignoredVersion = preferences.getString(IGNORED_VERSION, null)
        if (release.tagName != ignoredVersion &&
            isNewerVersion(release.versionName, currentVersion)
        ) {
            availableRelease = release
        }
    }

    LaunchedEffect(activeDownload?.id) {
        val download = activeDownload ?: return@LaunchedEffect
        while (true) {
            val snapshot = withContext(Dispatchers.IO) {
                queryDownload(context, download.id)
            }
            if (snapshot == null) {
                activeDownload = null
                updateError = "找不到更新下载任务，请重试。"
                return@LaunchedEffect
            }

            when (snapshot.status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val validationError = withContext(Dispatchers.IO) {
                        validateApk(context, download.file, download.release)
                    }
                    activeDownload = null
                    if (validationError != null) {
                        updateError = validationError
                    } else {
                        runCatching { launchInstaller(context, download.file) }
                            .onFailure { updateError = "无法打开系统安装器。" }
                    }
                    return@LaunchedEffect
                }

                DownloadManager.STATUS_FAILED -> {
                    activeDownload = null
                    updateError = "更新下载失败（错误 ${snapshot.failureReason}），请稍后重试。"
                    return@LaunchedEffect
                }

                else -> {
                    val progress = snapshot.totalBytes
                        .takeIf { it > 0L }
                        ?.let { total ->
                            ((snapshot.downloadedBytes * 100L) / total)
                                .toInt()
                                .coerceIn(0, 100)
                        }
                    activeDownload = activeDownload?.copy(progress = progress)
                }
            }
            delay(500)
        }
    }

    availableRelease?.let { release ->
        AlertDialog(
            onDismissRequest = { availableRelease = null },
            title = { Text("发现新版本 ${release.tagName}") },
            text = {
                Column {
                    Text("当前版本 $currentVersion")
                    release.notes.trim().takeIf { it.isNotEmpty() }?.let { notes ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = notes,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        availableRelease = null
                        if (context.packageManager.canRequestPackageInstalls()) {
                            beginDownload(release)
                        } else {
                            permissionRelease = release
                            Toast.makeText(
                                context,
                                "请允许 Currency 安装未知应用",
                                Toast.LENGTH_LONG,
                            ).show()
                            installPermissionLauncher.launch(
                                Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }
                    },
                ) {
                    Text("立即更新")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            preferences.edit()
                                .putString(IGNORED_VERSION, release.tagName)
                                .apply()
                            availableRelease = null
                        },
                    ) {
                        Text("不再提醒")
                    }
                    TextButton(onClick = { availableRelease = null }) {
                        Text("下次提醒")
                    }
                }
            },
        )
    }

    activeDownload?.takeUnless { hideDownloadProgress }?.let { download ->
        AlertDialog(
            onDismissRequest = { hideDownloadProgress = true },
            title = { Text("正在下载 ${download.release.tagName}") },
            text = {
                Column {
                    if (download.progress != null) {
                        LinearProgressIndicator(
                            progress = { download.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("${download.progress}%")
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text("正在连接下载服务器…")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { hideDownloadProgress = true }) {
                    Text("后台下载")
                }
            },
        )
    }

    updateError?.let { message ->
        AlertDialog(
            onDismissRequest = { updateError = null },
            title = { Text("更新失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { updateError = null }) {
                    Text("知道了")
                }
            },
        )
    }
}

@Suppress("DEPRECATION")
private fun currentVersionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"

private fun enqueueUpdate(context: Context, release: AppRelease): ActiveDownload {
    val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ?: error("External files directory is unavailable")
    val safeTag = release.tagName.replace(Regex("[^A-Za-z0-9._-]"), "-")
    val fileName = "Currency-$safeTag.apk"
    val file = File(directory, fileName)
    if (file.exists() && !file.delete()) error("Previous update file cannot be replaced")

    val request = DownloadManager.Request(Uri.parse(release.apkUrl))
        .setTitle("Currency ${release.tagName}")
        .setDescription("正在下载应用更新")
        .setMimeType(APK_MIME_TYPE)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalFilesDir(
            context,
            Environment.DIRECTORY_DOWNLOADS,
            fileName,
        )
    val manager = context.getSystemService(DownloadManager::class.java)
    return ActiveDownload(
        id = manager.enqueue(request),
        release = release,
        file = file,
    )
}

private fun queryDownload(context: Context, id: Long): DownloadSnapshot? {
    val manager = context.getSystemService(DownloadManager::class.java)
    val query = DownloadManager.Query().setFilterById(id)
    return manager.query(query)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        DownloadSnapshot(
            status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
            downloadedBytes = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
            ),
            totalBytes = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
            ),
            failureReason = cursor.getInt(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON),
            ),
        )
    }
}

private fun validateApk(context: Context, file: File, release: AppRelease): String? {
    if (!file.isFile || file.length() == 0L) return "下载的更新文件无效。"
    release.apkSha256?.let { expected ->
        if (sha256(file) != expected) return "更新文件校验失败，已阻止安装。"
    }

    @Suppress("DEPRECATION")
    val packageInfo = context.packageManager.getPackageArchiveInfo(
        file.absolutePath,
        PackageManager.GET_ACTIVITIES,
    )
    if (packageInfo?.packageName != context.packageName) {
        return "更新文件与当前应用不匹配，已阻止安装。"
    }
    @Suppress("DEPRECATION")
    val installedInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val archiveVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    val installedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        installedInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        installedInfo.versionCode.toLong()
    }
    if (archiveVersionCode <= installedVersionCode) {
        return "下载的 APK 不是更新版本，已阻止安装。"
    }
    return null
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }
}

private fun launchInstaller(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            clipData = ClipData.newRawUri("Currency update", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}
