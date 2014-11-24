package jp.kdy.partyapp.marubatsu;

import jp.kdy.bluetooth.BlueToothMessageResultReceiver;
import jp.kdy.bluetooth.InterChangeTask;
import jp.kdy.bluetooth.InterChangeTask.BlueToothResult;
import jp.kdy.partyapp.BlueToothBaseApplication;
import jp.kdy.partyapp.R;
import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AppMaruBatsuActivity extends Activity implements BlueToothMessageResultReceiver, MyTouchListener {

	private static final String TAG = "AppMaruBatsuActivity";

	BlueToothBaseApplication mApp;
	BluetoothSocket mSocket;

	// 盤面の状況を管理
	private final int ROW = 3;
	private final int LINE = 3;
	private StatusOfRecord mGameScreen[][] = { { StatusOfRecord.Empty, StatusOfRecord.Empty, StatusOfRecord.Empty }, { StatusOfRecord.Empty, StatusOfRecord.Empty, StatusOfRecord.Empty }, { StatusOfRecord.Empty, StatusOfRecord.Empty, StatusOfRecord.Empty } };
	private MyType mType;
	private boolean hasPermissionToSet = true;

	MaruBatsuView mView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate");

		setContentView(R.layout.activity_app_marubatsu);

		mApp = (BlueToothBaseApplication) this.getApplication();
		mSocket = mApp.mSocket;

		mView = (MaruBatsuView) this.findViewById(R.id.maruBatsuView);
		mView.setMyTouchListener(this);

		if (mSocket == null) {
			log("Socketなし");
			return;
		}

		if (!mApp.parentPlayer) {
			mType = MyType.Maru;
			hasPermissionToSet = false;
			InterChangeTask ict = new InterChangeTask(mSocket, false, null);
			ict.setBlueToothReceiver(this);
			ict.execute(new Object[] { "Wait=" + mSocket + ")" });
		} else {
			mType = MyType.Batsu;
			hasPermissionToSet = true;
		}

	}

	public void onClickChatSendButton(View view) {
		EditText e = (EditText) this.findViewById(R.id.chatTextEdit);
		String message = e.getText().toString();

		Button b = (Button) this.findViewById(R.id.chatSendButton);
		b.setEnabled(false);

		TextView tv = (TextView) this.findViewById(R.id.chatBoard);
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

			hasPermissionToSet = true;
			int num = Integer.parseInt(result.resultMessage);
			int i = num / 10;
			int j = num % 10;
			log(String.format("setMaruOrBatsu(%d,%d)", i, j));
			setMaruOrBatsu(i, j, enemyType());

			// すべて埋まっているかのチェックを実施し、その後勝敗を決定する
			if (judgeWinner(enemyType())) {
				log("Loser");
				hasPermissionToSet = false;
			}
			
			break;
		case Exception:
			Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show();
			tv = (TextView) this.findViewById(R.id.chatBoard);
			tv.setText(tv.getText() + "¥n" + "Exception:" + result.resultMessage);
			b = (Button) this.findViewById(R.id.chatSendButton);
			b.setEnabled(false);

			break;
		case Cancel:
			Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show();
			tv = (TextView) this.findViewById(R.id.chatBoard);
			tv.setText(tv.getText() + "¥n" + "Cancel:" + result.resultMessage);
			break;
		default:
			break;
		}
	}

	@Override
	public MyType checkPermission(int i, int j) {
		if (mGameScreen[i][j] == StatusOfRecord.Empty && hasPermissionToSet) {
			return mType;
		} else {
			return MyType.No_Permission;
		}
	}

	@Override
	public void startAction(int i, int j) {
		log(String.format("startAction(%d,%d)", i, j));

		setMaruOrBatsu(i, j, mType);
		hasPermissionToSet = false;
		InterChangeTask ict = new InterChangeTask(mSocket, true, "" + i + j);
		ict.setBlueToothReceiver(this);
		ict.execute(new Object[] { "Send=" + mSocket + ")" });

		// すべて埋まっているかのチェックを実施し、その後勝敗を決定する
		if (judgeWinner(mType)) {
			log("Winner");
		}
	}

	private void setMaruOrBatsu(int i, int j, MyType type) {
		if (type == MyType.Batsu) {
			mView.drawBatsu(i, j);
			mGameScreen[i][j] = StatusOfRecord.Batsu;
		} else if (type == MyType.Maru) {
			mView.drawMaru(i, j);
			mGameScreen[i][j] = StatusOfRecord.Maru;
		}
		mView.invalidate();
	}

	private MyType enemyType() {
		MyType enemy;
		if (mType == MyType.Batsu) {
			enemy = MyType.Maru;
		} else if (mType == MyType.Maru) {
			enemy = MyType.Batsu;
		} else {
			enemy = MyType.No_Permission;
		}

		return enemy;
	}

	public boolean judgeWinner(MyType type) {
		
		StatusOfRecord kind = StatusOfRecord.Maru;
		if (type == MyType.Maru) {
			kind = StatusOfRecord.Maru;
		} else if (type == MyType.Batsu) {
			kind = StatusOfRecord.Batsu;
		}else{
			return false;
		}

		// ROW一列判定
		for (int i = 0; i < ROW; i++) {
			if ((mGameScreen[i][0] == kind && mGameScreen[i][1] == kind && mGameScreen[i][2] == kind))
				return true;
		}
		
		// LINE一列判定
		for (int j = 0; j < LINE; j++) {
			if ((mGameScreen[0][j] == kind && mGameScreen[1][j] == kind && mGameScreen[2][j] == kind))
				return true;
		}
		
		// 斜め判定
		if (mGameScreen[0][0] == kind && mGameScreen[1][1] == kind && mGameScreen[2][2] == kind) {
			return true;
		}
		else if (mGameScreen[0][2] == kind && mGameScreen[1][1] == kind && mGameScreen[2][0] == kind) {
			return true;
		}

		
		// 全マス埋まっているかの判定
		boolean empty = false;
		for (int i = 0; i < this.mGameScreen.length; i++) {
			for (int j = 0; j < this.mGameScreen[i].length; j++) {
				if (mGameScreen[i][j] == StatusOfRecord.Empty) {
					empty = true;
				}
			}
		}
		if (empty) {
			// まだ続けられる
			return false;
		} else {
			// すべて埋まったためもう続けられない
			return false;
		}

	}

}
