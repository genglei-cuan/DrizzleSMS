package org.grovecity.drizzlesms.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.ContactsContract;
import android.support.v4.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.grovecity.drizzlesms.ApplicationPreferencesActivity;
import org.grovecity.drizzlesms.LogSubmitActivity;
import org.grovecity.drizzlesms.RegistrationActivity;
import org.grovecity.drizzlesms.contacts.ContactAccessor;
import org.grovecity.drizzlesms.contacts.ContactIdentityManager;
import org.grovecity.drizzlesms.crypto.MasterSecret;
import org.grovecity.drizzlesms.push.TextSecureCommunicationFactory;
import org.grovecity.drizzlesms.util.ProgressDialogAsyncTask;
import org.grovecity.drizzlesms.util.DrizzleSmsPreferences;
import org.grovecity.drizzlesms.R;
import org.whispersystems.libaxolotl.util.guava.Optional;
import org.whispersystems.textsecure.api.TextSecureAccountManager;
import org.whispersystems.textsecure.api.push.exceptions.AuthorizationFailedException;

import java.io.IOException;

public class AdvancedPreferenceFragment extends PreferenceFragment {
  private static final String TAG = AdvancedPreferenceFragment.class.getSimpleName();

  private static final String PUSH_MESSAGING_PREF   = "pref_toggle_push_messaging";
  private static final String SUBMIT_DEBUG_LOG_PREF = "pref_submit_debug_logs";

  private static final int PICK_IDENTITY_CONTACT = 1;

  private MasterSecret masterSecret;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);
    masterSecret = getArguments().getParcelable("master_secret");
    addPreferencesFromResource(R.xml.preferences_advanced);
    // Changes Made Here To Hide Push Messaging Toggle...
    //initializePushMessagingToggle();
    initializeIdentitySelection();

    /*this.findPreference(SUBMIT_DEBUG_LOG_PREF)
      .setOnPreferenceClickListener(new SubmitDebugLogListener());*/
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.preferences__advanced);
  }

  @Override
  public void onActivityResult(int reqCode, int resultCode, Intent data) {
    super.onActivityResult(reqCode, resultCode, data);

    Log.w(TAG, "Got result: " + resultCode + " for req: " + reqCode);
    if (resultCode == Activity.RESULT_OK && reqCode == PICK_IDENTITY_CONTACT) {
      handleIdentitySelection(data);
    }
  }

  private void initializePushMessagingToggle() {
    CheckBoxPreference preference = (CheckBoxPreference)this.findPreference(PUSH_MESSAGING_PREF);
    preference.setChecked(DrizzleSmsPreferences.isPushRegistered(getActivity()));
    preference.setOnPreferenceChangeListener(new PushMessagingClickListener());
  }

  private void initializeIdentitySelection() {
    ContactIdentityManager identity = ContactIdentityManager.getInstance(getActivity());

    Preference preference = this.findPreference(DrizzleSmsPreferences.IDENTITY_PREF);

    if (identity.isSelfIdentityAutoDetected()) {
      this.getPreferenceScreen().removePreference(preference);
    } else {
      Uri contactUri = identity.getSelfIdentityUri();

      if (contactUri != null) {
        String contactName = ContactAccessor.getInstance().getNameFromContact(getActivity(), contactUri);
        preference.setSummary(String.format(getString(R.string.ApplicationPreferencesActivity_currently_s),
                                            contactName));
      }

      preference.setOnPreferenceClickListener(new IdentityPreferenceClickListener());
    }
  }

  private class IdentityPreferenceClickListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      Intent intent = new Intent(Intent.ACTION_PICK);
      intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
      startActivityForResult(intent, PICK_IDENTITY_CONTACT);
      return true;
    }
  }

  private void handleIdentitySelection(Intent data) {
    Uri contactUri = data.getData();

    if (contactUri != null) {
      DrizzleSmsPreferences.setIdentityContactUri(getActivity(), contactUri.toString());
      initializeIdentitySelection();
    }
  }

  private class SubmitDebugLogListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      final Intent intent = new Intent(getActivity(), LogSubmitActivity.class);
      startActivity(intent);
      return true;
    }
  }

  private class PushMessagingClickListener implements Preference.OnPreferenceChangeListener {
    private static final int SUCCESS       = 0;
    private static final int NETWORK_ERROR = 1;

    private class DisablePushMessagesTask extends ProgressDialogAsyncTask<Void, Void, Integer> {
      private final CheckBoxPreference checkBoxPreference;

      public DisablePushMessagesTask(final CheckBoxPreference checkBoxPreference) {
        super(getActivity(), R.string.ApplicationPreferencesActivity_unregistering, R.string.ApplicationPreferencesActivity_unregistering_for_data_based_communication);
        this.checkBoxPreference = checkBoxPreference;
      }

      @Override
      protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        switch (result) {
        case NETWORK_ERROR:
          Toast.makeText(getActivity(),
                         R.string.ApplicationPreferencesActivity_error_connecting_to_server,
                         Toast.LENGTH_LONG).show();
          break;
        case SUCCESS:
          checkBoxPreference.setChecked(false);
          DrizzleSmsPreferences.setPushRegistered(getActivity(), false);
          break;
        }
      }

      @Override
      protected Integer doInBackground(Void... params) {
        try {
          Context                  context        = getActivity();
          TextSecureAccountManager accountManager = TextSecureCommunicationFactory.createManager(context);

          accountManager.setGcmId(Optional.<String>absent());
          GoogleCloudMessaging.getInstance(context).unregister();

          return SUCCESS;
        } catch (AuthorizationFailedException afe) {
          Log.w(TAG, afe);
          return SUCCESS;
        } catch (IOException ioe) {
          Log.w(TAG, ioe);
          return NETWORK_ERROR;
        }
      }
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, Object newValue) {
      if (((CheckBoxPreference)preference).isChecked()) {
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity());
        builder.setIconAttribute(R.attr.dialog_info_icon);
        builder.setTitle(R.string.ApplicationPreferencesActivity_disable_push_messages);
        builder.setMessage(R.string.ApplicationPreferencesActivity_this_will_disable_push_messages);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            new DisablePushMessagesTask((CheckBoxPreference)preference).execute();
          }
        });
        builder.show();
      } else {
        Intent nextIntent = new Intent(getActivity(), ApplicationPreferencesActivity.class);

        Intent intent = new Intent(getActivity(), RegistrationActivity.class);
        intent.putExtra("cancel_button", true);
        intent.putExtra("next_intent", nextIntent);
        intent.putExtra("master_secret", masterSecret);
        startActivity(intent);
      }

      return false;
    }
  }
}
