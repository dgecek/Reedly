package oxim.digital.reedly.dagger.application.module;

import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oxim.digital.reedly.MainActivity;
import oxim.digital.reedly.dagger.application.ForApplication;
import oxim.digital.reedly.data.feed.service.FeedService;
import oxim.digital.reedly.data.feed.service.FeedServiceImpl;
import oxim.digital.reedly.data.feed.service.parser.FeedParser;
import oxim.digital.reedly.data.util.CurrentTimeProvider;
import oxim.digital.reedly.ui.feed.background.BackgroundFeedsUpdateService;
import oxim.digital.reedly.ui.feed.background.FeedUpdateJobInfoFactory;
import oxim.digital.reedly.domain.repository.FeedsUpdateScheduler;
import oxim.digital.reedly.ui.feed.background.FeedsUpdateSchedulerImpl;
import oxim.digital.reedly.ui.feed.background.JobSchedulerWrapper;
import oxim.digital.reedly.ui.feed.background.JobSchedulerWrapperImpl;
import oxim.digital.reedly.ui.feed.background.NotificationFactory;

@Module
public final class ServiceModule {

    private static final int FEEDS_UPDATE_JOB_ID = 1978;
    private static final int FEEDS_UPDATE_INTERVAL_MINS = 30;

    @Provides
    @Singleton
    FeedService provideFeedService(final FeedParser feedParser, final CurrentTimeProvider currentTimeProvider) {
        return new FeedServiceImpl(feedParser, currentTimeProvider);
    }

    @Provides
    @Singleton
    JobSchedulerWrapper provideJobSchedulerWrapper(final JobScheduler jobScheduler) {
        return new JobSchedulerWrapperImpl(jobScheduler);
    }

    @Provides
    @Singleton
    JobScheduler provideJobScheduler(final @ForApplication Context context) {
        return (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    }

    @Provides
    @Singleton
    FeedsUpdateScheduler provideFeedsUpdateScheduler(final JobInfo feedUpdateJobInfo, final JobSchedulerWrapper jobSchedulerWrapper) {
        return new FeedsUpdateSchedulerImpl(feedUpdateJobInfo, jobSchedulerWrapper);
    }

    @Provides
    @Singleton
    JobInfo provideFeedJobInfo(final ComponentName feedsUpdateJobService) {
        return FeedUpdateJobInfoFactory.createJobInfo(FEEDS_UPDATE_JOB_ID, FEEDS_UPDATE_INTERVAL_MINS, feedsUpdateJobService);
    }

    @Provides
    @Singleton
    NotificationFactory provideNotificationFactory(final Resources resources) {
        return new NotificationFactory(resources);
    }

    @Provides
    @Singleton
    ComponentName provideFeedsUpdateJobService(final @ForApplication Context context) {
        return new ComponentName(context, BackgroundFeedsUpdateService.class);
    }

    @Provides
    @Singleton
    PendingIntent provideFeedUpdateNotificationPendingIntent(final @ForApplication Context context) {
        final Intent targetActivityIntent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(context, 0, targetActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public interface Exposes {

        FeedService feedService();
    }
}
