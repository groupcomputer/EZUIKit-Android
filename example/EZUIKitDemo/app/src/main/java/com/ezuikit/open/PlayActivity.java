﻿package com.ezuikit.open;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ezvizuikit.open.EZUIError;
import com.ezvizuikit.open.EZUIKit;
import com.ezvizuikit.open.EZUIPlayer;
import com.videogo.realplay.RealPlayStatus;
import com.videogo.util.LogUtil;

/**
 * 预览界面
 */
public class PlayActivity extends Activity implements EZUIPlayer.EZUIPlayerCallBack, View.OnClickListener, WindowSizeChangeNotifier.OnWindowSizeChangedListener {
    private static final String TAG = "PlayActivity";
    public static final String APPKEY = "AppKey";
    public static final String AccessToekn = "AccessToekn";
    public static final String PLAY_URL = "play_url";

    private EZUIPlayer mEZUIPlayer;

    private Button mBtnPlay;
    /**
     * onresume时是否恢复播放
     */
    private boolean isResumePlay = false;

    private MyOrientationDetector mOrientationDetector;

    /**
     *  开发者申请的Appkey
     */
    private String appkey;
    /**
     *  授权accesstoken
     */
    private String accesstoken;
    /**
     *  播放url：ezopen协议
     */
    private String playUrl;

    /**
     * 开启预览播放
     * @param context
     * @param appkey       开发者申请的appkey
     * @param accesstoken   开发者登录授权的accesstoken
     * @param url           预览url
     */
    public static void startPlayActivity(Context context,String appkey,String accesstoken,String url){
        Intent intent = new Intent(context, PlayActivity.class);
        intent.putExtra(APPKEY,appkey);
        intent.putExtra(AccessToekn,accesstoken);
        intent.putExtra(PLAY_URL,url);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        LogUtil.d(TAG,"onCreate");
        setContentView(R.layout.activity_play);
        Intent intent = getIntent();
        appkey = intent.getStringExtra(APPKEY);
        accesstoken = intent.getStringExtra(AccessToekn);
        playUrl = intent.getStringExtra(PLAY_URL);
        if (TextUtils.isEmpty(appkey)
                || TextUtils.isEmpty(accesstoken)
                || TextUtils.isEmpty(playUrl)){
            Toast.makeText(this,"appkey,accesstoken or playUrl is null",Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mOrientationDetector = new MyOrientationDetector(this);
        new WindowSizeChangeNotifier(this, this);
        mBtnPlay = (Button) findViewById(R.id.btn_play);

        //获取EZUIPlayer实例
        mEZUIPlayer = (EZUIPlayer) findViewById(R.id.player_ui);
        //设置加载需要显示的view
        mEZUIPlayer.setLoadingView(initProgressBar());

        mBtnPlay.setOnClickListener(this);
        mBtnPlay.setText("停止");
        preparePlay();
        setSurfaceSize();
    }

    /**
     * 创建加载view
     * @return
     */
    private ProgressBar initProgressBar() {
        ProgressBar mProgressBar = new ProgressBar(this);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        mProgressBar.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress));
        mProgressBar.setLayoutParams(lp);
        return mProgressBar;
    }

    /**
     * 准备播放资源参数
     */
    private void preparePlay(){
        //设置debug模式，输出log信息
        EZUIKit.setDebug(true);
        //appkey初始化
        EZUIKit.initWithAppKey(this.getApplication(),appkey);


        //设置授权accesstoken
        EZUIKit.setAccessToken(accesstoken);
        //设置播放资源参数
        mEZUIPlayer.setPlayParams(playUrl,this);
        //播放
        mEZUIPlayer.startPlay();


    }
    @Override
    protected void onResume() {
        super.onResume();
        mOrientationDetector.enable();
        Log.d(TAG,"onResume");
        //界面stop时，如果在播放，那isResumePlay标志位置为true，resume时恢复播放
        if (isResumePlay) {
            isResumePlay = false;
            mBtnPlay.setText("停止");
            mEZUIPlayer.startPlay();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientationDetector.disable();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop + "+mEZUIPlayer.getStatus());
        //界面stop时，如果在播放，那isResumePlay标志位置为true，以便resume时恢复播放
        if (mEZUIPlayer.getStatus() != RealPlayStatus.STATUS_STOP) {
            isResumePlay = true;
        }
        //停止播放
        mEZUIPlayer.stopPlay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");

        //释放资源
        mEZUIPlayer.releasePlayer();
    }

    @Override
    public void onPlaySuccess() {
        // TODO: 2017/2/7 播放成功处理
    }

    @Override
    public void onPlayFail(EZUIError error) {
        // TODO: 2017/2/21 播放失败处理
        if (error.getErrorString().equals(EZUIError.UE_ERROR_INNER_VERIFYCODE_ERROR)){

        }
    }

    @Override
    public void onVideoSizeChange(int width, int height) {
        // TODO: 2017/2/16 播放视频分辨率回调
        Log.d(TAG,"onVideoSizeChange  width = "+width+"   height = "+height);
    }


    @Override
    public void onClick(View view) {
        if (view == mBtnPlay){
            // TODO: 2017/2/14
            if (mEZUIPlayer.getStatus() == RealPlayStatus.STATUS_PLAY) {
                //播放状态，点击停止播放
                mBtnPlay.setText("播放");
                mEZUIPlayer.stopPlay();
            } else if (mEZUIPlayer.getStatus() == RealPlayStatus.STATUS_STOP) {
                //停止状态，点击播放
                mBtnPlay.setText("停止");
                mEZUIPlayer.startPlay();
            }
        }
    }


    /**
     * 屏幕旋转时调用此方法
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG,"onConfigurationChanged");
        setSurfaceSize();
    }

    private void setSurfaceSize(){
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        boolean isWideScrren = mOrientationDetector.isWideScrren();
        //竖屏
        if (!isWideScrren) {
            //竖屏调整播放区域大小，宽全屏，高根据视频分辨率自适应
            mEZUIPlayer.setSurfaceSize(dm.widthPixels,0);
        } else {
            //横屏屏调整播放区域大小，宽、高均全屏，播放区域根据视频分辨率自适应
            mEZUIPlayer.setSurfaceSize(dm.widthPixels,dm.heightPixels);
        }
    }

    @Override
    public void onWindowSizeChanged(int w, int h, int oldW, int oldH) {
        if (mEZUIPlayer != null) {
            setSurfaceSize();
        }
    }

    public class MyOrientationDetector extends OrientationEventListener {

        private WindowManager mWindowManager;
        private int mLastOrientation = 0;

        public MyOrientationDetector(Context context) {
            super(context);
            mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }

        public boolean isWideScrren() {
            Display display = mWindowManager.getDefaultDisplay();
            Point pt = new Point();
            display.getSize(pt);
            return pt.x > pt.y;
        }
        @Override
        public void onOrientationChanged(int orientation) {
            int value = getCurentOrientationEx(orientation);
            if (value != mLastOrientation) {
                mLastOrientation = value;
                int current = getRequestedOrientation();
                if (current == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        || current == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                }
            }
        }

        private int getCurentOrientationEx(int orientation) {
            int value = 0;
            if (orientation >= 315 || orientation < 45) {
                // 0度
                value = 0;
                return value;
            }
            if (orientation >= 45 && orientation < 135) {
                // 90度
                value = 90;
                return value;
            }
            if (orientation >= 135 && orientation < 225) {
                // 180度
                value = 180;
                return value;
            }
            if (orientation >= 225 && orientation < 315) {
                // 270度
                value = 270;
                return value;
            }
            return value;
        }
    }
}