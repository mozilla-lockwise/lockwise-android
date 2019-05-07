# Firefox Lockwise for Android Architecture

## RxKotlin

Firefox Lockwise for Android makes extensive use of RxKotlin, an implementation of the Observable pattern from ReactiveX. More information and many marble diagrams can be found in the [ReactiveX documentation](http://reactivex.io/). The rest of this document relies on a basic understanding of the reader of the ReactiveX-style Observer implementation. Their intro document is a [good starting point](http://reactivex.io/intro.html).

## Flux

### Architecture Pattern

In short, Flux architecture design maintains a unidirectional data flow, in which a global Dispatcher receives Actions & dispatches them to appropriate Stores. The Stores, in turn, process data & provide the source of truth for the Views. As users interact with the Views, any updates are made via a dispatched Action and the cycle begins again. See this [flux architecture](https://facebook.github.io/flux/docs/overview.html) writeup for more details on the original Flux architecture scheme.

Lockwise implements a modified version of the described architecture (LockFlux), keeping in mind that the original implementation ignores asynchronous work. In this implementation, all asynchronous work is handled by the Stores as they reduce actions and state updates to their observable state.

### Memory Management

The five major components of this architecture (`View`, `Presenter`, `Store`, `Dispatcher`, and `Action`) have distinct lifecycle management based on their functions.

`View`/`Presenter` pairs are allocated and de-allocated as views get displayed or hidden in turn.

`Store`s and the `Dispatcher` are global singleton objects, meaning that they get lazy-loaded by the application as their shared members get accessed by the `Presenter`s for view configuration or dispatching.

`Action`s get deallocated as soon as they reach the end observer for their intended function.

### View/Presenter

All views are bound to a presenter. In this separation, the presenter is responsible for all business logic, and the view is abstracted to a simple interface. The view is responsible for UI-specific configuration and passing user input to its presenter for handling. This allows any complex view-related configuration to be abstracted when dealing with business logic changes, and vice versa.

In the current implementation of LockFlux on Android, all `View`s are composed of Fragments, not Activities. Situations requiring an `Activity` to be added to the application will be reviewed on a case-to-case basis.

### Actions

Actions are tiny `enum`s or `sealed class`es that contain declarative language about either the triggering user action or the update request for a given `Store`. Actions can also be used to pass objects (item IDs, string resources, Telemetry events) between fragments.

### Dispatcher

The dispatcher class is the simplest in the application; it provides an `Action`-accepting method as a wrapper for the `PublishSubject<Action>` that publishes all dispatched actions to interested `Stores`:

```
class Dispatcher {
    companion object {
        val shared = Dispatcher()
    }

    private val actionSubject = PublishSubject.create<Action>()

    val register: Observable<Action> = this.actionSubject

    fun dispatch(action: Action) {
        this.actionSubject.onNext(action)
    }
}
```

### Store

Stores provide an opaque wrapper around system storage or simple `Replay- /Publish- Subject`s for the purposes of data access and view configuration. They selectively `register` with the `Dispatcher` for the `Action`s that they care about for updating state.

Stores also perform any asynchronous tasks that relate to the updating of their local state. The `Action`s dispatched by `Presenter`s are completely decoupled from observed `Store` state, removing the need for callback configurations.

It's important to note that there is no concept of ordering; all `Action`s will be delivered to `Store`s in realtime as they are dispatched.

### View Routing

The special case in this scenario is view routing. To handle the view-changing component of the architecture, there is a `RouteStore` observed by a `RoutePresenter` that rides along on the back of a `RootActivity`. This “containing” activity is not displayed to the user; rather, it performs the role of listening for navigation-specific `Action`s & performing the necessary top-level fragment swapping and back stack manipulation. Routing logic lives entirely separately from individual view configuration logic, allowing for modular view manipulation and easy testing.

### Benefits of Flux

Close readers will note that this document borrows heavily from a similar one in our [iOS application](https://mozilla-lockwise.github.io/lockwise-ios/architecture/).

The shared Flux pattern and reactive libraries allow us to "borrow" view-presentation logic from iOS as we move forward on Android.

Additionally, the separation of high-level view manipulation from view allows us to iterate quickly on implementation details and design feedback without implications for unrelated parts of the app.
