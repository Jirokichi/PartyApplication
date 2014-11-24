package jp.kdy.partyapp;

import jp.kdy.bluetooth.BlueToothMessageResultReceiver;
import jp.kdy.bluetooth.InterChangeTask;
import jp.kdy.bluetooth.InterChangeTask.BlueToothResult;
import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class App1Activity extends Activity implements BlueToothMessageResultReceiver {

	private static final String TAG = "App1Activity";

	BlueToothBaseApplication mApp;
	BluetoothSocket mSocket;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate");

		setContentView(R.layout.activity_app1);

		mApp = (BlueToothBaseApplication) this.getApplication();
		mSocket = mApp.mSocket;

		if (mSocket == null) {
			log("Socketなし");
			return;
		}

		if (!mApp.parentPlayer) {
			Button b = (Button) this.findViewById(R.id.chatSendButton);
			b.setEnabled(false);
			
			InterChangeTask ict = new InterChangeTask(mSocket, false, null);
			ict.setBlueToothReceiver(this);
			ict.execute(new Object[] { "Wait=" + mSocket + ")" });
		} else {
			Button b = (Button) this.findViewById(R.id.chatSendButton);
			b.setEnabled(true);
		}

	}

	public void onClickChatSendButton(View view) {
		EditText e = (EditText) this.findViewById(R.id.chatTextEdit);
		String message = e.getText().toString();
		
		Button b = (Button) this.findViewById(R.id.chatSendButton);
		b.setEnabled(false);

		TextView tv = (TextView)this.findViewById(R.id.chatBoard);
		tv.setText(tv.getText() + "¥n" + "自分:" + message);
		
		InterChangeTask ict = new InterChangeTask(mSocket, true, message);
		ict.setBlueToothReceiver(this);
		ict.execute(new Object[] { "Send=" + mSocket + ")" });
	}

	private void log(String message) {
		Log.d(TAG, message);
	}

	@Override
	public void didBlueToothMessageResultReceiver(BlueToothResult result) {
		log("didBlueToothResultReceiver:" + result);
		TextView tv = null;
		Button b = null;
		switch (result.type) {
		case SendSuccess:
			Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show();
			if (mSocket != null) {
				// 接続完了時の処理
				InterChangeTask ict = new InterChangeTask(mSocket, false, null);
				ict.setBlueToothReceiver(this);
				ict.execute(new Object[] { "X Wait=" + mSocket + ")" });
			}
			break;
		case ReceiveSuccess:
			Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show();
			
			tv = (TextView)this.findViewById(R.id.chatBoard);
			tv.setText(tv.getText() + "¥n" + "相手:" + result.resultMessage);
			b = (Button) this.findViewById(R.id.chatSendButton);
			b.setEnabled(true);
			
			break;
		case Exception:
			Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show();
			tv = (TextView)this.findViewById(R.id.chatBoard);
			tv.setText(tv.getText() + "¥n" + "Exception:" + result.resultMessage);
			b = (Button) this.findViewById(R.id.chatSendButton);
			b.setEnabled(false);
			
			
			break;
		case Cancel:
			Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show();
			tv = (TextView)this.findViewById(R.id.chatBoard);
			tv.setText(tv.getText() + "¥n" + "Cancel:" + result.resultMessage);
			break;
		default:
			break;
		}
	}
}
