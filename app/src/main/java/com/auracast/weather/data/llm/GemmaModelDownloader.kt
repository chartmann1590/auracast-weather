package com.auracast.weather.data.llm

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads the Gemma 4 E2B on-device model (.litertlm) from the official
 * litert-community Hugging Face repo, shown to the user during onboarding (Phase 5/16).
 *
 * Model file confirmed live on the repo file listing (2026-08-27):
 * https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/main
 *   gemma-4-E2B-it.litertlm — 2.59 GB (generic CPU/GPU-portable build, used here)
 *
 * The MediaPipe LLM Inference API guidance ("the model is too large to be bundled in
 * an APK — host it and download at runtime") applies just as much to LiteRT-LM, so this
 * is an in-app HTTPS download to app-private storage, not a Play Asset Delivery pack —
 * simpler for a v1 and avoids an app-bundle-config dependency. Revisit as Play Asset
 * Delivery if install-time bundling is ever wanted (see plan/05-phase-gemma4-llm.md).
 */
sealed interface GemmaDownloadState {
    data object Idle : GemmaDownloadState
    data object CheckingExisting : GemmaDownloadState
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : GemmaDownloadState
    data object Verifying : GemmaDownloadState
    data class Complete(val file: File) : GemmaDownloadState
    data class Failed(val message: String) : GemmaDownloadState
    data object RequiresWifi : GemmaDownloadState
}

@Singleton
class GemmaModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    companion object {
        private const val MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
        private const val EXPECTED_SIZE_BYTES = 2_781_000_000L // ~2.59 GB, per the HF repo listing; used as a sanity check, not exact
        const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
    }

    fun modelFile(): File = File(File(context.filesDir, "models"), MODEL_FILE_NAME)

    fun isModelDownloaded(): Boolean {
        val f = modelFile()
        return f.exists() && f.length() > EXPECTED_SIZE_BYTES * 9 / 10 // allow some slack vs. the approximate size above
    }

    private fun isOnUnmeteredNetwork(): Boolean {
        val cm = context.getSystemService<ConnectivityManager>() ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /**
     * Streams the download with resumable byte-range support. Emits progress so the
     * onboarding screen (Phase 16 §5.1) can show a real progress bar, not a spinner.
     *
     * @param allowCellular if false (default), aborts with [GemmaDownloadState.RequiresWifi]
     *   when no unmetered network is available — mirrors the same Wi-Fi-only-by-default
     *   guidance Phase 7 already applies to ML Kit language packs, scaled up for a
     *   multi-gigabyte model file.
     */
    fun download(allowCellular: Boolean = false): Flow<GemmaDownloadState> = flow {
        emit(GemmaDownloadState.CheckingExisting)

        val target = modelFile()
        target.parentFile?.mkdirs()

        if (isModelDownloaded()) {
            emit(GemmaDownloadState.Complete(target))
            return@flow
        }

        if (!allowCellular && !isOnUnmeteredNetwork()) {
            emit(GemmaDownloadState.RequiresWifi)
            return@flow
        }

        val partial = File(target.parentFile, "$MODEL_FILE_NAME.part")
        var downloadedBytes = if (partial.exists()) partial.length() else 0L

        val requestBuilder = Request.Builder().url(MODEL_URL)
        if (downloadedBytes > 0) {
            requestBuilder.addHeader("Range", "bytes=$downloadedBytes-")
        }

        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        response.use { resp ->
            if (!resp.isSuccessful) {
                // A 416 (Range Not Satisfiable) means our partial file is already complete or corrupt — restart clean.
                if (resp.code == 416) {
                    partial.delete()
                    downloadedBytes = 0L
                }
                if (resp.code != 416) {
                    emit(GemmaDownloadState.Failed("Download failed: HTTP ${resp.code}"))
                    return@flow
                }
            }

            val body = resp.body ?: run {
                emit(GemmaDownloadState.Failed("Empty response body"))
                return@flow
            }

            val contentLength = body.contentLength()
            val totalBytes = if (resp.code == 206) downloadedBytes + contentLength else contentLength.takeIf { it > 0 } ?: EXPECTED_SIZE_BYTES

            RandomAccessFile(partial, "rw").use { raf ->
                raf.seek(downloadedBytes)
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var lastEmitBytes = downloadedBytes
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        raf.write(buffer, 0, read)
                        downloadedBytes += read
                        // Throttle progress emissions to ~every 1MB so we don't flood the UI thread's StateFlow collector.
                        if (downloadedBytes - lastEmitBytes > 1_000_000) {
                            emit(GemmaDownloadState.Downloading(downloadedBytes, totalBytes))
                            lastEmitBytes = downloadedBytes
                        }
                    }
                }
            }
            emit(GemmaDownloadState.Downloading(downloadedBytes, totalBytes))
        }

        emit(GemmaDownloadState.Verifying)
        if (partial.length() < EXPECTED_SIZE_BYTES * 9 / 10) {
            emit(GemmaDownloadState.Failed("Downloaded file is smaller than expected — retry needed"))
            return@flow
        }
        partial.renameTo(target)
        emit(GemmaDownloadState.Complete(target))
    }.flowOn(Dispatchers.IO)

    fun cancelPartialDownload() {
        File(File(context.filesDir, "models"), "$MODEL_FILE_NAME.part").delete()
    }
}
