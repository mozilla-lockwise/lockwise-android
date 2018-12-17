package mozilla.lockbox.store

import android.content.Context
import android.net.ConnectivityManager
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.NetworkAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.ReplaySubject

open class NetworkStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) : ContextStore {

    open lateinit var connectivityManager: ConnectivityManager
    internal val compositeDisposable = CompositeDisposable()

    open val stateSubject: ReplaySubject<Boolean> = ReplaySubject.createWithSize(1)
    open val networkAvailable: Observable<Boolean> get() = stateSubject

    open val networkConnectivityState: Boolean
        get() = connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true

    companion object {
        val shared = NetworkStore()
    }

    init {
        dispatcher.register
            .filterByType(NetworkAction::class.java)
            .subscribe {
                when (it) {
                    is NetworkAction.CheckConnectivity -> {
                        stateSubject.onNext(networkConnectivityState)
                    }
                }
            }
            .addTo(compositeDisposable)

        stateSubject.subscribe { state ->
            networkAvailable.doOnNext { state }
        }.addTo(compositeDisposable)
    }

    override fun injectContext(context: Context) {
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
}