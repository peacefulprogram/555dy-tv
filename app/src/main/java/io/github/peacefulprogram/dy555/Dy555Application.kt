package io.github.peacefulprogram.dy555

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import cn.hutool.crypto.digest.MD5
import coil.ImageLoader
import coil.ImageLoaderFactory
import io.github.peacefulprogram.dy555.ext.showLongToast
import io.github.peacefulprogram.dy555.http.HttpDataRepository
import io.github.peacefulprogram.dy555.room.Dy555Database
import io.github.peacefulprogram.dy555.viewmodel.CategoriesViewModel
import io.github.peacefulprogram.dy555.viewmodel.HomeViewModel
import io.github.peacefulprogram.dy555.viewmodel.PlayHistoryViewModel
import io.github.peacefulprogram.dy555.viewmodel.PlaybackViewModel
import io.github.peacefulprogram.dy555.viewmodel.SearchResultViewModel
import io.github.peacefulprogram.dy555.viewmodel.SearchViewModel
import io.github.peacefulprogram.dy555.viewmodel.VideoDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class Dy555Application : Application(), ImageLoaderFactory {

    private val TAG = Dy555Application::class.java.simpleName

    override fun onCreate() {
        context = this
        startKoin {
            androidContext(this@Dy555Application)
            androidLogger()
            modules(httpModule(), viewModelModule(), roomModule())
        }
        reloadVideoServer()
        super.onCreate()
    }

    private fun roomModule() = module {
        single {
            Room.databaseBuilder(this@Dy555Application, Dy555Database::class.java, "dy555").apply {
                if (BuildConfig.DEBUG) {
                    val queryCallback = object : RoomDatabase.QueryCallback {
                        override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                            Log.i(TAG, "room sql: $sqlQuery  args: $bindArgs")
                        }
                    }
                    setQueryCallback(queryCallback, Executors.newSingleThreadExecutor())
                }
            }.build()
        }

        single {
            get<Dy555Database>().searchHistoryDao()
        }

        single {
            get<Dy555Database>().videoHistoryDao()
        }

        single {
            get<Dy555Database>().episodeHistoryDao()
        }

    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this).okHttpClient {
        OkHttpClient.Builder().hostnameVerifier { _, _ -> true }.addInterceptor { chain ->
            chain.request().newBuilder().header("user-agent", Constants.USER_AGENT)
                .header("referer", Constants.BASE_URL).build().let { chain.proceed(it) }
        }.sslSocketFactory(sslSocketFactory, trustManager).build()
    }.build()


    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set

        var ge_ua_key: String = ""

        val trustManager by lazy {
            object : X509TrustManager {
                override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
                }

                override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

            }
        }

        val sslSocketFactory = SSLContext.getInstance("SSL")
            .apply {
                init(null, arrayOf(trustManager), SecureRandom())
            }.socketFactory

    }

    private fun httpModule() = module {
        single {
            OkHttpClient.Builder()
                .hostnameVerifier { _, _ -> true }
                .addInterceptor { chain ->
                    val originalReq = chain.request()
                    val req = originalReq
                        .newBuilder()
                        .apply {
                            val ck = originalReq.header("cookie")
                            val headers = mutableListOf("searchneed=ok")
                            if (ck?.isNotEmpty() == true) {
                                headers.add(ck)
                            }
                            if (ge_ua_key.isNotEmpty()) {
                                headers.add("ge_ua_key=$ge_ua_key")
                            }
                            header("cookie", headers.joinToString(separator = ";"))
                            if (originalReq.header("user-agent")?.isNotEmpty() != true) {
                                header("user-agent", Constants.USER_AGENT)
                            }
                            if (originalReq.header("referer")?.isNotEmpty() != true) {
                                header("referer", Constants.BASE_URL + '/')
                            }
                        }
                        .build()
                    val resp = chain.proceed(req)
                    val guardCookie = Cookie.parseAll(resp.request.url, resp.headers)
                        .find { it.name == "guard" }
                        ?.value
                    if (guardCookie != null) {

                        val keyAndIv = MD5.create().digestHex(guardCookie.substring(0 until 8))
                            .chunked(2)
                            .map { it.toInt(0x10).toByte() }
                            .toByteArray()
                        val keySpec = SecretKeySpec(keyAndIv, "AES")
                        val cookieValue = Cipher.getInstance("AES/CBC/PKCS5Padding").run {
                            init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(keyAndIv))
                            // 网页点击事件的clientX clientY 以及他们的和
                            doFinal("{\"x\":804,\"y\":292,\"a\":1096}".toByteArray(Charsets.UTF_8))
                        }.let(cn.hutool.core.codec.Base64::encode)
                        req.newBuilder()
                            .header(
                                "cookie",
                                "guard=$guardCookie; guardrect=$cookieValue; searchneed=ok"
                            ).build().let { chain.proceed(it) }
                    } else {
                        resp
                    }

                }
                .apply {
                    if (BuildConfig.DEBUG) {
                        addNetworkInterceptor(HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.HEADERS
                        })
                    }
                }
                .build()
        }
        single { HttpDataRepository(get()) }
    }

    private fun viewModelModule() = module {
        viewModel { HomeViewModel(get()) }
        viewModel { parameters ->
            VideoDetailViewModel(
                videoId = parameters.get(), get(), get(), get()
            )
        }
        viewModel { PlaybackViewModel(get(), get(), get()) }
        viewModel { parameters -> CategoriesViewModel(get(), parameters.get()) }
        viewModel { SearchViewModel(get(), get()) }
        viewModel { parameters -> SearchResultViewModel(parameters.get(), get()) }
        viewModel { PlayHistoryViewModel(get()) }
    }

    fun reloadVideoServer() {
        val repository = get<HttpDataRepository>()
        val defaultVideoServer = "https://player.dwz0.cc:3653/api"
        GlobalScope.launch(Dispatchers.IO) {
            Constants.PLAY_URL_SERVER = try {
                repository.loadVideoServerUrl()
            } catch (ex: Exception) {
                Log.e(TAG, "reloadVideoServer: ${ex.message}", ex)
                withContext(Dispatchers.Main) {
                    context.showLongToast("加载视频服务器失败,将使用默认地址:${ex.message}")
                }
                defaultVideoServer
            }


        }
    }
}