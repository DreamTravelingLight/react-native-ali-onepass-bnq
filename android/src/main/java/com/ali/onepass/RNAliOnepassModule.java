
package com.ali.onepass;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.mobile.auth.gatewayauth.AuthRegisterXmlConfig;
import com.mobile.auth.gatewayauth.AuthUIConfig;
import com.mobile.auth.gatewayauth.AuthUIControlClickListener;
import com.mobile.auth.gatewayauth.PhoneNumberAuthHelper;
import com.mobile.auth.gatewayauth.PreLoginResultListener;
import com.mobile.auth.gatewayauth.ResultCode;
import com.mobile.auth.gatewayauth.TokenResultListener;
import com.mobile.auth.gatewayauth.model.TokenRet;
import com.ali.onepass.config.BaseUIConfig;
import com.mobile.auth.gatewayauth.ui.AbstractPnsViewDelegate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;


public class RNAliOnepassModule extends ReactContextBaseJavaModule implements TokenResultListener {
//    public PhoneNumberAuthHelper mAuthHelper;

    private final ReactApplicationContext reactContext;
    private PhoneNumberAuthHelper phoneNumberAuthHelper;
    private int prefetchNumberTimeout = 3000;
    private int fetchNumberTimeout = 3000;
    private int mScreenWidthDp;
    private int mScreenHeightDp;
    private BaseUIConfig mUIConfig;
    private boolean checkBox = false;
    private int showType = 0;

    private final String TAG = "??????????????????";
    private Promise mPromise;
//    private final WindowManager mWindowManager;

    public RNAliOnepassModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public String getName() {
        return "RNAliOnepass";
    }

    /**
     * ?????? sdk ????????????
     *
     * @param secretInfo ?????????????????????,??????????????????????????????,????????????????????????,???????????????????????????,???????????????????????? ??????
     * @param promise
     */
    @ReactMethod
    public void init(final String secretInfo, final Promise promise) {
        Log.i("secretInfo>>",secretInfo);
        phoneNumberAuthHelper = PhoneNumberAuthHelper.getInstance(reactContext, this);
        phoneNumberAuthHelper.setAuthSDKInfo(secretInfo);
        mPromise = promise;

        phoneNumberAuthHelper.setUIClickListener(new AuthUIControlClickListener() {
            @Override
            public void onClick(String code, Context context, String jsonString) {
                JSONObject jsonObj = null;
                try {
                    if(!TextUtils.isEmpty(jsonString)) {
                        jsonObj = new JSONObject(jsonString);
                    }
                } catch (JSONException e) {
                    jsonObj = new JSONObject();
                }
                Log.e("????????????", jsonString);
//                Toast.makeText(context, R.string.custom_toast, Toast.LENGTH_SHORT).show();
                switch (code) {
                    //??????????????????????????????????????????
                    case ResultCode.CODE_ERROR_USER_CANCEL:
                        Log.e(TAG, "????????????????????????????????????");
                        phoneNumberAuthHelper.quitLoginPage();
//                        mActivity.finish();
                        break;
                    //?????????????????????????????????????????????????????? ??????????????????
                    //????????????????????????????????????setSwitchAccHidden(true)???????????????  ???????????????view???????????????
                    case ResultCode.CODE_ERROR_USER_SWITCH:
                        Log.e(TAG, "????????????????????????????????????????????????");
                        Activity activity = reactContext.getCurrentActivity();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                WritableMap writableMap = Arguments.createMap();
                                writableMap.putString("key", "pageClickEvents");
                                writableMap.putString("code","700001");
                                writableMap.putString("showType", String.valueOf(showType));
                                sendEvent("pageClickEvents", writableMap);
                            }
                        });
                        break;
                    //??????????????????????????????????????????
                    //??????????????????????????? ????????????????????????toast ?????????????????????????????????????????? setLogBtnToastHidden(true)????????????Toast
                    //???????????????????????????toast
                    case ResultCode.CODE_ERROR_USER_LOGIN_BTN:
                        checkBox = jsonObj.optBoolean("isChecked");
                        Log.e(TAG, "checkbox????????????" + jsonObj.optBoolean("isChecked"));
                        if (!jsonObj.optBoolean("isChecked")) {
                            Log.e(TAG, "checkbox????????????" + jsonObj.optBoolean("isChecked"));
//                            Toast.makeText(mContext, R.string.custom_toast, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    //checkbox???????????????????????????
                    case ResultCode.CODE_ERROR_USER_CHECKBOX:
                        checkBox = jsonObj.optBoolean("isChecked");
                        Log.e(TAG, "checkbox????????????" + jsonObj.optBoolean("isChecked"));
                        break;
                    //??????????????????????????????
                    case ResultCode.CODE_ERROR_USER_PROTOCOL_CONTROL:
                        Log.e(TAG, "???????????????" + "name: " + jsonObj.optString("name") + ", url: " + jsonObj.optString("url"));
                        break;
                    default:
                        break;

                }

            }
        });


        promise.resolve("");
    }

    private boolean checkInit(final Promise promise) {
        if (phoneNumberAuthHelper != null) {
            return true;
        }
        promise.reject("0", "???????????????????????????init");
        return false;
    }

    /**
     * SDK ??????????????????,????????????????????????????????????
     */
    @ReactMethod
    public void checkEnvAvailable(final Promise promise) {
        if (!checkInit(promise)) {
            promise.resolve(false);
            return;
        }
        boolean available = phoneNumberAuthHelper.checkEnvAvailable();
        promise.resolve(available);
    }

    @Override
    public void onTokenSuccess(String s) {
        WritableMap writableMap = Arguments.createMap();
        TokenRet tokenRet = null;
        try {
            tokenRet = JSON.parseObject(s, TokenRet.class);
            writableMap.putString("vendorName", tokenRet.getVendorName());
            writableMap.putString("code", tokenRet.getCode());
            writableMap.putString("msg", tokenRet.getMsg());
            writableMap.putInt("requestCode", tokenRet.getRequestCode());
            writableMap.putString("token", tokenRet.getToken());
            writableMap.putInt("showType",showType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e("onTokenSuccess","onTokenSuccess  ??????");
        sendEvent("onTokenSuccess", writableMap);
    }

    @Override
    public void onTokenFailed(String s) {
        WritableMap writableMap = Arguments.createMap();
        TokenRet tokenRet = null;
        try {
            tokenRet = JSON.parseObject(s, TokenRet.class);
            writableMap.putString("vendorName", tokenRet.getVendorName());
            writableMap.putString("code", tokenRet.getCode());
            writableMap.putString("msg", tokenRet.getMsg());
            writableMap.putInt("requestCode", tokenRet.getRequestCode());
            writableMap.putInt("showType",showType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (tokenRet.getCode().equals("700001")){
//            ????????????????????????
            Log.e(TAG,"????????????????????????");
//            writableMap.putString("loginType", "weixin_icon_black");
            sendEvent("pageClickEvents", writableMap);
        }else {
            sendEvent("onTokenFailed", writableMap);
        }
        if (tokenRet.getCode().equals("600008")){
            hideLoginLoading(mPromise);
            Toast.makeText(reactContext, tokenRet.getMsg(), Toast.LENGTH_SHORT).show();
            return;
        }
        phoneNumberAuthHelper.quitLoginPage();
    }

    /**
     * ?????????
     *
     * @param promise
     */
    @ReactMethod
    public void prefetch(final Promise promise) {
        if (!checkInit(promise)) {
            return;
        }
        phoneNumberAuthHelper.accelerateLoginPage(prefetchNumberTimeout, new PreLoginResultListener() {
            @Override
            public void onTokenSuccess(String s) {
                promise.resolve(s);
            }

            @Override
            public void onTokenFailed(String s, String s1) {
                promise.reject(s, s1);
            }
        });
    }

    /**
     * ????????????
     *
     * @param promise
     */
    @ReactMethod
    public void onePass(final ReadableMap config,final int type, final Promise promise) {
        if (!checkInit(promise)) {
            return;
        }
        phoneNumberAuthHelper.getLoginToken(reactContext, fetchNumberTimeout);
        showType = type;
        if (type == 0){
            this.setUIConfig(config,promise);
        }else {
            this.setDialogUIConfig(config,promise);
        }
//

        promise.resolve("");
    }

    /**
     * ????????????????????? , ??????????????????????????? APP  ??????, ???????????????????????????????????????    !!!!
     * SDK  ???????????????,??????????????????????????????,???????????????????????????????????????????????????????????????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void quitLoginPage(final Promise promise) {
        phoneNumberAuthHelper.quitLoginPage();
        promise.resolve("");
    }

    /**
     * ????????????????????????,???????????? loading ????????? APP ??????
     *
     * @param promise
     */
    @ReactMethod
    public void hideLoginLoading(final Promise promise) {
        phoneNumberAuthHelper.hideLoginLoading();
        promise.resolve("");
    }


    /**
     * ?????????????????????
     *
     * @param promise
     * @return
     */
    @ReactMethod
    public String getOperatorType(final Promise promise) {
        if (!checkInit(promise)) {
            return null;
        }
        String carrierName = phoneNumberAuthHelper.getCurrentCarrierName();
        if (carrierName.equals("CMCC")) {
            carrierName = "????????????";
        } else if (carrierName.equals("CUCC")) {
            carrierName = "????????????";
        } else if (carrierName.equals("CTCC")) {
            carrierName = "????????????";
        }
        promise.resolve(carrierName);
        return carrierName;
    }

//    private String getRunningActivityName(){
//        ActivityManager activityManager=(ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        String runningActivity=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
//        return runningActivity;
//    }

    /**
     * ????????????UI
     *
     * @param config
     */
    @ReactMethod
    public void setUIConfig(final ReadableMap config, final Promise promise) {
        if (!checkInit(promise)) {
            return;
        }

        int authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        if (Build.VERSION.SDK_INT == 26) {
            authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND;
        }

        phoneNumberAuthHelper.removeAuthRegisterXmlConfig();
        phoneNumberAuthHelper.removeAuthRegisterViewConfig();
        //sdk????????????????????????marginTop50dp
        int designHeight = mScreenHeightDp - 50;
        int unit = designHeight / 10;
        int logBtnHeight = (int) (unit * 1.2);
        final int logBtnOffsetY = unit * 3;

        phoneNumberAuthHelper.addAuthRegisterXmlConfig(new AuthRegisterXmlConfig.Builder()
                .setLayout(R.layout.custom_land_dialog, new AbstractPnsViewDelegate() {
                    @Override
                    public void onViewCreated(View view) {
//                        String showThirtyLogin = config.getString(methodName2KeyName("setCustomState"));
//                        if (showThirtyLogin.equals("2")){
//                            findViewById(R.id.thirty_login_view).setVisibility(View.GONE);
//                        }
                        findViewById(R.id.container_icon).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Log.i("????????????????????????", "111");
                                if (!checkBox){
//                                    ???????????????
                                    Toast.makeText(reactContext, R.string.custom_toast, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                WritableMap writableMap = Arguments.createMap();
                                writableMap.putString("key", "otherLogin");
                                writableMap.putString("loginType","weixin_icon_black");
                                sendEvent("otherLoginTypeSelect", writableMap);
//                                phoneNumberAuthHelper.quitLoginPage();
                            }
                        });

                        findViewById(R.id.full_alert_cancel_action).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                phoneNumberAuthHelper.quitLoginPage();
                            }
                        });

                        int iconTopMargin = AppUtils.dp2px(getContext(), logBtnOffsetY + 50);
                        View iconContainer = findViewById(R.id.container_icon);
                        RelativeLayout.LayoutParams iconLayout = (RelativeLayout.LayoutParams) iconContainer.getLayoutParams();
//                        iconLayout.topMargin = 800;
//                        iconLayout.bottomMargin=100;
                        iconLayout.width = AppUtils.dp2px(getContext(), mScreenWidthDp / 2 - 60);
                    }
                })
                .build());

        int screenHeight = this.getCurrentActivity().getWindowManager().getDefaultDisplay().getHeight();
        Log.i("??????????????????", String.valueOf(screenHeight));

        int height = screenHeight/3;
        WindowManager wm = (WindowManager) this.reactContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        if (wm != null) {
            wm.getDefaultDisplay().getMetrics(outMetrics);
            height = (int) (outMetrics.heightPixels/outMetrics.scaledDensity);
        }


        AuthUIConfig.Builder builder = new AuthUIConfig.Builder();
        setSloganUI(builder, config);
//        setNavBarUI(builder, config);
        setLogBtnUI(builder, config);
        setSwitchAccUI(builder, config);
        setStatusBarUI(builder, config);
        setLogoUI(builder, config);
        setNumberUI(builder, config);
        setPrivacyUI(builder, config);
        setOtherUI(builder, config);
        builder.setDialogHeight(height);
        builder.setDialogBottom(true);
        builder.setDialogOffsetY(5);

        builder.setWebNavColor(Color.parseColor("#ffffff"));
        builder.setWebNavTextColor(Color.parseColor("#333333"));
        phoneNumberAuthHelper.setAuthUIConfig(builder.create());
        promise.resolve("");
    }

    // dialog??????
    @ReactMethod
    public void setDialogUIConfig(final ReadableMap config, final Promise promise) {
        phoneNumberAuthHelper.removeAuthRegisterXmlConfig();
        phoneNumberAuthHelper.removeAuthRegisterViewConfig();
        Activity activity = this.reactContext.getCurrentActivity();
        Log.i("1111>>>>>", String.valueOf(activity));
        mUIConfig = BaseUIConfig.init(4, activity, phoneNumberAuthHelper,this.getOperatorType(promise),config);
        Log.i("2222222>>>>>", String.valueOf(mUIConfig));
        assert mUIConfig != null;
        mUIConfig.configAuthPage();
        promise.resolve("");
    }


    // ??????????????????
    private void configLoginTokenPortDialog(ReadableMap config) {
        // initDynamicView();
        phoneNumberAuthHelper.removeAuthRegisterXmlConfig();
        phoneNumberAuthHelper.removeAuthRegisterViewConfig();
        int authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        if (Build.VERSION.SDK_INT == 26) {
            authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND;
        }
        updateScreenSize(authPageOrientation);
        int dialogWidth = (int) (mScreenWidthDp * 0.8f);
        int dialogHeight = (int) (mScreenHeightDp * 0.65f);

        int logBtnOffset = dialogHeight / 2;
        phoneNumberAuthHelper.setAuthUIConfig(
                new AuthUIConfig.Builder()
                        // .setAppPrivacyOne("???????????????????????????", "https://www.baidu.com")
                        .setAppPrivacyColor(Color.GRAY, Color.parseColor("#FFA346"))
                        .setPrivacyState(false)
                        .setCheckboxHidden(true)
//            .setNavHidden(false)
//            .setNavColor(Color.parseColor("#FFA346"))
//            .setNavReturnImgPath("icon_close")
                        .setWebNavColor(Color.parseColor("#ffffff"))
                        .setWebNavTextColor(Color.parseColor("#191919"))
                        .setAuthPageActIn("in_activity", "out_activity")
                        .setAuthPageActOut("in_activity", "out_activity")
                        .setVendorPrivacyPrefix("???")
                        .setVendorPrivacySuffix("???")
                        .setLogoImgPath("ic_launcher")
                        .setLogBtnWidth(dialogWidth - 30)
                        .setLogBtnMarginLeftAndRight(15)
                        .setLogBtnBackgroundPath("button")
                        .setLogoOffsetY(48)
                        .setLogoWidth(42)
                        .setLogoHeight(42)
                        .setLogBtnOffsetY(logBtnOffset)
                        .setSloganText("????????????????????????????????????????????????")
                        .setSloganOffsetY(logBtnOffset - 100)
                        .setSloganTextSize(11)
                        .setNumFieldOffsetY(logBtnOffset - 50)
                        .setSwitchOffsetY(logBtnOffset + 50)
                        .setSwitchAccTextSize(11)
//            .setPageBackgroundPath("dialog_page_background")
                        .setNumberSize(17)
                        .setLogBtnHeight(38)
                        .setLogBtnTextSize(16)
                        .setDialogWidth(dialogWidth)
                        .setDialogHeight(dialogHeight)
                        .setDialogBottom(false)
//            .setDialogAlpha(82)
//                        .setScreenOrientation(authPageOrientation)
                        .create()
        );
    }

    /**
     * ??????????????????key???
     * @param methodName
     * @return
     */
    private String methodName2KeyName(String methodName) {
        String result = "";
        if (methodName == null) {
            return result;
        }
        if (methodName.startsWith("set")) {
            result = methodName.substring(3);
        }
        String firstChar = result.substring(0, 1); // ?????????
        String otherChar = result.substring(1); // ?????????
        Log.d(methodName, firstChar.toLowerCase() + otherChar);
        return firstChar.toLowerCase() + otherChar;
    }

    private void setDialogUIHeight(AuthUIConfig.Builder builder, ReadableMap config, int defaultHeight) {
        if (config.hasKey(methodName2KeyName("setDialogHeightDelta"))) {
            builder.setDialogHeight(defaultHeight - config.getInt(methodName2KeyName("setDialogHeightDelta")));
        }
    }

    /**
     * ?????????UI??????
     */
    private void setNavBarUI(AuthUIConfig.Builder builder, ReadableMap config) {
        if (config.hasKey(methodName2KeyName("setNavColor"))) {
            builder.setNavColor(Color.parseColor(config.getString(methodName2KeyName("setNavColor"))));
        }
        if (config.hasKey(methodName2KeyName("setNavText"))) {
            builder.setNavText(config.getString(methodName2KeyName("setNavText")));
        }
        if (config.hasKey(methodName2KeyName("setNavTextColor"))) {
            builder.setNavTextColor(Color.parseColor(config.getString(methodName2KeyName("setNavTextColor"))));
        }
        if (config.hasKey(methodName2KeyName("setNavTextSize"))) {
            builder.setNavTextSize(config.getInt(methodName2KeyName("setNavTextSize")));
        }
        if (config.hasKey(methodName2KeyName("setNavReturnImgPath"))) {
            builder.setNavReturnImgPath(config.getString(methodName2KeyName("setNavReturnImgPath")));
            builder.setNavReturnScaleType(ImageView.ScaleType.FIT_CENTER);
        }
        if (config.hasKey(methodName2KeyName("setNavReturnImgWidth"))) {
            builder.setNavReturnImgWidth(config.getInt(methodName2KeyName("setNavReturnImgWidth")));
        }
        if (config.hasKey(methodName2KeyName("setNavReturnImgHeight"))) {
            builder.setNavReturnImgHeight(config.getInt(methodName2KeyName("setNavReturnImgHeight")));
        }
        // webView
        if (config.hasKey(methodName2KeyName("setWebNavColor"))) {
            builder.setWebNavColor(Color.parseColor(config.getString(methodName2KeyName("setWebNavColor"))));
        }
        if (config.hasKey(methodName2KeyName("setWebNavTextColor"))) {
            builder.setWebNavTextColor(Color.parseColor(config.getString(methodName2KeyName("setWebNavTextColor"))));
        }
        if (config.hasKey(methodName2KeyName("setWebNavTextSize"))) {
            builder.setWebNavTextSize(config.getInt(methodName2KeyName("setWebNavTextSize")));
        }
    }

    /**
     * ???????????????UI??????
     */
    private void setSloganUI(AuthUIConfig.Builder builder, ReadableMap config) {
        if (config.hasKey(methodName2KeyName("setSloganText"))) {
            builder.setSloganText(config.getString(methodName2KeyName("setSloganText")));
        }
        if (config.hasKey(methodName2KeyName("setSloganTextColor"))) {
            builder.setSloganTextColor(Color.parseColor(config.getString(methodName2KeyName("setSloganTextColor"))));
        }
        if (config.hasKey(methodName2KeyName("setSloganTextSize"))) {
            builder.setSloganTextSize(config.getInt(methodName2KeyName("setSloganTextSize")));
        }
        if (config.hasKey(methodName2KeyName("setSloganOffsetY"))) {
            builder.setSloganOffsetY(config.getInt(methodName2KeyName("setSloganOffsetY")));
        }
        if (config.hasKey(methodName2KeyName("setSloganOffsetY_B"))) {
            builder.setSloganOffsetY_B(config.getInt(methodName2KeyName("setSloganOffsetY_B")));
        }
    }

    /**
     * ????????????UI??????
     */
    private void setLogBtnUI(AuthUIConfig.Builder builder, ReadableMap config) {
        if (config.hasKey(methodName2KeyName("setLogBtnText"))) {
            builder.setLogBtnText(config.getString(methodName2KeyName("setLogBtnText")));
        }
        if (config.hasKey(methodName2KeyName("setLogBtnTextColor"))) {
            builder.setLogBtnTextColor(Color.parseColor(config.getString(methodName2KeyName("setLogBtnTextColor"))));
        }
        if (config.hasKey(methodName2KeyName("setLogBtnTextSize"))) {
            builder.setLogBtnTextSize(config.getInt(methodName2KeyName("setLogBtnTextSize")));
        }
        if (config.hasKey(methodName2KeyName("setLogBtnWidth"))) {
            builder.setLogBtnWidth(config.getInt(methodName2KeyName("setLogBtnWidth")));
        }
        if (config.hasKey(methodName2KeyName("setLogBtnHeight"))) {
            builder.setLogBtnHeight(config.getInt(methodName2KeyName("setLogBtnHeight")));
        }
        if (config.hasKey(methodName2KeyName("setLogBtnMarginLeftAndRight"))) {
            builder.setLogBtnMarginLeftAndRight(config.getInt(methodName2KeyName("setLogBtnMarginLeftAndRight")));
        }
        if (config.hasKey(methodName2KeyName("setLogBtnBackgroundPath"))) {
            builder.setLogBtnBackgroundPath(config.getString(methodName2KeyName("setLogBtnBackgroundPath")));
        }
        if (config.hasKey(methodName2KeyName("setLogBtnOffsetY"))) {
            builder.setLogBtnOffsetY(config.getInt(methodName2KeyName("setLogBtnOffsetY")));
        }
        if (config.hasKey(methodName2KeyName("setLogBtnOffsetY_B"))) {
            builder.setLogBtnOffsetY_B(config.getInt(methodName2KeyName("setLogBtnOffsetY_B")));
        }
        if (config.hasKey(methodName2KeyName("setLoadingImgPath"))) {
            builder.setLoadingImgPath(config.getString(methodName2KeyName("setLoadingImgPath")));
        }
    }

    /**
     * ????????????????????????UI??????
     */
    private void setSwitchAccUI(AuthUIConfig.Builder builder, ReadableMap config) {
        if (config.hasKey(methodName2KeyName("setSwitchAccHidden"))) {
            builder.setSwitchAccHidden(config.getBoolean(methodName2KeyName("setSwitchAccHidden")));
        }
        if (config.hasKey(methodName2KeyName("setSwitchAccText"))) {
            builder.setSwitchAccText(config.getString(methodName2KeyName("setSwitchAccText")));
        }
        if (config.hasKey(methodName2KeyName("setSwitchAccTextSize"))) {
            builder.setSwitchAccTextSize(config.getInt(methodName2KeyName("setSwitchAccTextSize")));
        }
        if (config.hasKey(methodName2KeyName("setSwitchOffsetY"))) {
            builder.setSwitchOffsetY(config.getInt(methodName2KeyName("setSwitchOffsetY")));
        }
        if (config.hasKey(methodName2KeyName("setSwitchOffsetY_B"))) {
            builder.setSwitchOffsetY_B(config.getInt(methodName2KeyName("setSwitchOffsetY_B")));
        }
        if (config.hasKey(methodName2KeyName("setSwitchAccTextColor"))) {
            builder.setSwitchAccTextColor(Color.parseColor(config.getString(methodName2KeyName("setSwitchAccTextColor"))));
        }
    }

    /**
     * ?????????
     */
    private void setStatusBarUI(AuthUIConfig.Builder builder, ReadableMap config) {
        if (config.hasKey(methodName2KeyName("setStatusBarColor"))) {
            builder.setStatusBarColor(Color.parseColor(config.getString(methodName2KeyName("setStatusBarColor"))));
        }
        if (config.hasKey(methodName2KeyName("setLightColor"))) {
            builder.setLightColor(config.getBoolean(methodName2KeyName("setLightColor")));
        }
        if (config.hasKey(methodName2KeyName("setStatusBarHidden"))) {
            builder.setStatusBarHidden(config.getBoolean(methodName2KeyName("setStatusBarHidden")));
        }
    }

    /**
     * logo
     */
    private void setLogoUI(AuthUIConfig.Builder builder, ReadableMap config) {
        if (config.hasKey(methodName2KeyName("setLogoImgPath"))) {
            builder.setLogoImgPath(config.getString(methodName2KeyName("setLogoImgPath")));
        }
        if (config.hasKey(methodName2KeyName("setLogoHidden"))) {
            builder.setLogoHidden(config.getBoolean(methodName2KeyName("setLogoHidden")));
        }
        if (config.hasKey(methodName2KeyName("setLogoWidth"))) {
            builder.setLogoWidth(config.getInt(methodName2KeyName("setLogoWidth")));
        }
        if (config.hasKey(methodName2KeyName("setLogoHeight"))) {
            builder.setLogoHeight(config.getInt(methodName2KeyName("setLogoHeight")));
        }
        if (config.hasKey(methodName2KeyName("setLogoOffsetY"))) {
            builder.setLogoOffsetY(config.getInt(methodName2KeyName("setLogoOffsetY")));
        }
        if (config.hasKey(methodName2KeyName("setLogoOffsetY_B"))) {
            builder.setLogoOffsetY_B(config.getInt(methodName2KeyName("setLogoOffsetY_B")));
        }
    }

    /**
     * ??????UI
     */
    private void setNumberUI(AuthUIConfig.Builder builder, ReadableMap config) {
        if (config.hasKey(methodName2KeyName("setNumberColor"))) {
            builder.setNumberColor(Color.parseColor(config.getString(methodName2KeyName("setNumberColor"))));
        }
        if (config.hasKey(methodName2KeyName("setNumberSize"))) {
            builder.setNumberSize(config.getInt(methodName2KeyName("setNumberSize")));
        }
        if (config.hasKey(methodName2KeyName("setNumberFieldOffsetX"))) {
            builder.setNumberFieldOffsetX(config.getInt(methodName2KeyName("setNumberFieldOffsetX")));
        }
        if (config.hasKey(methodName2KeyName("setNumberFieldOffsetY"))) {
            builder.setNumFieldOffsetY(config.getInt(methodName2KeyName("setNumberFieldOffsetY")));
        }
        if (config.hasKey(methodName2KeyName("setNumberFieldOffsetY_B"))) {
            builder.setNumFieldOffsetY_B(config.getInt(methodName2KeyName("setNumberFieldOffsetY_B")));
        }
    }

    /**
     * ??????
     */
    private void setPrivacyUI(AuthUIConfig.Builder builder, ReadableMap config) {
        if (config.hasKey(methodName2KeyName("setAppPrivacyOneName")) && config.hasKey(methodName2KeyName("setAppPrivacyOneUrl"))) {
            builder.setAppPrivacyOne(config.getString(methodName2KeyName("setAppPrivacyOneName")), config.getString(methodName2KeyName("setAppPrivacyOneUrl")));
        }
        if (config.hasKey(methodName2KeyName("setAppPrivacyTwoName")) && config.hasKey(methodName2KeyName("setAppPrivacyTwoUrl"))) {
            builder.setAppPrivacyTwo(config.getString(methodName2KeyName("setAppPrivacyTwoName")), config.getString(methodName2KeyName("setAppPrivacyTwoUrl")));
        }
        if (config.hasKey(methodName2KeyName("setPrivacyState"))) {
            builder.setPrivacyState(config.getBoolean(methodName2KeyName("setPrivacyState")));
        }
        if (config.hasKey(methodName2KeyName("setPrivacyTextSize"))) {
            builder.setPrivacyTextSize(config.getInt(methodName2KeyName("setPrivacyTextSize")));
        }
        if (config.hasKey(methodName2KeyName("setAppPrivacyBaseColor")) && config.hasKey(methodName2KeyName("setAppPrivacyColor"))) {
            builder.setAppPrivacyColor(Color.parseColor(config.getString(methodName2KeyName("setAppPrivacyBaseColor"))), Color.parseColor(config.getString(methodName2KeyName("setAppPrivacyColor"))));
        }
        if (config.hasKey(methodName2KeyName("setVendorPrivacyPrefix"))) {
            builder.setVendorPrivacyPrefix(config.getString(methodName2KeyName("setVendorPrivacyPrefix")));
        }
        if (config.hasKey(methodName2KeyName("setVendorPrivacySuffix"))) {
            builder.setVendorPrivacySuffix(config.getString(methodName2KeyName("setVendorPrivacySuffix")));
        }
        if (config.hasKey(methodName2KeyName("setPrivacyBefore"))) {
            builder.setPrivacyBefore(config.getString(methodName2KeyName("setPrivacyBefore")));
        }
        if (config.hasKey(methodName2KeyName("setPrivacyEnd"))) {
            builder.setPrivacyEnd(config.getString(methodName2KeyName("setPrivacyEnd")));
        }
        if (config.hasKey(methodName2KeyName("setCheckboxHidden"))) {
            builder.setCheckboxHidden(config.getBoolean(methodName2KeyName("setCheckboxHidden")));
        }
        if (config.hasKey(methodName2KeyName("setPrivacyOffsetY"))) {
            builder.setPrivacyOffsetY(config.getInt(methodName2KeyName("setPrivacyOffsetY")));
        }
        if (config.hasKey(methodName2KeyName("setPrivacyOffsetY_B"))) {
            builder.setPrivacyOffsetY_B(config.getInt(methodName2KeyName("setPrivacyOffsetY_B")));
        }
    }

    /**
     * ??????
     */
    private void setOtherUI(AuthUIConfig.Builder builder, ReadableMap config) {
//        int authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
//        if (Build.VERSION.SDK_INT == 26) {
//            authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND;
//        }
//        builder.setScreenOrientation(authPageOrientation);
    }

    private void sendEvent(String eventName, WritableMap params) {
        try {
            this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        } catch (RuntimeException e) {
            Log.e("ERROR", "java.lang.RuntimeException: Trying to invoke Javascript before CatalystInstance has been set!");
        }
    }

    private void updateScreenSize(int authPageScreenOrientation) {
        int screenHeightDp = AppUtils.px2dp(reactContext, AppUtils.getPhoneHeightPixels(reactContext));
        int screenWidthDp = AppUtils.px2dp(reactContext, AppUtils.getPhoneWidthPixels(reactContext));
        mScreenWidthDp = screenWidthDp;
        mScreenHeightDp = screenHeightDp;
    }
}
