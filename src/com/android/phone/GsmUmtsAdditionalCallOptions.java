package com.android.phone;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.util.Log;
import android.view.MenuItem;

import com.android.internal.telephony.Phone;

import java.util.ArrayList;

public class GsmUmtsAdditionalCallOptions extends TimeConsumingPreferenceActivity {
    private static final String LOG_TAG = "GsmUmtsAdditionalCallOptions";
    private final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private static final String BUTTON_CLIR_KEY  = "button_clir_key";
    private static final String BUTTON_CW_KEY    = "button_cw_key";
    private static final String BUTTON_PN_KEY    = "button_pn_key";

    private CLIRListPreference mCLIRButton;
    private CallWaitingSwitchPreference mCWButton;
    private MSISDNEditPreference mMSISDNButton;

    private final ArrayList<Preference> mPreferences = new ArrayList<Preference>();
    private int mInitIndex = 0;
    private Phone mPhone;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;

    private boolean mShowCLIRButton;
    private boolean mShowCWButton;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.gsm_umts_additional_options);

        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        mSubscriptionInfoHelper.setActionBarTitle(
                getActionBar(), getResources(), R.string.additional_gsm_call_settings_with_label);
        mPhone = mSubscriptionInfoHelper.getPhone();

        PreferenceScreen prefSet = getPreferenceScreen();
        mCLIRButton = (CLIRListPreference) prefSet.findPreference(BUTTON_CLIR_KEY);
        mCWButton = (CallWaitingSwitchPreference) prefSet.findPreference(BUTTON_CW_KEY);
        mMSISDNButton = (MSISDNEditPreference) prefSet.findPreference(BUTTON_PN_KEY);

        PersistableBundle b = null;
        if (mSubscriptionInfoHelper.hasSubId()) {
            b = PhoneGlobals.getInstance().getCarrierConfigForSubId(
                    mSubscriptionInfoHelper.getSubId());
        } else {
            b = PhoneGlobals.getInstance().getCarrierConfig();
        }

        if (b != null) {
            mShowCLIRButton = b.getBoolean(
                    CarrierConfigManager.KEY_ADDITIONAL_SETTINGS_CALLER_ID_VISIBILITY_BOOL);
            mShowCWButton = b.getBoolean(
                    CarrierConfigManager.KEY_ADDITIONAL_SETTINGS_CALL_WAITING_VISIBILITY_BOOL);
        }

        if (mCLIRButton != null) {
            if (mShowCLIRButton) {
                mPreferences.add(mCLIRButton);
            } else {
                prefSet.removePreference(mCLIRButton);
            }
        }

        if (mCWButton != null) {
            if (mShowCWButton) {
                mPreferences.add(mCWButton);
            } else {
                prefSet.removePreference(mCWButton);
            }
        }
		
        mPreferences.add(mMSISDNButton);

        if (mPreferences.size() != 0) {
            if (icicle == null) {
                if (DBG) Log.d(LOG_TAG, "start to init ");
                doPreferenceInit(mInitIndex);
            } else {
                if (DBG) Log.d(LOG_TAG, "restore stored states");
                mInitIndex = mPreferences.size();
                if (mShowCWButton) {
                    mCWButton.init(this, true, mPhone);
                }
                if (mShowCLIRButton) {
                    int[] clirArray = icicle.getIntArray(mCLIRButton.getKey());
                    if (clirArray != null) {
                        if (DBG) {
                            Log.d(LOG_TAG, "onCreate:  clirArray[0]="
                                    + clirArray[0] + ", clirArray[1]=" + clirArray[1]);
                        }
                        mCLIRButton.handleGetCLIRResult(clirArray);
                    } else {
                        if (isUtEnabledToDisableClir()) {
                            mCLIRButton.setSummary(R.string.sum_default_caller_id);
                        } else {
                            mCLIRButton.init(this, false, mPhone);
                        }
                    }
                mMSISDNButton.init(this, false, mPhone);
                }
            }
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private boolean isUtEnabledToDisableClir() {
        boolean skipClir = false;
        CarrierConfigManager configManager = (CarrierConfigManager)
            getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle pb = configManager.getConfigForSubId(mPhone.getSubId());
        if (pb != null) {
            skipClir = pb.getBoolean("config_disable_clir_over_ut");
        }
        return mPhone.isUtEnabled() && skipClir;
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mShowCLIRButton && mCLIRButton.clirArray != null) {
            outState.putIntArray(mCLIRButton.getKey(), mCLIRButton.clirArray);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        if (mInitIndex < mPreferences.size()-1 && !isFinishing()) {
            mInitIndex++;
            doPreferenceInit(mInitIndex);
        }
        super.onFinished(preference, reading);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            CallFeaturesSetting.goUpToTopLevelSetting(this, mSubscriptionInfoHelper);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void doPreferenceInit(int index) {
        if (mPreferences.size() != 0) {
            Preference pref = mPreferences.get(index);
            if (pref instanceof CallWaitingSwitchPreference) {
                ((CallWaitingSwitchPreference) pref).init(this, false, mPhone);
            } else if (pref instanceof MSISDNEditPreference) {
                ((MSISDNEditPreference) pref).init(this, false, mPhone);
            } else if (pref instanceof CLIRListPreference) {
                if (isUtEnabledToDisableClir()) {
                  ((CLIRListPreference) pref).setSummary(R.string.sum_default_caller_id);
                } else {
                  ((CLIRListPreference) pref).init(this, false, mPhone);
                }
            }
        }
    }
}
