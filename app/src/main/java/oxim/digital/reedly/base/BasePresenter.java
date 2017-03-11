package oxim.digital.reedly.base;

import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Named;

import oxim.digital.reedly.configuration.ViewActionQueue;
import oxim.digital.reedly.configuration.ViewActionQueueProvider;
import oxim.digital.reedly.dagger.application.module.ThreadingModule;
import oxim.digital.reedly.data.util.connectivity.ConnectivityReceiver;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public abstract class BasePresenter<View extends BaseView> implements ScopedPresenter {

    @Inject
    protected ViewActionQueueProvider viewActionQueueProvider;

    @Inject
    ConnectivityReceiver connectivityReceiver;

    @Inject
    @Named(ThreadingModule.MAIN_SCHEDULER)
    Scheduler mainThreadScheduler;

    private WeakReference<View> viewReference = new WeakReference<>(null);
    private Subscription viewActionsSubscription;

    protected String viewId;
    protected ViewActionQueue<View> viewActionQueue;

    private CompositeSubscription subscriptions = new CompositeSubscription();

    public BasePresenter(final View view) {
        viewReference = new WeakReference<>(view);
        Log.i("PRES", "Created presenter for -> " + view);
    }

    @Override
    @CallSuper
    public void start() {
        viewId = getIfViewNotNull(BaseView::getViewId, "");
        viewActionQueue = viewActionQueueProvider.queueFor(viewId);
        Log.i("PRES", "Started presenter for -> " + viewId);
        subscribeToConnectivityChange();
    }

    private void subscribeToConnectivityChange() {
        addSubscription(connectivityReceiver.getConnectivityStatus()
                                            .observeOn(mainThreadScheduler)
                                            .subscribeOn(Schedulers.io())
                                            .subscribe(this::onConnectivityChange, this::logError));
    }

    protected void onConnectivityChange(final boolean isConnected) {
        // Template method
    }

    @Override
    @CallSuper
    public void activate() {
        Log.i("PRES", "Activating");
        viewActionsSubscription = viewActionQueue.viewActionsObservable()
                                                 .observeOn(mainThreadScheduler)
                                                 .subscribe(this::onViewAction);
        viewActionQueue.resume();
    }

    protected void onViewAction(final Action1<View> viewAction) {
        doIfViewNotNull(viewAction::call);
    }

    @Override
    @CallSuper
    public void deactivate() {
        Log.i("PRES", "Deactivate");
        viewActionQueue.pause();
        viewActionsSubscription.unsubscribe();
        subscriptions.clear();
    }

    @Override
    @CallSuper
    public void destroy() {
        Log.i("PRES", "Destroy");
        viewActionQueue.destroy();
        viewActionQueueProvider.dispose(viewId);
    }

    private void addSubscription(final Subscription subscription) {
        if (subscriptions == null || subscriptions.isUnsubscribed()) {
            subscriptions = new CompositeSubscription();
        }
        subscriptions.add(subscription);
    }

    protected final void doIfConnectedToInternet(final Action0 ifConnected, final Action0 ifNotConnected) {
        addSubscription(connectivityReceiver.isConnected()
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(mainThreadScheduler)
                                            .subscribe(isConnected -> onConnectedToInternet(isConnected, ifConnected, ifNotConnected), this::logError)
        );
    }

    private void onConnectedToInternet(final boolean isConnected, final Action0 ifConnected, final Action0 ifNotConnected) {
        ((isConnected) ? ifConnected : ifNotConnected).call();
    }

    protected final void logError(final Throwable throwable) {
        if (!TextUtils.isEmpty(throwable.getMessage())) {
            Log.e(getClass().getSimpleName(), throwable.getMessage(), throwable);
        }
        // TODO - crashlytics
    }

    protected void doIfViewNotNull(final Action1<View> whenViewNotNull) {
        final View view = getNullableView();
        if (view != null) {
            whenViewNotNull.call(view);
        }
    }

    protected <R> R getIfViewNotNull(final Func1<View, R> whenViewNotNull, final R defaultValue) {
        final View view = getNullableView();
        if (view != null) {
            return whenViewNotNull.call(view);
        }
        return defaultValue;
    }

    @Nullable
    protected View getNullableView() {
        return viewReference.get();
    }
}
