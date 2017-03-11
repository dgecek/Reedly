package oxim.digital.reedly.ui.feed.background;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.util.Log;

public final class FeedsUpdateSchedulerImpl implements FeedsUpdateScheduler {

    private static final String TAG = FeedsUpdateSchedulerImpl.class.getSimpleName();

    private final JobInfo feedsUpdateJobInfo;
    private final JobSchedulerWrapper jobScheduler;

    public FeedsUpdateSchedulerImpl(final JobInfo feedsUpdateJobInfo, final JobSchedulerWrapper jobScheduler) {
        this.feedsUpdateJobInfo = feedsUpdateJobInfo;
        this.jobScheduler = jobScheduler;
    }

    @Override
    public void scheduleBackgroundFeedUpdates() {
        final int scheduleResult = jobScheduler.schedule(feedsUpdateJobInfo);
        checkScheduleResult(scheduleResult);
    }

    @Override
    public void cancelBackgroundFeedUpdates() {
        jobScheduler.cancel(feedsUpdateJobInfo.getId());
    }

    private void checkScheduleResult(final int scheduleResult) {
        if (scheduleResult != JobScheduler.RESULT_SUCCESS) {
            Log.e(TAG, "Failed to schedule background feeds update");
            // TODO - crashlytics
        }
    }
}
