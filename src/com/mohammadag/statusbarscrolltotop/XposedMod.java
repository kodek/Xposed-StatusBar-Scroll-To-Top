package com.mohammadag.statusbarscrolltotop;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ScrollView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage, IXposedHookZygoteInit {
	/* My name's here so we don't conflict with other fields, deal with it :p */
	private static final String KEY_RECEIVERS = "kodekapps_statustap";

	/* You can trigger this with any app that can fire intents! */
	private static final String INTENT_TAP_RECEIVED = "com.kodekapps.statustap.TAP_RECEIVED";

	/* We get a MotionEvent when the status bar is tapped, we need to know if it's a click or a drag */
	private float mDownX;
	private float mDownY;
	private final float SCROLL_THRESHOLD = 10;
	private boolean mIsClick;


	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals("com.android.systemui"))
			return;

		Class<?> StatusBarWindowView = findClass("com.android.systemui.statusbar.phone.StatusBarWindowView",
				lpparam.classLoader);

		findAndHookMethod(StatusBarWindowView, "onInterceptTouchEvent", MotionEvent.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				MotionEvent ev = (MotionEvent) param.args[0];
				View view = (View) param.thisObject;
				switch (ev.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
					mDownX = ev.getX();
					mDownY = ev.getY();
					mIsClick = true;
					break;
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					if (mIsClick) {
						try {
							/* Get NotificationPanelView instance, it subclasses PanelView */
							Object notificationPanelView =
									XposedHelpers.getObjectField(param.thisObject, "mNotificationPanel");

							float expandedFraction = (Float) XposedHelpers.callMethod(notificationPanelView,
									"getExpandedFraction");

							if (expandedFraction < 0.1)
								view.getContext().sendBroadcast(new Intent(INTENT_TAP_RECEIVED));
						} catch (Throwable t) {
							XposedBridge.log("StatusBarScrollToTop: Unable to determine expanded fraction: " + t.getMessage());
							t.printStackTrace();
							view.getContext().sendBroadcast(new Intent(INTENT_TAP_RECEIVED));
						}
					}
					break;
				case MotionEvent.ACTION_MOVE:
					if (mIsClick && (Math.abs(mDownX - ev.getX()) > SCROLL_THRESHOLD || Math.abs(mDownY - ev.getY()) > SCROLL_THRESHOLD)) {
						mIsClick = false;
					}
					break;
				default:
					break;
				}
			}
		});
	}

	/* Helpers so the code looks less like shit */

	/* And that's a wrap */
}
