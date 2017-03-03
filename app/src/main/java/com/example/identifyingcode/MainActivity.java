package com.example.identifyingcode;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.view.View.OnClickListener;
import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import android.widget.FrameLayout.LayoutParams;

public class MainActivity  extends Activity implements OnClickListener{

    private Button but1;//声明发送短信按钮
    private Button but2;//声明验证按钮
    private EditText phoneNum;//手机号输入框
    private EditText code;//验证码输入框
    private ProgressBar mProBar;
    int i = 30;  // 声明 int i= 30 后面用于验证码倒计时
    private EventHandler eventHandler = new MyHandler(); //实例化MyHandler类

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SMSSDK.initSDK(MainActivity.this, "1bc7231ea7737", "8209de887fb3ae4d0f3c90660503fadc");//sms短信验证码sdk
        SMSSDK.registerEventHandler(eventHandler);//方法回调
        findView();//调用findview  方法绑定控件
    }

    private void findView(){
        but1=(Button) findViewById(R.id.but1);
        but2=(Button) findViewById(R.id.but2);
        phoneNum=(EditText) findViewById(R.id.phoneNum);
        code=(EditText) findViewById(R.id.code);
        but1.setOnClickListener(this);//给下一步按钮设置监听
        but2.setOnClickListener(this);//给获取验证码按钮设置监听
        mProBar = new ProgressBar(this);
    }


    //回调函数
    private class MyHandler extends EventHandler {
        @Override
        public void onRegister() {
            super.onRegister();
        }

        @Override
        public void beforeEvent(int event, Object data) {
            super.beforeEvent(event, data);
        }
        @Override
        public void afterEvent(int event, int result, Object data) {
            //线程阻断 等待1秒 让等待效果出现
            try {
                Thread.currentThread().sleep(1000);//阻断1秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Message msg = new Message();
            msg.arg1 = event;
            msg.arg2 = result;
            msg.obj = data;
            handler.sendMessage(msg);
            super.afterEvent(event, result, data);
        }
        @Override
        public void onUnregister() {
            super.onUnregister();
        }
    }


    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == -9) {
                but1.setText("重新发送(" + i + ")");
            } else if (msg.what == -8) {
                but1.setText("获取验证码");
                but1.setClickable(true);
                i = 30;
            } else {
                int event = msg.arg1;
                int result = msg.arg2;
                Object data = msg.obj;
                mProBar.setVisibility(View.GONE); //隐藏等待效果
                if (result == SMSSDK.RESULT_COMPLETE) {
                    // 短信注册成功后，返回下一个界面,然后提示
                    if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {// 提交验证码成功
                        Toast.makeText(getApplicationContext(), "验证成功", Toast.LENGTH_SHORT).show();//消息提示
                       // finish();//设置无法返回本界面
                    } else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) {
                        Toast.makeText(getApplicationContext(), "验证码已经发送", Toast.LENGTH_SHORT).show();
                    } else {
                        ((Throwable) data).printStackTrace();
                    }
                }else{
                    Toast.makeText(MainActivity.this, "验证码错误", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

   //监听事件
    public void onClick(View v) {
        String phoneNums = phoneNum.getText().toString();
        switch (v.getId()) {
            case R.id.but1:

                // 1. 通过规则判断手机号
                if (!judgePhoneNums(phoneNums)) {
                    return;
                }
                // 2. 通过sdk发送短信验证
                SMSSDK.getVerificationCode("86", phoneNums);
                // 3. 把按钮变成不可点击，并且显示倒计时（正在获取）
                createProgressBar();
               but1.setClickable(false);

                but1.setText("重新发送(" + i + ")");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (; i > 0; i--) {
                            handler.sendEmptyMessage(-9);
                            if (i <= 0) {
                                break;
                            }
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        handler.sendEmptyMessage(-8);
                    }
                }).start();
                break;

            case R.id.but2:
                // 1. 通过规则判断手机号
                if (!judgePhoneNums(phoneNums)) {
                    return;
                }
                SMSSDK.submitVerificationCode("86", phoneNums,code.getText().toString());
                createProgressBar();
                break;
        }
    }

    //定义等待动画
    private void createProgressBar() {
        FrameLayout layout = (FrameLayout) findViewById(android.R.id.content);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;

        mProBar.setLayoutParams(layoutParams);
        mProBar.setVisibility(View.VISIBLE);
        layout.addView(mProBar);
    }



     //判断手机号码是否合理
    private boolean judgePhoneNums(String phoneNums) {
        if (isMatchLength(phoneNums, 11)
                && isMobileNO(phoneNums)) {
            return true;
        }
        Toast.makeText(this, "手机号码输入有误！",Toast.LENGTH_SHORT).show();
        return false;
    }

    //验证手机格式
    public static boolean isMobileNO(String mobileNums) {
        /*
         * 移动：134、135、136、137、138、139、150、151、157(TD)、158、159、187、188
         * 联通：130、131、132、152、155、156、185、186 电信：133、153、180、189、（1349卫通）
         * 总结起来就是第一位必定为1，第二位必定为3或5或8，其他位置的可以为0-9
         */
        String telRegex = "[1][358]\\d{9}";// "[1]"代表第1位为数字1，"[358]"代表第二位可以为3、5、8中的一个，"\\d{9}"代表后面是可以是0～9的数字，有9位。
        if (TextUtils.isEmpty(mobileNums))
            return false;
        else
            return mobileNums.matches(telRegex);
    }

    //判断一个字符串的位数
    public static boolean isMatchLength(String str, int length) {
        if (str.isEmpty()) {
            return false;
        } else {
            return str.length() == length ? true : false;
        }
    }

}
