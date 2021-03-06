/*
 * Copyright 2013 JNRain
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jnrain.mobile.accounts.kbs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import name.xen0n.cytosol.ui.util.DialogHelper;
import name.xen0n.cytosol.ui.util.FormatHelper;
import name.xen0n.cytosol.ui.widget.GuidedEditText;
import name.xen0n.cytosol.util.TelephonyHelper;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jnrain.mobile.R;
import org.jnrain.mobile.ui.base.JNRainActivity;
import org.jnrain.mobile.ui.base.RegisterPoint;
import org.jnrain.mobile.ui.kbs.KBSUIConstants;

import roboguice.inject.InjectView;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;


public class KBSRegisterActivity extends JNRainActivity<KBSRegisterResult>
        implements RegisterPoint {
    public static final String PARAM_NEW_UID = "org.jnrain.mobile.kbs.newuid";
    public static final String PARAM_NEW_PSW = "org.jnrain.mobile.kbs.newpsw";

    @InjectView(R.id.textRegisterDisclaimer)
    TextView textRegisterDisclaimer;

    @InjectView(R.id.editNewUID)
    GuidedEditText editNewUID;
    @InjectView(R.id.editNewEmail)
    GuidedEditText editNewEmail;
    @InjectView(R.id.editNewPassword)
    GuidedEditText editNewPassword;
    @InjectView(R.id.editRetypeNewPassword)
    GuidedEditText editRetypeNewPassword;
    @InjectView(R.id.editNewNickname)
    GuidedEditText editNewNickname;

    @InjectView(R.id.editStudID)
    GuidedEditText editStudID;
    @InjectView(R.id.editRealName)
    GuidedEditText editRealName;

    @InjectView(R.id.textEthnicMinorityIndicator)
    TextView textEthnicMinorityIndicator;
    @InjectView(R.id.checkIsEthnicMinority)
    CheckBox checkIsEthnicMinority;

    @InjectView(R.id.checkUseCurrentPhone)
    CheckBox checkUseCurrentPhone;
    @InjectView(R.id.editPhone)
    GuidedEditText editPhone;

    @InjectView(R.id.imageRegCaptcha)
    ImageView imageRegCaptcha;
    @InjectView(R.id.editCaptcha)
    GuidedEditText editCaptcha;

    @InjectView(R.id.btnSubmitRegister)
    Button btnSubmitRegister;

    // private static final String TAG = "KBSRegisterActivity";
    private static final int FIXED_VALIDATION_FIELD_IDS[] = {
            R.id.editNewUID,
            R.id.editNewEmail,
            R.id.editNewPassword,
            R.id.editRetypeNewPassword,
            R.id.editNewNickname,
            R.id.editStudID,
            R.id.editRealName,
            // R.id.editPhone,
            R.id.editCaptcha
    };

    private static final String EMAIL_CHECK_RE = "^[0-9A-Za-z._\\-]+(?:[+][0-9A-Za-z._\\-]+)?@[0-9A-Za-z._\\-]+\\.[A-Za-z]+$";
    private static final Pattern EMAIL_CHECKER = Pattern
        .compile(EMAIL_CHECK_RE);

    private static final String IDENT_CHECK_RE = "(?:\\d{10})|(?:[Ss]\\d{9})|(?:\\d{7})";
    private static final Pattern IDENT_CHECKER = Pattern
        .compile(IDENT_CHECK_RE);

    private ProgressDialog loadingDlg;
    private Handler mHandler;

    private String currentPhoneNumber;
    private boolean isCurrentPhoneNumberAvailable;

    private Timer delayedUIDChecker;
    private long lastUIDCheckTime;

    private SparseBooleanArray validatedMap;

    protected synchronized void initValidationMapping() {
        if (validatedMap == null) {
            validatedMap = new SparseBooleanArray();

            for (int id : FIXED_VALIDATION_FIELD_IDS) {
                validatedMap.put(id, false);
            }

            // dynamic fields
            validatedMap.put(R.id.editPhone, false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kbs_register);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                loadingDlg = DialogHelper.showProgressDialog(
                        KBSRegisterActivity.this,
                        R.string.check_updates_dlg_title,
                        R.string.please_wait,
                        false,
                        false);
            }
        };

        lastUIDCheckTime = 0;

        initValidationMapping();

        // instance state
        if (savedInstanceState != null) {
            /*
             * editNewUID.onRestoreInstanceState(savedInstanceState
             * .getParcelable("newUID"));
             * editNewEmail.onRestoreInstanceState(savedInstanceState
             * .getParcelable("newEmail"));
             * editNewPassword.onRestoreInstanceState(savedInstanceState
             * .getParcelable("newPass"));
             * editRetypeNewPassword.onRestoreInstanceState
             * (savedInstanceState .getParcelable("repeatPass"));
             * editNewNickname.onRestoreInstanceState(savedInstanceState
             * .getParcelable("newNickname"));
             * editStudID.onRestoreInstanceState(savedInstanceState
             * .getParcelable("studID"));
             * editRealName.onRestoreInstanceState(savedInstanceState
             * .getParcelable("realname"));
             * editPhone.onRestoreInstanceState(savedInstanceState
             * .getParcelable("phone"));
             * editCaptcha.onRestoreInstanceState(savedInstanceState
             * .getParcelable("captcha"));
             */

            // captcha image
            byte[] captchaPNG = savedInstanceState
                .getByteArray("captchaImage");
            ByteArrayInputStream captchaStream = new ByteArrayInputStream(
                    captchaPNG);
            try {
                Drawable captchaDrawable = BitmapDrawable.createFromStream(
                        captchaStream,
                        "src");
                imageRegCaptcha.setImageDrawable(captchaDrawable);
            } finally {
                try {
                    captchaStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        // event handlers
        // UID
        editNewUID.addTextChangedListener(new StrippedDownTextWatcher() {
            long lastCheckTime = 0;
            String lastUID;

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {
                // FIXME: use monotonic clock...
                long curtime = System.currentTimeMillis();
                if (curtime - lastCheckTime >= KBSUIConstants.REG_CHECK_UID_INTERVAL_MILLIS) {
                    String uid = s.toString();

                    // don't check at the first char
                    if (uid.length() > 1) {
                        checkUIDAvailability(uid, curtime);
                    }

                    lastCheckTime = curtime;
                    lastUID = uid;

                    // schedule a new delayed check
                    if (delayedUIDChecker != null) {
                        delayedUIDChecker.cancel();
                        delayedUIDChecker.purge();
                    }
                    delayedUIDChecker = new Timer();
                    delayedUIDChecker.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            final String uid = getUID();

                            if (uid != lastUID) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        checkUIDAvailability(
                                                uid,
                                                System.currentTimeMillis());
                                    }
                                });

                                lastUID = uid;
                            }
                        }
                    },
                            KBSUIConstants.REG_CHECK_UID_INTERVAL_MILLIS);
                }
            }
        });

        editNewUID.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    // lost focus, force check uid availability
                    checkUIDAvailability(
                            getUID(),
                            System.currentTimeMillis());
                } else {
                    // inputting, temporarily clear that notice
                    updateUIDAvailability(false, 0);
                }
            }
        });

        // E-mail
        editNewEmail.addTextChangedListener(new StrippedDownTextWatcher() {
            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {
                if (TextUtils.isEmpty(s)) {
                    updateValidation(
                            editNewEmail,
                            false,
                            true,
                            R.string.reg_email_empty);
                    return;
                }

                if (!EMAIL_CHECKER.matcher(s).matches()) {
                    updateValidation(
                            editNewEmail,
                            false,
                            true,
                            R.string.reg_email_malformed);
                    return;
                }

                updateValidation(editNewEmail, true, true, R.string.ok_short);
            }
        });

        // Password
        editNewPassword
            .addTextChangedListener(new StrippedDownTextWatcher() {
                @Override
                public void onTextChanged(
                        CharSequence s,
                        int start,
                        int before,
                        int count) {
                    if (TextUtils.isEmpty(s)) {
                        updateValidation(
                                editNewPassword,
                                false,
                                true,
                                R.string.reg_psw_empty);
                        return;
                    }

                    if (s.length() < 6) {
                        updateValidation(
                                editNewPassword,
                                false,
                                true,
                                R.string.reg_psw_too_short);
                        return;
                    }

                    if (s.length() > 39) {
                        updateValidation(
                                editNewPassword,
                                false,
                                true,
                                R.string.reg_psw_too_long);
                        return;
                    }

                    if (getUID().equalsIgnoreCase(s.toString())) {
                        updateValidation(editNewPassword, false, true, 0);
                    }

                    updateValidation(
                            editNewPassword,
                            true,
                            true,
                            R.string.ok_short);

                    updateRetypedPasswordCorrectness();
                }
            });

        // Retype password
        editRetypeNewPassword
            .addTextChangedListener(new StrippedDownTextWatcher() {
                @Override
                public void onTextChanged(
                        CharSequence s,
                        int start,
                        int before,
                        int count) {
                    updateRetypedPasswordCorrectness();
                }
            });

        // Nickname
        editNewNickname
            .addTextChangedListener(new StrippedDownTextWatcher() {
                @Override
                public void onTextChanged(
                        CharSequence s,
                        int start,
                        int before,
                        int count) {
                    if (TextUtils.isEmpty(s)) {
                        updateValidation(
                                editNewNickname,
                                false,
                                true,
                                R.string.reg_nick_empty);
                        return;
                    }

                    updateValidation(
                            editNewNickname,
                            true,
                            true,
                            R.string.ok_short);
                }
            });

        // Student ID
        editStudID.addTextChangedListener(new StrippedDownTextWatcher() {
            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {
                if (TextUtils.isEmpty(s)) {
                    updateValidation(
                            editStudID,
                            false,
                            true,
                            R.string.reg_stud_id_empty);
                    return;
                }

                if (!IDENT_CHECKER.matcher(s).matches()) {
                    updateValidation(
                            editStudID,
                            false,
                            true,
                            R.string.reg_stud_id_malformed);
                    return;
                }

                updateValidation(editStudID, true, true, R.string.ok_short);
            }
        });

        // Real name
        editRealName.addTextChangedListener(new StrippedDownTextWatcher() {
            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {
                if (TextUtils.isEmpty(s)) {
                    updateValidation(
                            editRealName,
                            false,
                            true,
                            R.string.reg_realname_empty);
                    return;
                }

                updateValidation(editRealName, true, true, R.string.ok_short);
            }
        });

        // Phone
        editPhone.addTextChangedListener(new StrippedDownTextWatcher() {
            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {
                if (TextUtils.isEmpty(s)) {
                    updateValidation(
                            editPhone,
                            false,
                            true,
                            R.string.reg_phone_empty);
                    return;
                }

                if (!TextUtils.isDigitsOnly(s)) {
                    updateValidation(
                            editPhone,
                            false,
                            true,
                            R.string.reg_phone_onlynumbers);
                    return;
                }

                updateValidation(editPhone, true, true, R.string.ok_short);
            }
        });

        // Captcha
        editCaptcha.addTextChangedListener(new StrippedDownTextWatcher() {
            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {
                if (TextUtils.isEmpty(s)) {
                    updateValidation(
                            editCaptcha,
                            false,
                            true,
                            R.string.reg_captcha_empty);
                    return;
                }

                updateValidation(editCaptcha, true, true, R.string.ok_short);
            }
        });

        // Use current phone
        checkUseCurrentPhone
            .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                        CompoundButton buttonView,
                        boolean isChecked) {
                    setUseCurrentPhone(isChecked);
                }
            });

        // Is ethnic minority
        checkIsEthnicMinority
            .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                        CompoundButton buttonView,
                        boolean isChecked) {
                    setEthnicMinority(isChecked);
                }
            });

        // Submit form!
        btnSubmitRegister.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // progress dialog
                mHandler.sendMessage(new Message());

                // prevent repeated requests
                setSubmitButtonEnabled(false);

                // fire our biiiiiig request!
                // TODO: 性别默认成了妹子23333
                final String uid = getUID();
                final String psw = editNewPassword.getText().toString();
                makeSpiceRequest(new KBSRegisterRequest(
                        uid,
                        psw,
                        editNewNickname.getText().toString(),
                        getRealName(),
                        editStudID.getText().toString(),
                        editNewEmail.getText().toString(),
                        getPhone(),
                        editCaptcha.getText().toString(),
                        2), new KBSRegisterRequestListener(
                        KBSRegisterActivity.this,
                        uid,
                        psw));
            }
        });

        // interface init
        // UID availability
        updateUIDAvailability(false, 0);

        // HTML-formatted register disclaimer
        FormatHelper.setHtmlText(
                this,
                textRegisterDisclaimer,
                R.string.register_disclaimer);
        textRegisterDisclaimer.setMovementMethod(LinkMovementMethod
            .getInstance());

        // is ethnic minority defaults to false
        checkIsEthnicMinority.setChecked(false);
        setEthnicMinority(false);

        // current phone number
        currentPhoneNumber = TelephonyHelper.getPhoneNumber(this);
        isCurrentPhoneNumberAvailable = currentPhoneNumber != null;

        if (isCurrentPhoneNumberAvailable) {
            // display the obtained number as hint
            FormatHelper.setHtmlText(
                    this,
                    checkUseCurrentPhone,
                    R.string.field_use_current_phone,
                    currentPhoneNumber);
        } else {
            // phone number unavailable, disable the choice
            checkUseCurrentPhone.setEnabled(false);
            checkUseCurrentPhone.setVisibility(View.GONE);
        }

        // default to use current phone number if available
        checkUseCurrentPhone.setChecked(isCurrentPhoneNumberAvailable);
        setUseCurrentPhone(isCurrentPhoneNumberAvailable);

        if (savedInstanceState == null) {
            // issue preflight request
            // load captcha in success callback
            this.makeSpiceRequest(
                    new KBSRegisterRequest(),
                    new KBSRegisterRequestListener(this));
        }
    }

    @Override
    protected void onStop() {
        if (delayedUIDChecker != null) {
            delayedUIDChecker.cancel();
        }

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        /*
         * outState.putParcelable("newUID",
         * editNewUID.onSaveInstanceState()); outState.putParcelable(
         * "newEmail", editNewEmail.onSaveInstanceState());
         * outState.putParcelable( "newPass",
         * editNewPassword.onSaveInstanceState()); outState.putParcelable(
         * "repeatPass", editRetypeNewPassword.onSaveInstanceState());
         * outState.putParcelable( "newNickname",
         * editNewNickname.onSaveInstanceState());
         * outState.putParcelable("studID",
         * editStudID.onSaveInstanceState()); outState.putParcelable(
         * "realname", editRealName.onSaveInstanceState());
         * outState.putParcelable("phone", editPhone.onSaveInstanceState());
         * outState.putParcelable("captcha",
         * editCaptcha.onSaveInstanceState());
         */

        // captcha image
        ByteArrayOutputStream captchaOutStream = new ByteArrayOutputStream();
        Bitmap captchaBitmap = ((BitmapDrawable) imageRegCaptcha
            .getDrawable()).getBitmap();
        captchaBitmap.compress(CompressFormat.PNG, 100, captchaOutStream);

        outState
            .putByteArray("captchaImage", captchaOutStream.toByteArray());
    }

    public ProgressDialog getLoadingDialog() {
        return loadingDlg;
    }

    @Override
    public void onRegisterSuccess(String uid, String psw) {
        final Intent resultIntent = new Intent();

        resultIntent.putExtra(PARAM_NEW_UID, uid);
        resultIntent.putExtra(PARAM_NEW_PSW, psw);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    public boolean isUseCurrentPhone() {
        return checkUseCurrentPhone.isChecked();
    }

    protected String getPhoneInput() {
        return editPhone.getText().toString().trim();
    }

    public String getPhone() {
        if (!isCurrentPhoneNumberAvailable) {
            return getPhoneInput();
        }

        return checkUseCurrentPhone.isChecked()
                ? currentPhoneNumber
                : getPhoneInput();
    }

    public synchronized void setUseCurrentPhone(boolean useCurrent) {
        editPhone.setEnabled(!useCurrent);
        editPhone.setVisibility(useCurrent ? View.GONE : View.VISIBLE);
    }

    public String getRealName() {
        String name = editRealName.getText().toString().trim();

        // TODO: consider foreign students who have a "（留）" suffix
        // appended to their last names WRITTEN IN UPPERCASE
        // Note this is Jiangnan University-specific.
        return checkIsEthnicMinority.isChecked() ? ('☆' + name) : name;
    }

    public synchronized void setEthnicMinority(boolean isEthnicMinority) {
        textEthnicMinorityIndicator.setVisibility(isEthnicMinority
                ? View.VISIBLE
                : View.GONE);
    }

    @Override
    public void updateCaptcha(Drawable captcha) {
        imageRegCaptcha.setImageDrawable(captcha);
        imageRegCaptcha.invalidate();
    }

    @Override
    public void setSubmitButtonEnabled(boolean enabled) {
        btnSubmitRegister.setEnabled(enabled);
    }

    @Override
    public String getUID() {
        return editNewUID.getText().toString().trim();
    }

    @Override
    public void checkUIDAvailability(String uid, long timestamp) {
        if (uid.length() == 0) {
            // directly "return"
            updateUIDAvailability(false, R.string.reg_uid_empty);
            return;
        }

        makeSpiceRequest(
                new KBSCheckIDRequest(uid),
                new KBSCheckIDRequestListener(this, timestamp));
    }

    @Override
    public void notifyUIDAvailability(int status, long timestamp) {
        switch (status) {
            case 0:
                // Success
                updateUIDAvailability(true, R.string.ok_short, timestamp);
                break;

            case 1:
                // UID empty
                updateUIDAvailability(
                        false,
                        R.string.reg_uid_empty,
                        timestamp);
                break;

            case 2:
                // Already exists
                updateUIDAvailability(
                        false,
                        R.string.reg_uid_exists,
                        timestamp);
                break;

            case 3:
                // Too short
                updateUIDAvailability(
                        false,
                        R.string.reg_uid_too_short,
                        timestamp);
                break;

            case 4:
                // Too long
                updateUIDAvailability(
                        false,
                        R.string.reg_uid_too_long,
                        timestamp);
                break;

            case 5:
                // Forbidden characters
                updateUIDAvailability(
                        false,
                        R.string.reg_uid_malformed,
                        timestamp);
                break;

            case 6:
                // Banned
                updateUIDAvailability(
                        false,
                        R.string.reg_uid_banned,
                        timestamp);
                break;

            default:
                break;
        }
    }

    protected synchronized void updateUIDAvailability(
            boolean ok,
            int statusResId,
            long timestamp) {
        if (timestamp < lastUIDCheckTime) {
            // results of an earlier check arrived late, discard it
            return;
        }

        updateUIDAvailability(ok, statusResId);
    }

    protected synchronized void updateUIDAvailability(
            boolean ok,
            int statusResId) {
        if (statusResId != 0) {
            updateValidation(editNewUID, ok, true, statusResId);
        } else {
            updateValidation(editNewUID, ok, false, "");
        }
    }

    protected synchronized void updateRetypedPasswordCorrectness() {
        CharSequence retypedPassword = editRetypeNewPassword.getText();
        // don't show mismatch message if retyped password field is empty

        if (TextUtils.isEmpty(retypedPassword)) {
            updateValidation(editRetypeNewPassword, false, false, "");
            return;
        }

        if (!editNewPassword
            .getText()
            .toString()
            .equals(retypedPassword.toString())) {
            updateValidation(
                    editRetypeNewPassword,
                    false,
                    true,
                    R.string.reg_psw_mismatch);
            return;
        }

        updateValidation(
                editRetypeNewPassword,
                true,
                true,
                R.string.ok_short);
    }

    protected void setValidationColor(GuidedEditText editText, boolean ok) {
        Resources res = this.getResources();
        editText.setGuideTextColor(ok ? res
            .getColorStateList(R.color.jnrain_green_dark) : res
            .getColorStateList(R.color.error_red));
    }

    protected void updateValidation(
            GuidedEditText editText,
            boolean ok,
            boolean visible,
            CharSequence text) {
        setValidationColor(editText, ok);
        editText.setGuideVisible(visible);
        editText.setGuideText(text);

        validatedMap.put(editText.getId(), ok);
        doValidation();
    }

    protected void updateValidation(
            GuidedEditText editText,
            boolean ok,
            boolean visible,
            int resId) {
        setValidationColor(editText, ok);
        editText.setGuideVisible(visible);
        editText.setGuideText(resId);

        validatedMap.put(editText.getId(), ok);
        doValidation();
    }

    protected void doValidation() {
        boolean passed = true;

        // fixed fields
        for (int itemId : FIXED_VALIDATION_FIELD_IDS) {
            if (!validatedMap.get(itemId)) {
                passed = false;
                break;
            }
        }

        // dynamic fields
        if (!isUseCurrentPhone() && !validatedMap.get(R.id.editPhone)) {
            passed = false;
        }

        setSubmitButtonEnabled(passed);
    }

    private abstract class StrippedDownTextWatcher implements TextWatcher {
        @Override
        public void afterTextChanged(Editable s) {
            // stripped down to nothing
        }

        @Override
        public void beforeTextChanged(
                CharSequence s,
                int start,
                int count,
                int after) {
            // ditto
        }
    }
}
