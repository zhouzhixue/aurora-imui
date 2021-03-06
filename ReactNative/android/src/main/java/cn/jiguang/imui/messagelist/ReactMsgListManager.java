package cn.jiguang.imui.messagelist;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Nullable;

import cn.jiguang.imui.commons.ImageLoader;
import cn.jiguang.imui.messages.MessageList;
import cn.jiguang.imui.messages.MsgListAdapter;
import cn.jiguang.imui.messages.ViewHolderController;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by caiyaoguan on 2017/5/22.
 */

public class ReactMsgListManager extends ViewGroupManager<MessageList> implements SensorEventListener {

    private static final String REACT_MESSAGE_LIST = "RCTMessageList";
    public static final String SEND_MESSAGE = "send_message";
    private static final String RECEIVE_MESSAGE = "receive_message";
    private static final String LOAD_HISTORY = "load_history_message";
    private static final String UPDATE_MESSAGE = "update_message";

    private static final String ON_AVATAR_CLICK_EVENT = "onAvatarClick";
    private static final String ON_MSG_CLICK_EVENT = "onMsgClick";
    private static final String ON_MSG_LONG_CLICK_EVENT = "onMsgLongClick";
    private static final String ON_STATUS_VIEW_CLICK_EVENT = "onStatusViewClick";
    private static final String ON_TOUCH_MSG_LIST_EVENT = "onTouchMsgList";
    private static final String ON_PULL_TO_REFRESH_EVENT = "onPullToRefresh";

    public static final String RCT_APPEND_MESSAGES_ACTION = "cn.jiguang.imui.messagelist.intent.appendMessages";
    public static final String RCT_UPDATE_MESSAGE_ACTION = "cn.jiguang.imui.messagelist.intent.updateMessage";
    public static final String RCT_INSERT_MESSAGES_ACTION = "cn.jiguang.imui.messagelist.intent.insertMessages";
    public static final String RCT_SCROLL_TO_BOTTOM_ACTION = "cn.jiguang.imui.messagelist.intent.scrollToBottom";

    private MsgListAdapter mAdapter;
    private ReactContext mContext;
    private MessageList mMessageList;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public String getName() {
        return REACT_MESSAGE_LIST;
    }

    @SuppressLint("ClickableViewAccessibility")
    @SuppressWarnings("unchecked")
    @Override
    protected MessageList createViewInstance(final ThemedReactContext reactContext) {
        EventBus.getDefault().register(this);
        registerProximitySensorListener();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        reactContext.registerReceiver(RCTMsgListReceiver, intentFilter);
        mContext = reactContext;
        mMessageList = new MessageList(reactContext, null);
        mMessageList.setHasFixedSize(true);
        // Use default layout
        MsgListAdapter.HoldersConfig holdersConfig = new MsgListAdapter.HoldersConfig();
        ImageLoader imageLoader = new ImageLoader() {
            @Override
            public void loadAvatarImage(ImageView avatarImageView, String string) {
                int resId = IdHelper.getDrawable(reactContext, string);
                if (resId != 0) {
                    Log.d("ReactMsgListManager", "Set drawable name: " + string);
                    avatarImageView.setImageResource(resId);
                } else {
                    Glide.with(reactContext)
                            .load(string)
                            .placeholder(IdHelper.getDrawable(reactContext, "aurora_headicon_default"))
                            .into(avatarImageView);
                }
            }

            @Override
            public void loadImage(ImageView imageView, String string) {
                // You can use other image load libraries.
                Glide.with(reactContext)
                        .load(string)
                        .fitCenter()
                        .placeholder(IdHelper.getDrawable(reactContext, "aurora_picture_not_found"))
                        .override(400, Target.SIZE_ORIGINAL)
                        .into(imageView);
            }
        };
        mAdapter = new MsgListAdapter<>("0", holdersConfig, imageLoader);
        mMessageList.setAdapter(mAdapter);
        mAdapter.setOnMsgClickListener(new MsgListAdapter.OnMsgClickListener<RCTMessage>() {
            @Override
            public void onMessageClick(RCTMessage message) {
                WritableMap event = Arguments.createMap();
                event.putString("message", message.toString());
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(mMessageList.getId(), ON_MSG_CLICK_EVENT, event);
            }
        });

        mAdapter.setMsgLongClickListener(new MsgListAdapter.OnMsgLongClickListener<RCTMessage>() {
            @Override
            public void onMessageLongClick(RCTMessage message) {
                WritableMap event = Arguments.createMap();
                event.putString("message", message.toString());
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(mMessageList.getId(), ON_MSG_LONG_CLICK_EVENT, event);
            }
        });

        mAdapter.setOnAvatarClickListener(new MsgListAdapter.OnAvatarClickListener<RCTMessage>() {
            @Override
            public void onAvatarClick(RCTMessage message) {
                WritableMap event = Arguments.createMap();
                event.putString("message", message.toString());
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(mMessageList.getId(), ON_AVATAR_CLICK_EVENT, event);
            }
        });

        mAdapter.setMsgStatusViewClickListener(new MsgListAdapter.OnMsgStatusViewClickListener<RCTMessage>() {
            @Override
            public void onStatusViewClick(RCTMessage message) {
                WritableMap event = Arguments.createMap();
                event.putString("message", message.toString());
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(mMessageList.getId(), ON_STATUS_VIEW_CLICK_EVENT, event);
            }
        });

        mMessageList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(mMessageList.getId(), ON_TOUCH_MSG_LIST_EVENT, null);
                        if (reactContext.getCurrentActivity() != null) {
                            InputMethodManager imm = (InputMethodManager) reactContext.getCurrentActivity()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                            Window window = reactContext.getCurrentActivity().getWindow();
                            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        break;
                }
                return false;
            }
        });
        mAdapter.setOnLoadMoreListener(new MsgListAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore(int i, int i1) {
                Log.i(REACT_MESSAGE_LIST, "onPullToRefresh will call");
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(mMessageList.getId(),
                        ON_PULL_TO_REFRESH_EVENT, null);
            }
        });
        // 通知 AuroraIMUIModule 完成初始化 MessageList
        EventBus.getDefault().post(new LoadedEvent(AuroraIMUIModule.RCT_MESSAGE_LIST_LOADED_ACTION));
//        Intent intent = new Intent(AuroraIMUIModule.RCT_MESSAGE_LIST_LOADED_ACTION);
//        reactContext.sendBroadcast(intent);
        return mMessageList;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ScrollEvent event) {
        if (event.getFlag()) {
            Log.i(REACT_MESSAGE_LIST, "Scroll to bottom smoothly");
            mMessageList.smoothScrollToPosition(0);
        } else {
            Log.i(REACT_MESSAGE_LIST, "Scroll to bottom");
            mMessageList.getLayoutManager().scrollToPosition(0);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent event) {
        final Activity activity = mContext.getCurrentActivity();
        if (event.getAction().equals(RCT_APPEND_MESSAGES_ACTION)) {
            RCTMessage[] messages = event.getMessages();
            for (final RCTMessage rctMessage : messages) {
                Log.d("RCTMessageListManager", "Add message to start, message: " + rctMessage);
                if (activity != null) {
                    final DisplayMetrics dm = new DisplayMetrics();
                    final WindowManager windowManager = activity.getWindowManager();
                    windowManager.getDefaultDisplay().getMetrics(dm);
                    mAdapter.addToStart(rctMessage, true);
                    mMessageList.smoothScrollToPosition(0);
                }
            }
        } else if (event.getAction().equals(RCT_UPDATE_MESSAGE_ACTION)) {
            RCTMessage rctMessage = event.getMessage();
            Log.d("RCTMessageListManager", "updating message, message: " + rctMessage);
            if (activity != null) {
                mAdapter.updateMessage(rctMessage.getMsgId(), rctMessage);
            }
        } else if (event.getAction().equals(RCT_INSERT_MESSAGES_ACTION)) {
            RCTMessage[] messages = event.getMessages();
            Log.d("RCTMessageListManager", "Add send message to top");
            mAdapter.addToEnd(Arrays.asList(messages));
        }
    }

    @ReactProp(name = "sendBubble")
    public void setSendBubble(MessageList messageList, ReadableMap map) {
        int resId = mContext.getResources().getIdentifier(map.getString("imageName"),
                "drawable", mContext.getPackageName());
        if (resId != 0) {
            messageList.setSendBubbleDrawable(resId);
        }
    }

    @ReactProp(name = "receiveBubble")
    public void setReceiveBubble(MessageList messageList, ReadableMap map) {
        int resId = mContext.getResources().getIdentifier(map.getString("imageName"),
                "drawable", mContext.getPackageName());
        if (resId != 0) {
            messageList.setReceiveBubbleDrawable(resId);
        }
    }

    @ReactProp(name = "sendBubbleTextColor")
    public void setSendBubbleTextColor(MessageList messageList, String color) {
        int colorRes = Color.parseColor(color);
        messageList.setSendBubbleTextColor(colorRes);
    }

    @ReactProp(name = "receiveBubbleTextColor")
    public void setReceiveBubbleTextColor(MessageList messageList, String color) {
        int colorRes = Color.parseColor(color);
        messageList.setReceiveBubbleTextColor(colorRes);
    }

    @ReactProp(name = "sendBubbleTextSize")
    public void setSendBubbleTextSize(MessageList messageList, int size) {
        messageList.setSendBubbleTextSize(dip2sp(size));
    }

    @ReactProp(name = "receiveBubbleTextSize")
    public void setReceiveBubbleTextSize(MessageList messageList, int size) {
        messageList.setReceiveBubbleTextSize(dip2sp(size));
    }

    @ReactProp(name = "sendBubblePadding")
    public void setSendBubblePadding(MessageList messageList, ReadableMap map) {
        messageList.setSendBubblePaddingLeft(dip2px(map.getInt("left")));
        messageList.setSendBubblePaddingTop(dip2px(map.getInt("top")));
        messageList.setSendBubblePaddingRight(dip2px(map.getInt("right")));
        messageList.setSendBubblePaddingBottom(dip2px(map.getInt("bottom")));
    }

    private int dip2px(float dpValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private int dip2sp(int dip) {
        int px = dip2px(dip);
        float scale = mContext.getResources().getDisplayMetrics().scaledDensity;
        return (int) (px / scale);
    }

    @ReactProp(name = "receiveBubblePadding")
    public void setReceiveBubblePaddingLeft(MessageList messageList, ReadableMap map) {
        messageList.setReceiveBubblePaddingLeft(dip2px(map.getInt("left")));
        messageList.setReceiveBubblePaddingTop(dip2px(map.getInt("top")));
        messageList.setReceiveBubblePaddingRight(dip2px(map.getInt("right")));
        messageList.setReceiveBubblePaddingBottom(dip2px(map.getInt("bottom")));
    }

    @ReactProp(name = "dateTextSize")
    public void setDateTextSize(MessageList messageList, int size) {
        messageList.setDateTextSize(dip2sp(size));
    }

    @ReactProp(name = "dateTextColor")
    public void setDateTextColor(MessageList messageList, String color) {
        int colorRes = Color.parseColor(color);
        messageList.setDateTextColor(colorRes);
    }

    @ReactProp(name = "datePadding")
    public void setDatePadding(MessageList messageList, int padding) {
        messageList.setDatePadding(dip2px(padding));
    }

    @ReactProp(name = "avatarSize")
    public void setAvatarWidth(MessageList messageList, ReadableMap map) {
        messageList.setAvatarWidth(dip2px(map.getInt("width")));
        messageList.setAvatarHeight(dip2px(map.getInt("height")));
    }

    /**
     * if showDisplayName equals 1, then show display name.
     *
     * @param messageList       MessageList
     * @param isShowDisplayName boolean
     */
    @ReactProp(name = "isShowDisplayName")
    public void setShowDisplayName(MessageList messageList, boolean isShowDisplayName) {
        if (isShowDisplayName) {
            messageList.setShowReceiverDisplayName(1);
            messageList.setShowSenderDisplayName(1);
        } else {
            messageList.setShowSenderDisplayName(0);
            messageList.setShowReceiverDisplayName(0);
        }
    }

    @ReactProp(name = "isShowIncomingDisplayName")
    public void setShowReceiverDisplayName(MessageList messageList, boolean isShowDisplayName) {
        if (isShowDisplayName) {
            messageList.setShowReceiverDisplayName(1);
        } else {
            messageList.setShowSenderDisplayName(0);
        }
    }

    @ReactProp(name = "isShowOutgoingDisplayName")
    public void setShowSenderDisplayName(MessageList messageList, boolean isShowDisplayName) {
        if (isShowDisplayName) {
            messageList.setShowSenderDisplayName(1);
        } else {
            messageList.setShowSenderDisplayName(0);
        }
    }

    @ReactProp(name = "isAllowPullToRefresh")
    public void isAllowPullToRefresh(MessageList messageList, boolean flag) {
        messageList.forbidScrollToRefresh(!flag);
    }

    @ReactProp(name = "eventMsgTxtColor")
    public void setEventTextColor(MessageList messageList, String color) {
        int colorRes = Color.parseColor(color);
        messageList.setEventTextColor(colorRes);
    }

    @ReactProp(name = "eventMsgTxtPadding")
    public void setEventTextPadding(MessageList messageList, int padding) {
        messageList.setEventTextPadding(dip2px(padding));
    }

    @ReactProp(name = "eventMsgTxtSize")
    public void setEventTextSize(MessageList messageList, int size) {
        messageList.setEventTextSize(dip2sp(size));
    }

    @SuppressWarnings("unchecked")
    private BroadcastReceiver RCTMsgListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent) {
                return;
            }
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                mAdapter.setAudioPlayByEarPhone(intent.getIntExtra("state", 0));
            }
        }
    };

    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String, Object>builder()
                .put(ON_AVATAR_CLICK_EVENT, MapBuilder.of("registrationName", ON_AVATAR_CLICK_EVENT))
                .put(ON_MSG_CLICK_EVENT, MapBuilder.of("registrationName", ON_MSG_CLICK_EVENT))
                .put(ON_MSG_LONG_CLICK_EVENT, MapBuilder.of("registrationName", ON_MSG_LONG_CLICK_EVENT))
                .put(ON_STATUS_VIEW_CLICK_EVENT, MapBuilder.of("registrationName", ON_STATUS_VIEW_CLICK_EVENT))
                .put(ON_TOUCH_MSG_LIST_EVENT, MapBuilder.of("registrationName", ON_TOUCH_MSG_LIST_EVENT))
                .put(ON_PULL_TO_REFRESH_EVENT, MapBuilder.of("registrationName", ON_PULL_TO_REFRESH_EVENT))
                .build();
    }

    @Override
    public @Nullable Map<String, Integer> getCommandsMap() {
        return MapBuilder.of("unregister", 1);
    }

    @Override
    public void receiveCommand(MessageList root, int commandId, @Nullable ReadableArray args) {
        super.receiveCommand(root, commandId, args);
        Log.i(REACT_MESSAGE_LIST, "unregister invoke");
//        mContext.unregisterReceiver(RCTMsgListReceiver);
//        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDropViewInstance(MessageList view) {
        super.onDropViewInstance(view);
        try {
            EventBus.getDefault().unregister(this);
            mContext.unregisterReceiver(RCTMsgListReceiver);
            mSensorManager.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
    }

    private void registerProximitySensorListener() {
        try {
            Activity activity = mContext.getCurrentActivity();
            mPowerManager = (PowerManager) activity.getSystemService(POWER_SERVICE);
            mWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, REACT_MESSAGE_LIST);
            mSensorManager = (SensorManager) activity.getSystemService(SENSOR_SERVICE);
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(AUDIO_SERVICE);
        try {
            if (audioManager.isBluetoothA2dpOn() || audioManager.isWiredHeadsetOn()) {
                return;
            }
            if (mAdapter.getMediaPlayer().isPlaying()) {
                float distance = event.values[0];
                if (distance >= mSensor.getMaximumRange()) {
                    mAdapter.setAudioPlayByEarPhone(0);
                    setScreenOn();
                } else {
                    mAdapter.setAudioPlayByEarPhone(2);
                    ViewHolderController.getInstance().replayVoice();
                    setScreenOff();
                }
            } else {
                if (mWakeLock != null && mWakeLock.isHeld()) {
                    mWakeLock.release();
                    mWakeLock = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setScreenOn() {
        if (mWakeLock != null) {
            mWakeLock.setReferenceCounted(false);
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    private void setScreenOff() {
        if (mWakeLock == null) {
            mWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, REACT_MESSAGE_LIST);
        }
        mWakeLock.acquire();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}