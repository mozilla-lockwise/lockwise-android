package mozilla.lockbox.store

import android.content.Context
import android.net.ConnectivityManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.ReplaySubject
import mozilla.lockbox.action.NetworkAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher

open class NetworkStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) : ContextStore {

    lateinit var connectivityManager: ConnectivityManager
    internal val compositeDisposable = CompositeDisposable()

    private val isConnectedSubject: ReplaySubject<Boolean> = ReplaySubject.createWithSize(1)
    val isConnected: Observable<Boolean>

    val isConnectedState: Boolean
        get() = connectivityManager.activeNetworkInfo?.isConnected == true

    companion object {
        val shared = NetworkStore()
    }

    init {
        dispatcher.register
            .filterByType(NetworkAction::class.java)
            .subscribe {
                when (it) {
                    is NetworkAction.CheckConnectivity -> {
                        isConnectedSubject.onNext(isConnectedState)
                    }
                }
            }
            .addTo(compositeDisposable)

        isConnected = isConnectedSubject
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { checkConnectivity() }
    }

    private fun checkConnectivity() {
        isConnectedSubject.onNext(isConnectedState)
    }

    override fun injectContext(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
}