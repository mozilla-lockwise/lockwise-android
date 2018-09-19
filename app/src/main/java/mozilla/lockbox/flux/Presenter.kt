package mozilla.lockbox.flux

import android.support.annotation.CallSuper
import io.reactivex.disposables.CompositeDisposable

abstract class Presenter {
    val compositeDisposable: CompositeDisposable = CompositeDisposable()

    open fun onViewReady() {
    }

    @CallSuper
    open fun onDestroy() {
        compositeDisposable.clear()
    }
}
