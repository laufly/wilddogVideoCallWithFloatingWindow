package com.wilddog.floatingwindow;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wilddog.client.WilddogSync;
import com.wilddog.floatingwindow.util.PermissionUtils;
import com.wilddog.floatingwindow.util.StreamsHolder;
import com.wilddog.floatingwindow.util.WindowService;
import com.wilddog.video.CallStatus;
import com.wilddog.video.Conversation;
import com.wilddog.video.RemoteStream;
import com.wilddog.video.WilddogVideo;
import com.wilddog.video.base.LocalStream;
import com.wilddog.video.base.LocalStreamOptions;
import com.wilddog.video.base.WilddogVideoError;
import com.wilddog.video.base.WilddogVideoInitializer;
import com.wilddog.video.base.WilddogVideoView;
import com.wilddog.video.base.WilddogVideoViewLayout;
import com.wilddog.video.core.stats.LocalStreamStatsReport;
import com.wilddog.video.core.stats.RemoteStreamStatsReport;
import com.wilddog.wilddogauth.WilddogAuth;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class ConversationActivity extends AppCompatActivity {

    private static final String TAG = ConversationActivity.class.getSimpleName();

    private boolean isInConversation = false;
    private boolean isAudioEnable = true;
    @BindView(R.id.btn_invite)
    Button btnInvite;
    @BindView(R.id.local_video_view)
    WilddogVideoView localView;

    @BindView(R.id.remote_video_view)
    WilddogVideoView remoteView;

    private MyServiceConnection serviceConnection;

    private WilddogVideo video;
    private LocalStream localStream;
    private Conversation mConversation;
    private AlertDialog alertDialog;
    private Map<Conversation, AlertDialog> conversationAlertDialogMap;
    //AlertDialog列表
    private WilddogVideo.Listener inviteListener = new WilddogVideo.Listener() {
        @Override
        public void onCalled(final Conversation conversation, String s) {
            if(!TextUtils.isEmpty(s)){
                Toast.makeText(ConversationActivity.this,"对方邀请时候携带的信息是:"+s, Toast.LENGTH_SHORT).show();
            }
            mConversation = conversation;
            mConversation.setConversationListener(conversationListener);
            AlertDialog.Builder builder = new AlertDialog.Builder(ConversationActivity.this);
            builder.setMessage("邀请你加入会话");
            builder.setTitle("加入邀请");
            builder.setNegativeButton("拒绝邀请", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mConversation.reject();
                }
            });
            builder.setPositiveButton("确认加入", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    conversationAlertDialogMap.remove(conversation);
                    mConversation.accept(localStream);
                    isInConversation = true;

                }
            });

            alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
            conversationAlertDialogMap.put(conversation, alertDialog);
        }

        @Override
        public void onTokenError(WilddogVideoError wilddogVideoError) {

        }

    };







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams
                .FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View
                .SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_conversation);

        ButterKnife.bind(this);
        serviceConnection = new MyServiceConnection();
        //初始化Video
        WilddogVideoInitializer.initialize(getApplicationContext(), Constants.VIDEO_APPID, WilddogAuth.getInstance().getCurrentUser().getToken(false).getResult()
                .getToken());
        //获取video对象
        video = WilddogVideo.getInstance();
        remoteView.setZOrderMediaOverlay(true);
        remoteView.setVisibility(View.GONE);
        createAndShowLocalStream();
        conversationAlertDialogMap = new HashMap<>();
        //在使用inviteToConversation方法前需要先设置会话邀请监听，否则使用邀请功能会抛出IllegalStateException异常
        video.setListener(inviteListener);

    }

    private void createAndShowLocalStream() {

        LocalStreamOptions.Builder builder = new LocalStreamOptions.Builder();
        LocalStreamOptions options = builder.dimension(LocalStreamOptions.Dimension.DIMENSION_480P).build();
        //创建本地视频流，通过video对象获取本地视频流
        localStream = LocalStream.create(options);
        //开启音频/视频，设置为 false 则关闭声音或者视频画面
        //localStream.enableAudio(true);
        // localStream.enableVideo(true);
        //为视频流绑定播放控件
        localStream.attach(localView);
        StreamsHolder.setLocalStream(localStream);
    }


    @OnClick(R.id.btn_invite)
    public void invite() {
        //取消发起会话邀请
        showLoginUsers();

    }
    @OnClick(R.id.btn_show)
    public void showFloatWindow() {
        //显示悬浮框
        PermissionUtils.requestPermissionsResult(ConversationActivity.this,1,
                new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW
                }, new PermissionUtils.OnPermissionListener() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onPermissionGranted() {
                        showFloatingWindow();
                    }

                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onPermissionDenied() {
                        showFloatingWindow();
                        Toast.makeText(ConversationActivity.this, "请打开悬浮窗权限", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void showFloatingWindow() {
        moveTaskToBack(true);
        StreamsHolder.getLocalStream().detach();
        StreamsHolder.getRemoteStream().detach();
        Intent  service = new Intent(ConversationActivity.this, WindowService.class);

        if (mybinder != null&& !mybinder.isMiUI8) {
            mybinder.showFloatingWindow();
        } else {
            startService(service);
            bindService(service, serviceConnection, Service.BIND_AUTO_CREATE);
            if (mybinder!=null) {
                mybinder.showFloatingWindowMiUI8();
            }
        }
    }

    private WindowService.MyBinder mybinder;

    public class MyServiceConnection implements ServiceConnection {
        private boolean isbind =false;

        public boolean getIsbind(){
            return isbind;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mybinder = (WindowService.MyBinder) iBinder;
            isbind = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onResume() {
        super.onResume();
        if (mybinder != null) {
            if (mybinder.isMiUI8) {
//                mybinder.hidwindowMiUI8();
            } else {
                mybinder.hidFloatingWindow();
            }
        }
        if(StreamsHolder.getLocalStream()!=null){
            StreamsHolder.getLocalStream().attach(localView);
        }
        if(StreamsHolder.getRemoteStream()!=null){
            StreamsHolder.getRemoteStream().attach(remoteView);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @OnClick(R.id.btn_mic)
    public void mic(){
        if(localStream!=null){
            isAudioEnable = !isAudioEnable;
            localStream.enableAudio(isAudioEnable);
        }
    }

    @OnClick(R.id.btn_invite_cancel)
    public void inviteCancel() {

        closeConversation();
    }

    private void closeConversation() {
        if (mConversation != null) {
            mConversation.close();
            mConversation = null;
            //挂断时会释放本地流，如需继续显示本地流，则挂断后要重新获取一次本地流
        }
        btnInvite.setText("用户列表");
    }


    private void showLoginUsers() {
        startActivityForResult(new Intent(ConversationActivity.this, UserListActivity.class), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            //选取用户列表中的用户，获得其 Wilddog UID
            String participant = data.getStringExtra("participant");
            //调用inviteToConversation 方法发起会话
            inviteToConversation(participant);
            btnInvite.setText("用户列表");
        }
    }


    private void inviteToConversation(String participant) {
        String data = "extra data";
        //创建连接参数对象
        mConversation = video.call(participant, localStream, data);
        mConversation.setConversationListener(conversationListener);

    }

    private Conversation.Listener conversationListener = new Conversation.Listener() {
        @Override
        public void onCallResponse(CallStatus callStatus) {
            switch (callStatus) {
                case ACCEPTED:

                    isInConversation = true;
                    break;
                case REJECTED:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ConversationActivity.this, "对方拒绝你的邀请", Toast.LENGTH_SHORT).show();
                            isInConversation = false;
                            btnInvite.setText("用户列表");
                        }
                    });
                    break;
                case BUSY:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ConversationActivity.this, "对方正在通话中,稍后再呼叫", Toast.LENGTH_SHORT).show();
                            isInConversation = false;
                            btnInvite.setText("用户列表");
                        }
                    });
                    break;
                case TIMEOUT:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ConversationActivity.this, "呼叫对方超时,请稍后再呼叫", Toast.LENGTH_SHORT).show();
                            isInConversation = false;
                            btnInvite.setText("用户列表");
                        }
                    });
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onStreamReceived(final RemoteStream remoteStream) {
            remoteStream.attach(remoteView);
            StreamsHolder.setRemoteStream(remoteStream);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    remoteView.setVisibility(View.VISIBLE);
                    btnInvite.setText("用户已加入");
                }
            });
        }

        @Override
        public void onClosed() {
            Log.e(TAG, "onClosed");
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
            isInConversation = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    remoteView.setVisibility(View.GONE);
                    closeConversation();
                    Toast.makeText(ConversationActivity.this, "对方挂断", Toast.LENGTH_SHORT).show();
                }
            });

        }

        @Override
        public void onError(WilddogVideoError wilddogVideoError) {
            if (wilddogVideoError != null) {
                Toast.makeText(ConversationActivity.this, "通话中出错,请查看日志", Toast.LENGTH_SHORT).show();
                Log.e("error", wilddogVideoError.getMessage());
                btnInvite.setText("用户列表");
                isInConversation = false;
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //解绑服务
        if (serviceConnection != null && serviceConnection.getIsbind()) {
            unbindService(serviceConnection);
        }
        //需要离开会话时调用此方法，并做资源释放和其他自定义操作
        if (localView != null) {
            localView.release();
            localView = null;
        }
        if (remoteView != null) {
            remoteView.release();
            remoteView = null;
        }
        if (mConversation != null) {
            mConversation.close();
        }
        if (localStream != null) {
            if (!localStream.isClosed()) {
                localStream.close();
            }
        }
        WilddogSync.goOffline();
    }
}
