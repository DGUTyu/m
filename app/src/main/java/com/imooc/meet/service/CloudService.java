package com.imooc.meet.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.google.gson.Gson;
import com.imooc.meet.MainActivity;
import com.imooc.meet.R;
import com.imooc.meet.ui.ChatActivity;
import com.imooc.meet.ui.NewFriendActivity;
import com.liuguilin.framework.bmob.BmobManager;
import com.liuguilin.framework.bmob.IMUser;
import com.liuguilin.framework.cloud.CloudManager;
import com.liuguilin.framework.db.CallRecord;
import com.liuguilin.framework.db.LitePalHelper;
import com.liuguilin.framework.entity.Constants;
import com.liuguilin.framework.event.EventManager;
import com.liuguilin.framework.event.MessageEvent;
import com.liuguilin.framework.gson.TextBean;
import com.liuguilin.framework.helper.GlideHelper;
import com.liuguilin.framework.helper.NotificationHelper;
import com.liuguilin.framework.helper.WindowHelper;
import com.liuguilin.framework.manager.MediaPlayerManager;
import com.liuguilin.framework.utils.CommonUtils;
import com.liuguilin.framework.utils.LogUtils;
import com.liuguilin.framework.utils.SpUtils;
import com.liuguilin.framework.utils.TimeUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;
import de.hdodenhof.circleimageview.CircleImageView;
import io.reactivex.disposables.Disposable;
import io.rong.calllib.IRongCallListener;
import io.rong.calllib.IRongReceivedCallListener;
import io.rong.calllib.RongCallCommon;
import io.rong.calllib.RongCallSession;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.ImageMessage;
import io.rong.message.LocationMessage;
import io.rong.message.TextMessage;

/**
 * FileName: CloudService
 * Founder: LiuGuiLin
 * Profile: ?????????
 */
public class CloudService extends Service implements View.OnClickListener {

    //??????
    private static final int H_TIME_WHAT = 1000;

    //????????????
    private int callTimer = 0;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case H_TIME_WHAT:
                    callTimer++;
                    String time = TimeUtils.formatDuring(callTimer * 1000);
                    audio_tv_status.setText(time);
                    video_tv_time.setText(time);
                    mSmallTime.setText(time);
                    mHandler.sendEmptyMessageDelayed(H_TIME_WHAT, 1000);
                    break;
            }
            return false;
        }
    });

    private Disposable disposable;

    //????????????
    private View mFullAudioView;
    //??????
    private CircleImageView audio_iv_photo;
    //??????
    private TextView audio_tv_status;
    //????????????
    private ImageView audio_iv_recording;
    //????????????
    private LinearLayout audio_ll_recording;
    //????????????
    private ImageView audio_iv_answer;
    //????????????
    private LinearLayout audio_ll_answer;
    //????????????
    private ImageView audio_iv_hangup;
    //????????????
    private LinearLayout audio_ll_hangup;
    //????????????
    private ImageView audio_iv_hf;
    //????????????
    private LinearLayout audio_ll_hf;
    //?????????
    private ImageView audio_iv_small;

    //????????????
    private View mFullVideoView;
    //?????????
    private RelativeLayout video_big_video;
    //?????????
    private RelativeLayout video_small_video;
    //??????
    private CircleImageView video_iv_photo;
    //??????
    private TextView video_tv_name;
    //??????
    private TextView video_tv_status;
    //??????????????????
    private LinearLayout video_ll_info;
    //??????
    private TextView video_tv_time;
    //??????
    private LinearLayout video_ll_answer;
    //??????
    private LinearLayout video_ll_hangup;

    //??????????????????View
    private WindowManager.LayoutParams lpSmallView;
    private View mSmallAudioView;
    //??????
    private TextView mSmallTime;

    //??????ID
    private String callId = "";

    //?????????
    private MediaPlayerManager mAudioCallMedia;
    private MediaPlayerManager mAudioHangupMedia;

    //?????????
    private SurfaceView mLocalView;
    private SurfaceView mRemoteView;

    //?????????????????????????????????
    private boolean isSmallShowLocal = false;

    //????????????
    private int isCallTo = 0;
    //????????????
    private int isReceiverTo = 0;
    //??????????????????
    private boolean isCallOrReceiver = true;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initService();
        initWindow();
        linkCloudServer();
    }

    /**
     * ???????????????
     */
    private void initService() {

        EventManager.register(this);

        //????????????
        mAudioCallMedia = new MediaPlayerManager();
        //????????????
        mAudioHangupMedia = new MediaPlayerManager();

        //????????????
        mAudioCallMedia.setOnComplteionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mAudioCallMedia.startPlay(CloudManager.callAudioPath);
            }
        });
    }

    /**
     * ???????????????
     */
    private void initWindow() {

        //??????
        mFullAudioView = WindowHelper.getInstance().getView(R.layout.layout_chat_audio);
        audio_iv_photo = mFullAudioView.findViewById(R.id.audio_iv_photo);
        audio_tv_status = mFullAudioView.findViewById(R.id.audio_tv_status);
        audio_iv_recording = mFullAudioView.findViewById(R.id.audio_iv_recording);
        audio_ll_recording = mFullAudioView.findViewById(R.id.audio_ll_recording);
        audio_iv_answer = mFullAudioView.findViewById(R.id.audio_iv_answer);
        audio_ll_answer = mFullAudioView.findViewById(R.id.audio_ll_answer);
        audio_iv_hangup = mFullAudioView.findViewById(R.id.audio_iv_hangup);
        audio_ll_hangup = mFullAudioView.findViewById(R.id.audio_ll_hangup);
        audio_iv_hf = mFullAudioView.findViewById(R.id.audio_iv_hf);
        audio_ll_hf = mFullAudioView.findViewById(R.id.audio_ll_hf);
        audio_iv_small = mFullAudioView.findViewById(R.id.audio_iv_small);

        audio_ll_recording.setOnClickListener(this);
        audio_ll_answer.setOnClickListener(this);
        audio_ll_hangup.setOnClickListener(this);
        audio_ll_hf.setOnClickListener(this);
        audio_iv_small.setOnClickListener(this);

        //??????
        mFullVideoView = WindowHelper.getInstance().getView(R.layout.layout_chat_video);
        video_big_video = mFullVideoView.findViewById(R.id.video_big_video);
        video_small_video = mFullVideoView.findViewById(R.id.video_small_video);
        video_iv_photo = mFullVideoView.findViewById(R.id.video_iv_photo);
        video_tv_name = mFullVideoView.findViewById(R.id.video_tv_name);
        video_tv_status = mFullVideoView.findViewById(R.id.video_tv_status);
        video_ll_info = mFullVideoView.findViewById(R.id.video_ll_info);
        video_tv_time = mFullVideoView.findViewById(R.id.video_tv_time);
        video_ll_answer = mFullVideoView.findViewById(R.id.video_ll_answer);
        video_ll_hangup = mFullVideoView.findViewById(R.id.video_ll_hangup);

        video_ll_answer.setOnClickListener(this);
        video_ll_hangup.setOnClickListener(this);
        video_small_video.setOnClickListener(this);

        createSmallAudioView();
    }

    /**
     * ??????????????????????????????
     */
    private void createSmallAudioView() {
        lpSmallView = WindowHelper.getInstance().createLayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.LEFT);
        mSmallAudioView = WindowHelper.getInstance().getView(R.layout.layout_chat_small_audio);
        mSmallTime = mSmallAudioView.findViewById(R.id.mSmallTime);

        mSmallAudioView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //?????????
                WindowHelper.getInstance().hideView(mSmallAudioView);
                WindowHelper.getInstance().showView(mFullAudioView);
            }
        });

        mSmallAudioView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                /**
                 * OnTouch ??? OnClick ????????????
                 * ????????????????????? ?????? ??????
                 * ???????????????????????? - ??????????????? ?????????????????????????????? ?????? = 0 ????????????????????????????????????
                 */
                int mStartX = (int) event.getRawX();
                int mStartY = (int) event.getRawY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isMove = false;
                        isDrag = false;
                        mLastX = (int) event.getRawX();
                        mLastY = (int) event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:

                        //?????????
                        int dx = mStartX - mLastX;
                        int dy = mStartY - mLastY;

                        if (isMove) {
                            isDrag = true;
                        } else {
                            if (dx == 0 && dy == 0) {
                                isMove = false;
                            } else {
                                isMove = true;
                                isDrag = true;
                            }
                        }

                        //??????
                        lpSmallView.x += dx;
                        lpSmallView.y += dy;

                        //????????????
                        mLastX = mStartX;
                        mLastY = mStartY;

                        //WindowManager addView removeView updateView
                        WindowHelper.getInstance().updateView(mSmallAudioView, lpSmallView);

                        break;
                }
                return isDrag;
            }
        });
    }

    //????????????
    private boolean isMove = false;
    //????????????
    private boolean isDrag = false;
    private int mLastX;
    private int mLastY;

    /**
     * ???????????????
     */
    private void linkCloudServer() {
        //??????Token
        String token = SpUtils.getInstance().getString(Constants.SP_TOKEN, "");
        LogUtils.e("token:" + token);
        //????????????
        CloudManager.getInstance().connect(token);
        //????????????
        CloudManager.getInstance().setOnReceiveMessageListener((message, i) -> {
            parsingImMessage(message);
            return false;
        });

        //????????????
        CloudManager.getInstance().setReceivedCallListener(new IRongReceivedCallListener() {
            @Override
            public void onReceivedCall(RongCallSession rongCallSession) {
                LogUtils.i("rongCallSession");

                //??????????????????
                if (!CloudManager.getInstance().isVoIPEnabled(CloudService.this)) {
                    return;
                }

                /**
                 * 1.????????????????????????ID
                 * 2.????????????????????????
                 * 3.???????????????????????????
                 * 4.??????Window
                 */

                //????????????ID
                String callUserId = rongCallSession.getCallerUserId();

                //??????ID
                callId = rongCallSession.getCallId();

                //??????????????????
                mAudioCallMedia.startPlay(CloudManager.callAudioPath);

                //??????????????????
                updateWindowInfo(0, rongCallSession.getMediaType(), callUserId);

                if (rongCallSession.getMediaType().equals(RongCallCommon.CallMediaType.AUDIO)) {
                    WindowHelper.getInstance().showView(mFullAudioView);
                } else if (rongCallSession.getMediaType().equals(RongCallCommon.CallMediaType.VIDEO)) {
                    WindowHelper.getInstance().showView(mFullVideoView);
                }

                isReceiverTo = 1;

                isCallOrReceiver = false;
            }

            @Override
            public void onCheckPermission(RongCallSession rongCallSession) {
                LogUtils.i("onCheckPermission:" + rongCallSession.toString());
            }
        });

        //??????????????????
        CloudManager.getInstance().setVoIPCallListener(new IRongCallListener() {

            //????????????
            @Override
            public void onCallOutgoing(RongCallSession rongCallSession, SurfaceView surfaceView) {
                LogUtils.i("onCallOutgoing");

                isCallOrReceiver = true;

                isCallTo = 1;

                //????????????
                String targetId = rongCallSession.getTargetId();
                //????????????
                updateWindowInfo(1, rongCallSession.getMediaType(), targetId);

                //??????ID
                callId = rongCallSession.getCallId();

                if (rongCallSession.getMediaType().equals(RongCallCommon.CallMediaType.AUDIO)) {
                    WindowHelper.getInstance().showView(mFullAudioView);
                } else if (rongCallSession.getMediaType().equals(RongCallCommon.CallMediaType.VIDEO)) {
                    WindowHelper.getInstance().showView(mFullVideoView);
                    //???????????????
                    mLocalView = surfaceView;
                    video_big_video.addView(mLocalView);
                }

            }

            //???????????????
            @Override
            public void onCallConnected(RongCallSession rongCallSession, SurfaceView surfaceView) {
                LogUtils.i("onCallConnected");

                /**
                 * 1.????????????
                 * 2.????????????
                 * 3.????????????
                 */

                isCallTo = 2;
                isReceiverTo = 2;

                //????????????
                if (mAudioCallMedia.isPlaying()) {
                    mAudioCallMedia.stopPlay();
                }

                //????????????
                mHandler.sendEmptyMessage(H_TIME_WHAT);

                if (rongCallSession.getMediaType().equals(RongCallCommon.CallMediaType.AUDIO)) {
                    goneAudioView(true, false, true, true, true);
                } else if (rongCallSession.getMediaType().equals(RongCallCommon.CallMediaType.VIDEO)) {
                    goneVideoView(false, true, true, false, true, true);
                    mLocalView = surfaceView;
                }

            }

            //????????????
            @Override
            public void onCallDisconnected(RongCallSession rongCallSession, RongCallCommon.CallDisconnectedReason callDisconnectedReason) {
                LogUtils.i("onCallDisconnected");

                String callUserId = rongCallSession.getCallerUserId();
                String recevierId = rongCallSession.getTargetId();

                //????????????
                mHandler.removeMessages(H_TIME_WHAT);

                //????????????
                mAudioCallMedia.pausePlay();

                //??????????????????
                mAudioHangupMedia.startPlay(CloudManager.callAudioHangup);

                //???????????????
                callTimer = 0;

                if (rongCallSession.getMediaType().equals(RongCallCommon.CallMediaType.AUDIO)) {
                    if (isCallOrReceiver) {
                        if (isCallTo == 1) {
                            //???????????????????????????????????????
                            saveAudioRecord(recevierId, CallRecord.CALL_STATUS_DIAL);
                        } else if (isCallTo == 2) {
                            saveAudioRecord(recevierId, CallRecord.CALL_STATUS_ANSWER);
                        }
                    } else {
                        if (isReceiverTo == 1) {
                            saveAudioRecord(callUserId, CallRecord.CALL_STATUS_UN_ANSWER);
                        } else if (isReceiverTo == 2) {
                            saveAudioRecord(callUserId, CallRecord.CALL_STATUS_ANSWER);
                        }
                    }

                } else if (rongCallSession.getMediaType().equals(RongCallCommon.CallMediaType.VIDEO)) {
                    if (isCallOrReceiver) {
                        if (isCallTo == 1) {
                            //???????????????????????????????????????
                            saveVideoRecord(recevierId, CallRecord.CALL_STATUS_DIAL);
                        } else if (isCallTo == 2) {
                            saveVideoRecord(recevierId, CallRecord.CALL_STATUS_ANSWER);
                        }
                    } else {
                        if (isReceiverTo == 1) {
                            saveVideoRecord(callUserId, CallRecord.CALL_STATUS_UN_ANSWER);
                        } else if (isReceiverTo == 2) {
                            saveVideoRecord(callUserId, CallRecord.CALL_STATUS_ANSWER);
                        }
                    }
                }

                //??????????????????,??????????????????
                WindowHelper.getInstance().hideView(mFullAudioView);
                WindowHelper.getInstance().hideView(mSmallAudioView);
                WindowHelper.getInstance().hideView(mFullVideoView);

                isCallTo = 0;
                isReceiverTo = 0;
            }

            //?????????????????????
            @Override
            public void onRemoteUserRinging(String s) {

            }

            //?????????????????????
            @Override
            public void onRemoteUserJoined(String s, RongCallCommon.CallMediaType callMediaType, int i, SurfaceView surfaceView) {
                //?????????
                MessageEvent event = new MessageEvent(EventManager.FLAG_SEND_CAMERA_VIEW);
                event.setmSurfaceView(surfaceView);
                EventManager.post(event);
            }

            //????????????????????????????????????????????????
            @Override
            public void onRemoteUserInvited(String s, RongCallCommon.CallMediaType callMediaType) {

            }

            //?????????????????????????????????
            @Override
            public void onRemoteUserLeft(String s, RongCallCommon.CallDisconnectedReason callDisconnectedReason) {

            }

            //????????????
            @Override
            public void onMediaTypeChanged(String s, RongCallCommon.CallMediaType callMediaType, SurfaceView surfaceView) {

            }

            //????????????
            @Override
            public void onError(RongCallCommon.CallErrorCode callErrorCode) {

            }

            //??????????????????????????????
            @Override
            public void onRemoteCameraDisabled(String s, boolean b) {

            }

            //??????????????????????????????
            @Override
            public void onRemoteMicrophoneDisabled(String s, boolean b) {

            }

            //???????????????
            @Override
            public void onNetworkReceiveLost(String s, int i) {

            }

            //???????????????
            @Override
            public void onNetworkSendLost(int i, int i1) {

            }

            //?????????????????????
            @Override
            public void onFirstRemoteVideoFrame(String s, int i, int i1) {

            }
        });
    }

    /**
     * ???????????????
     *
     * @param message
     */
    private void parsingImMessage(Message message) {
        LogUtils.i("message:" + message);
        String objectName = message.getObjectName();
        //????????????
        if (objectName.equals(CloudManager.MSG_TEXT_NAME)) {
            //??????????????????
            TextMessage textMessage = (TextMessage) message.getContent();
            String content = textMessage.getContent();
            LogUtils.i("content:" + content);
            TextBean textBean = null;
            try {
                LogUtils.i("Gson Try:1");
                textBean = new Gson().fromJson(content, TextBean.class);
                LogUtils.i("Gson Try:2");
            } catch (Exception e) {
                LogUtils.i("Gson Try:" + e.toString());
                e.printStackTrace();
            }
            LogUtils.i("Gson Try:3");
            if (null == textBean) {
                LogUtils.i("Gson Try:4");
                //??????????????????
                MessageEvent event = new MessageEvent(EventManager.FLAG_SEND_TEXT);
                event.setText(content);
                event.setUserId(message.getSenderUserId());
                EventManager.post(event);
                pushSystem(message.getSenderUserId(), 1, 0, 0, content);
                return;
            }
            //????????????
            if (textBean.getType().equals(CloudManager.TYPE_TEXT)) {
                MessageEvent event = new MessageEvent(EventManager.FLAG_SEND_TEXT);
                event.setText(textBean.getMsg());
                event.setUserId(message.getSenderUserId());
                EventManager.post(event);
                pushSystem(message.getSenderUserId(), 1, 0, 0, textBean.getMsg());
                //??????????????????
            } else if (textBean.getType().equals(CloudManager.TYPE_ADD_FRIEND)) {
                //??????????????? Bmob RongCloud ???????????????????????????
                //?????????????????????????????? ?????????????????????
                LogUtils.i("??????????????????");
                saveNewFriend(textBean.getMsg(), message.getSenderUserId());
                //?????????????????????????????????????????????
                //???????????????????????????????????????????????????
//                disposable = Observable.create((ObservableOnSubscribe<List<NewFriend>>) emitter -> {
//                    emitter.onNext(LitePalHelper.getInstance().queryNewFriend());
//                    emitter.onComplete();
//                }).subscribeOn(Schedulers.newThread())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(newFriends -> {
//                            if (CommonUtils.isEmpty(newFriends)) {
//                                boolean isHave = false;
//                                for (int j = 0; j < newFriends.size(); j++) {
//                                    NewFriend newFriend = newFriends.get(j);
//                                    if (message.getSenderUserId().equals(newFriend.getId())) {
//                                        isHave = true;
//                                        break;
//                                    }
//                                }
//                                //??????????????????
//                                if (!isHave) {
//                                    saveNewFriend(textBean.getMsg(), message.getSenderUserId());
//                                }
//                            } else {
//                                saveNewFriend(textBean.getMsg(), message.getSenderUserId());
//                            }
//                        });
                //????????????????????????
            } else if (textBean.getType().equals(CloudManager.TYPE_ARGEED_FRIEND)) {
                //1.?????????????????????
                BmobManager.getInstance().addFriend(message.getSenderUserId(), new SaveListener<String>() {
                    @Override
                    public void done(String s, BmobException e) {
                        if (e == null) {
                            pushSystem(message.getSenderUserId(), 0, 1, 0, "");
                            //2.??????????????????
                            EventManager.post(EventManager.FLAG_UPDATE_FRIEND_LIST);
                        }
                    }
                });
            }
        } else if (objectName.equals(CloudManager.MSG_IMAGE_NAME)) {
            try {
                ImageMessage imageMessage = (ImageMessage) message.getContent();
                String url = imageMessage.getRemoteUri().toString();
                if (!TextUtils.isEmpty(url)) {
                    LogUtils.i("url:" + url);
                    MessageEvent event = new MessageEvent(EventManager.FLAG_SEND_IMAGE);
                    event.setImgUrl(url);
                    event.setUserId(message.getSenderUserId());
                    EventManager.post(event);
                    pushSystem(message.getSenderUserId(), 1, 0, 0, getString(R.string.text_chat_record_img));
                }
            } catch (Exception e) {
                LogUtils.e("e." + e.toString());
                e.printStackTrace();
            }
        } else if (objectName.equals(CloudManager.MSG_LOCATION_NAME)) {
            LocationMessage locationMessage = (LocationMessage) message.getContent();
            LogUtils.e("locationMessage:" + locationMessage.toString());
            MessageEvent event = new MessageEvent(EventManager.FLAG_SEND_LOCATION);
            event.setLa(locationMessage.getLat());
            event.setLo(locationMessage.getLng());
            event.setUserId(message.getSenderUserId());
            event.setAddress(locationMessage.getPoi());
            EventManager.post(event);
            pushSystem(message.getSenderUserId(), 1, 0, 0, getString(R.string.text_chat_record_location));
        }
    }

    /**
     * ???????????????
     *
     * @param msg
     * @param senderUserId
     */
    private void saveNewFriend(String msg, String senderUserId) {
        pushSystem(senderUserId, 0, 0, 0, msg);
        LitePalHelper.getInstance().saveNewFriend(msg, senderUserId);
    }

    /**
     * ??????????????????????????????
     *
     * @param index ????????????
     * @param index 0:?????? 1?????????
     * @param id
     */
    private void updateWindowInfo(final int index, final RongCallCommon.CallMediaType type, String id) {
        //??????
        if (type.equals(RongCallCommon.CallMediaType.AUDIO)) {
            if (index == 0) {
                goneAudioView(false, true, true, false, false);
            } else if (index == 1) {
                goneAudioView(false, false, true, false, false);
            }
            //??????
        } else if (type.equals(RongCallCommon.CallMediaType.VIDEO)) {
            if (index == 0) {
                goneVideoView(true, false, false, true, true, false);
            } else if (index == 1) {
                goneVideoView(true, false, true, false, true, false);
            }
        }

        //????????????
        BmobManager.getInstance().queryObjectIdUser(id, new FindListener<IMUser>() {
            @Override
            public void done(List<IMUser> list, BmobException e) {
                if (e == null) {
                    if (CommonUtils.isEmpty(list)) {
                        IMUser imUser = list.get(0);
                        //??????
                        if (type.equals(RongCallCommon.CallMediaType.AUDIO)) {
                            GlideHelper.loadUrl(CloudService.this, imUser.getPhoto(), audio_iv_photo);
                            if (index == 0) {
                                audio_tv_status.setText(imUser.getNickName() + getString(R.string.text_service_calling));
                            } else if (index == 1) {
                                audio_tv_status.setText(getString(R.string.text_service_call_ing) + imUser.getNickName() + "...");
                            }
                            //??????
                        } else if (type.equals(RongCallCommon.CallMediaType.VIDEO)) {
                            GlideHelper.loadUrl(CloudService.this, imUser.getPhoto(), video_iv_photo);
                            video_tv_name.setText(imUser.getNickName());
                            if (index == 0) {
                                video_tv_status.setText(imUser.getNickName() + getString(R.string.text_service_video_calling));
                            } else if (index == 1) {
                                video_tv_status.setText(getString(R.string.text_service_call_video_ing) + imUser.getNickName() + "...");
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param recording
     * @param answer
     * @param hangup
     * @param hf
     * @param small
     */
    private void goneAudioView(boolean recording, boolean answer, boolean hangup, boolean hf,
                               boolean small) {
        // ?????? ?????? ?????? ?????? ?????????
        audio_ll_recording.setVisibility(recording ? View.VISIBLE : View.GONE);
        audio_ll_answer.setVisibility(answer ? View.VISIBLE : View.GONE);
        audio_ll_hangup.setVisibility(hangup ? View.VISIBLE : View.GONE);
        audio_ll_hf.setVisibility(hf ? View.VISIBLE : View.GONE);
        audio_iv_small.setVisibility(small ? View.VISIBLE : View.GONE);
    }

    /**
     * ?????????????????????????????????????????????
     * * @param info
     * * @param small
     *
     * @param answer
     * @param hangup
     * @param time
     */
    private void goneVideoView(boolean info, boolean small,
                               boolean big, boolean answer, boolean hangup,
                               boolean time) {
        // ???????????? ?????????  ??????  ?????? ??????
        video_ll_info.setVisibility(info ? View.VISIBLE : View.GONE);
        video_small_video.setVisibility(small ? View.VISIBLE : View.GONE);
        video_big_video.setVisibility(big ? View.VISIBLE : View.GONE);
        video_ll_answer.setVisibility(answer ? View.VISIBLE : View.GONE);
        video_ll_hangup.setVisibility(hangup ? View.VISIBLE : View.GONE);
        video_tv_time.setVisibility(time ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }
        }
        EventManager.unregister(this);
    }

    private boolean isRecording = false;
    private boolean isHF = false;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.audio_ll_recording:
                if (isRecording) {
                    isRecording = false;
                    CloudManager.getInstance().stopAudioRecording();
                    audio_iv_recording.setImageResource(R.drawable.img_recording);
                } else {
                    isRecording = true;
                    //??????
                    CloudManager.getInstance().startAudioRecording(
                            "/sdcard/Meet/" + System.currentTimeMillis() + "wav");
                    audio_iv_recording.setImageResource(R.drawable.img_recording_p);
                }
                break;
            case R.id.audio_ll_answer:
            case R.id.video_ll_answer:
                //??????
                CloudManager.getInstance().acceptCall(callId);
                break;
            case R.id.audio_ll_hangup:
            case R.id.video_ll_hangup:
                //??????
                CloudManager.getInstance().hangUpCall(callId);
                break;
            case R.id.audio_ll_hf:
                isHF = !isHF;
                CloudManager.getInstance().setEnableSpeakerphone(isHF);
                audio_iv_hf.setImageResource(isHF ? R.drawable.img_hf_p : R.drawable.img_hf);
                break;
            case R.id.audio_iv_small:
                //?????????
                WindowHelper.getInstance().hideView(mFullAudioView);
                WindowHelper.getInstance().showView(mSmallAudioView, lpSmallView);
                break;
            case R.id.video_small_video:
                isSmallShowLocal = !isSmallShowLocal;
                //????????????
                updateVideoView();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        switch (event.getType()) {
            case EventManager.FLAG_SEND_CAMERA_VIEW:
                SurfaceView sv = event.getmSurfaceView();
                if (sv != null) {
                    mRemoteView = sv;
                }
                updateVideoView();
                break;
        }
    }

    /**
     * ???????????????
     */
    private void updateVideoView() {
        video_big_video.removeAllViews();
        video_small_video.removeAllViews();

        if (isSmallShowLocal) {
            if (mLocalView != null) {
                video_small_video.addView(mLocalView);
                mLocalView.setZOrderOnTop(true);
            }
            if (mRemoteView != null) {
                video_big_video.addView(mRemoteView);
                mRemoteView.setZOrderOnTop(false);
            }
        } else {
            if (mLocalView != null) {
                video_big_video.addView(mLocalView);
                mLocalView.setZOrderOnTop(false);
            }
            if (mRemoteView != null) {
                video_small_video.addView(mRemoteView);
                mRemoteView.setZOrderOnTop(true);
            }
        }
    }

    /**
     * ??????????????????
     *
     * @param id
     * @param callStatus
     */
    private void saveAudioRecord(String id, int callStatus) {
        LitePalHelper.getInstance()
                .saveCallRecord(id, CallRecord.MEDIA_TYPE_AUDIO, callStatus);
    }

    /**
     * ??????????????????
     *
     * @param id
     * @param callStatus
     */
    private void saveVideoRecord(String id, int callStatus) {
        LitePalHelper.getInstance()
                .saveCallRecord(id, CallRecord.MEDIA_TYPE_VIDEO, callStatus);
    }

    /**
     * @param id          ?????????id
     * @param type        0??????????????? 1???????????????
     * @param friendType  0: ?????????????????? 1?????????????????????
     * @param messageType 0?????????  1????????? 2?????????
     */
    private void pushSystem(final String id, final int type, final int friendType, final int messageType, final String msgText) {
        LogUtils.i("pushSystem");
        BmobManager.getInstance().queryObjectIdUser(id, new FindListener<IMUser>() {
            @Override
            public void done(List<IMUser> list, BmobException e) {
                if (e == null) {
                    if (CommonUtils.isEmpty(list)) {
                        IMUser imUser = list.get(0);
                        String text = "";
                        if (type == 0) {
                            switch (friendType) {
                                case 0:
                                    text = imUser.getNickName() + getString(R.string.text_server_noti_send_text);
                                    break;
                                case 1:
                                    text = imUser.getNickName() + getString(R.string.text_server_noti_receiver_text);
                                    break;
                            }
                        } else if (type == 1) {
                            switch (messageType) {
                                case 0:
                                    text = msgText;
                                    break;
                                case 1:
                                    text = getString(R.string.text_chat_record_img);
                                    break;
                                case 2:
                                    text = getString(R.string.text_chat_record_location);
                                    break;
                            }
                        }
                        pushBitmap(type, friendType, imUser, imUser.getNickName(), text, imUser.getPhoto());
                    }
                }
            }
        });
    }

    /**
     * ????????????
     *
     * @param type       0??????????????? 1???????????????
     * @param friendType 0: ?????????????????? 1?????????????????????
     * @param imUser     ????????????
     * @param title      ??????
     * @param text       ??????
     * @param url        ??????Url
     */
    private void pushBitmap(final int type, final int friendType, final IMUser imUser, final String title, final String text, String url) {
        LogUtils.i("pushBitmap");
        GlideHelper.loadUrlToBitmap(this, url, new GlideHelper.OnGlideBitmapResultListener() {
            @Override
            public void onResourceReady(Bitmap resource) {
                if (type == 0) {
                    if (friendType == 0) {
                        Intent intent = new Intent(CloudService.this, NewFriendActivity.class);
                        PendingIntent pi = PendingIntent.getActivities(CloudService.this, 0, new Intent[]{intent}, PendingIntent.FLAG_CANCEL_CURRENT);
                        NotificationHelper.getInstance().pushAddFriendNotification(imUser.getObjectId(), title, text, resource, pi);
                    } else if (friendType == 1) {
                        Intent intent = new Intent(CloudService.this, MainActivity.class);
                        PendingIntent pi = PendingIntent.getActivities(CloudService.this, 0, new Intent[]{intent}, PendingIntent.FLAG_CANCEL_CURRENT);
                        NotificationHelper.getInstance().pushArgeedFriendNotification(imUser.getObjectId(), title, text, resource, pi);
                    }
                } else if (type == 1) {
                    Intent intent = new Intent(CloudService.this, ChatActivity.class);
                    intent.putExtra(Constants.INTENT_USER_ID, imUser.getObjectId());
                    intent.putExtra(Constants.INTENT_USER_NAME, imUser.getNickName());
                    intent.putExtra(Constants.INTENT_USER_PHOTO, imUser.getPhoto());
                    PendingIntent pi = PendingIntent.getActivities(CloudService.this, 0, new Intent[]{intent}, PendingIntent.FLAG_CANCEL_CURRENT);
                    NotificationHelper.getInstance().pushMessageNotification(imUser.getObjectId(), title, text, resource, pi);
                }
            }
        });
    }
}
