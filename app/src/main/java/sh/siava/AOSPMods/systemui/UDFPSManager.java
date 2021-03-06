package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.widget.ImageView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class UDFPSManager extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
    private static boolean transparentBG = false;

    public UDFPSManager(Context context) { super(context); }
    
    @Override
    public void updatePrefs(String...Key)
    {
        if(Xprefs == null) return;
        transparentBG = Xprefs.getBoolean("fingerprint_circle_hide", false);
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;


        Class<?> UtilClass = findClass("com.android.settingslib.Utils", lpparam.classLoader);


        findAndHookMethod("com.android.systemui.biometrics.UdfpsKeyguardView", lpparam.classLoader,
                "updateAlpha", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!transparentBG) return;

                        ImageView mBgProtection = (ImageView) getObjectField(param.thisObject, "mBgProtection");
                        mBgProtection.setImageAlpha(0);
//                        ImageView mLockScreenFp = (ImageView) getObjectField(param.thisObject, "mLockScreenFp");
                    }
                });

        findAndHookMethod("com.android.keyguard.LockIconView", lpparam.classLoader,
                "setUseBackground", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(!transparentBG) return;
                        param.args[0] = false;
                    }
                });
        findAndHookMethod("com.android.systemui.biometrics.UdfpsKeyguardView", lpparam.classLoader,
                "updateColor", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(!transparentBG) return;

                        Object mLockScreenFp = getObjectField(param.thisObject, "mLockScreenFp");


                        int mTextColorPrimary = (int) callStaticMethod(UtilClass, "getColorAttrDefaultColor", mContext,
                                mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()));

                        setObjectField(param.thisObject, "mTextColorPrimary", mTextColorPrimary);

                        callMethod(mLockScreenFp, "invalidate");
                        param.setResult(null);
                    }
                });
    }
}
