package com.chinamobile.sn.secaid;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SecAidActivity extends Activity {

	// 来电号码
	private EditText fromNoText = null;
	// 信任列表
	private EditText allowListText = null;
	private String[] allowListArray = null;
	// 回复内容
	private EditText feedbackText = null;
	private String feedback = null;
	// 运行日志
	private EditText runLogText = null;
	// 工作状态
	private TextView workStatusText = null;

	// 按钮
	private Button startButton = null;
	private Button stopButton = null;

	private BroadcastReceiver smsReciver = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sec_aid);

		fromNoText = (EditText) findViewById(R.id.fromNo);
		fromNoText.setText("10658379");
		//fromNoText.setText("+8615102980");

		allowListText = (EditText) findViewById(R.id.allowList);
		allowListText.setText(" chengying  ");

		feedbackText = (EditText) findViewById(R.id.feedback);
		feedbackText.setText("１");

		runLogText = (EditText) findViewById(R.id.runLog);
		runLogText.setText("");

		workStatusText = (TextView) findViewById(R.id.workStatus);
		workStatusText.setText("  暂停中");

		// 按钮
		startButton = (Button) this.findViewById(R.id.startBtn);
		stopButton = (Button) this.findViewById(R.id.stopBtn);
		stopButton.setEnabled(false);

		// 注册 Start 按钮 单击事件
		startButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				// TODO 检查输入框是否填写了正确的值

				allowListArray = allowListText.getText().toString().split(" ");
				feedback = feedbackText.getText().toString();

				workStatusText.setText("  值班中");
				startButton.setEnabled(false);
				stopButton.setEnabled(true);

				// runLogText.append("Register Moniter SMS.");

				// 注册sms监听

				smsReciver = new SmsReceiver();
				IntentFilter filter = new IntentFilter(
						"android.provider.Telephony.SMS_RECEIVED");
				filter.setPriority(999);
				registerReceiver(smsReciver, filter);

				runLogText.append("\n开始监听短信。");
			}
		});

		// 注册 Stop 按钮 单击事件
		stopButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				workStatusText.setText("  暂停中");
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
				runLogText.setText("");
				// 取消sms监听
				try {
					unregisterReceiver(smsReciver);

				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
				runLogText.append("\n已经停止监听短信。");
			}
		});
	}

	public class SmsReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			SmsMessage[] msgs = null;
			String msgFromNo = null;
			StringBuffer msgStrBuf = new StringBuffer();
			String msgBody = null;

			// runLogText.append("Start onReceive." );

			if (bundle != null) {
				// 接收短信
				Object[] pdus = (Object[]) bundle.get("pdus");
				msgs = new SmsMessage[pdus.length];
				for (int i = 0; i < msgs.length; i++) {
					msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
					msgFromNo = msgs[i].getOriginatingAddress();
					msgStrBuf.append(msgs[i].getMessageBody());
				}
				// ---display the new SMS message---
				// Toast.makeText(context, msgBody, Toast.LENGTH_SHORT).show();
			}
			msgBody = msgStrBuf.toString();
			runLogText.append("\n----------------\n收到短信: " + msgBody);

			String caredNo = fromNoText.getText().toString();

			runLogText.append("\n短信发送方号码: " + msgFromNo);
			// runLogText.append("\n监控号码: " + caredNo );
			boolean flag = false;
			if (msgFromNo.indexOf(caredNo) == 0) {
				// 确实是关注的号码发过来的短信，则开始检查短信内容。
				runLogText.append("\n发送方号码符合设定，检查短信内容。");
				for (int i = 0; i < allowListArray.length; i++) {
					if (msgBody.indexOf(allowListArray[i].trim()) > 0) {
						flag = true;
						break;
					}
				}
				if (flag) {
					runLogText.append("\n短信内容符合设定，自动回复短信。");
					String strRunLog = sendMsg(msgFromNo, feedback);
					runLogText.append("\n" + strRunLog);
				} else {
					runLogText.append("\n短信内容不匹配，忽略。");
				}
			} else {
				// 无关短信，忽略，记Log
				runLogText.append("\n发送方号码不匹配，忽略。");
			}
		}
	}

	@Override
	public void onStop() {
		//runLogText.append("\n onStop()");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		//runLogText.append("\n onDestroy()");
		try {
			unregisterReceiver(smsReciver);

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_sec_aid, menu);
		return true;
	}

	private String sendMsg(String mobileNo, String content) {
		String strlog = "";
		// 移动运营商允许每次发送的字节数据有限，我们可以使用Android给我们提供 的短信工具。
		if (content != null) {
			SmsManager sms = SmsManager.getDefault();
			// 如果短信超过限制长度，则返回一个长度的List。
			List<String> texts = sms.divideMessage(content);
			for (String text : texts) {
				sms.sendTextMessage(mobileNo, null, text, null, null);
				strlog = "\n发送短信 : " + content + " 到 " + mobileNo;
				Log.i("debug", strlog);
			}
		}
		return strlog;
	}

}
