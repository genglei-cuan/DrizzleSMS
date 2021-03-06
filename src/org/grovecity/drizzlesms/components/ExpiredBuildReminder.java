package org.grovecity.drizzlesms.components;

import android.content.Context;

import org.grovecity.drizzlesms.R;
import org.grovecity.drizzlesms.util.Util;

public class ExpiredBuildReminder extends Reminder {

  private static final String TAG = ExpiredBuildReminder.class.getSimpleName();

  public ExpiredBuildReminder() {
    super(R.drawable.ic_warning_dark,
          R.string.reminder_header_expired_build,
          R.string.reminder_header_expired_build_details);
  }

  @Override
  public boolean isDismissable() {
    return false;
  }

  public static boolean isEligible(Context context) {
    return !Util.isBuildFresh();
  }

}
