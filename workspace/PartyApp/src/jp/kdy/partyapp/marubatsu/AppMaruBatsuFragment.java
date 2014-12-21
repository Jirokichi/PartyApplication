package jp.kdy.partyapp.marubatsu;

import java.io.IOException;

import jp.kdy.bluetooth.BlueToothMessageResultReceiver;
import jp.kdy.bluetooth.InterChangeTask;
import jp.kdy.bluetooth.InterChangeTask.BlueToothResult;
import jp.kdy.partyapp.BlueToothBaseApplication;
import jp.kdy.partyapp.KYUtils;
import jp.kdy.partyapp.R;
import jp.kdy.util.MyFragmentDialog;
import android.app.Application;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Config;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class AppMaruBatsuFragment extends Fragment implements BlueToothMessageResultReceiver, MyTouchListener, OnClickListener {

	private static final String TAG = "AppMaruBatsuFragment";
	// DEBUG時はBluetoothの通信を行わず、自分で○×両方をうつ

	BlueToothBaseApplication mApp;
	BluetoothSocket mSocket;

	/**
	 *  盤面の状況を管理するためのインスタンス(ここが大きくなる場合はクラスにまとめるべき)
	 */
	private final int ROW = 3;//行数
	private final int LINE = 3;//列数
	private StatusOfRecord mGameScreen[][] = { { StatusOfRecord.Empty, StatusOfRecord.Empty, StatusOfRecord.Empty }, { StatusOfRecord.Empty, StatusOfRecord.Empty, StatusOfRecord.Empty },
			{ StatusOfRecord.Empty, StatusOfRecord.Empty, StatusOfRecord.Empty } };
	private MyType mType; //  マルかバツかのタイプ
	private boolean hasPermissionToSet = true;// マルかバツをセットする権限
	private void resetGameScreen(){
		for (int i = 0; i < this.mGameScreen.length; i++) {
			for (int j = 0; j < this.mGameScreen[i].length; j++) {
				mGameScreen[i][j] = StatusOfRecord.Empty;
			}
		}
	}
	

	/**
	 * ビュー関係
	 */
	// 盤面 
	MaruBatsuView mView = null;
	// 盤面上部に表示されるどちらのターンかのテキスト
	TextView mMyKindsTextView = null;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = (ViewGroup) inflater.inflate(R.layout.fragment_app_marubatsu, container);
		
		log("onCreate");
		Application app = this.getActivity().getApplication();
		if(app instanceof BlueToothBaseApplication){
			mApp = (BlueToothBaseApplication) this.getActivity().getApplication();	
		}else{
			log("This fragment is only for BlueToothBaseApplication.");
			return view;
		}
		
		mSocket = mApp.mSocket;
		mView = (MaruBatsuView) view.findViewById(R.id.maruBatsuView);
		mView.setMyTouchListener(this);
		
		mMyKindsTextView = (TextView)view.findViewById(R.id.textViewMaruBatsu);

		if (KYUtils.DEBUG) {
			mType = MyType.Maru;
			hasPermissionToSet = true;
			setmMyKindsTextView();
			return view;
		}

		if (mSocket == null) {
			log("Socketなし");
			return view;
		}

		if (!mApp.parentPlayer) {
			mType = MyType.Maru;
			hasPermissionToSet = false;
			setmMyKindsTextView();
			InterChangeTask ict = new InterChangeTask(mSocket, false, null);
			ict.setBlueToothReceiver(this);
			ict.execute(new Object[] { "Wait=" + mSocket + ")" });
		} else {
			mType = MyType.Batsu;
			hasPermissionToSet = true;
			setmMyKindsTextView();
		}

		return view;
	}

	@Override
	public void didBlueToothMessageResultReceiver(BlueToothResult result) {
		log("didBlueToothResultReceiver:" + result);
		TextView tv = null;
		switch (result.type) {
		case SendSuccess:
			Toast.makeText(this.getActivity(), result.toString(), Toast.LENGTH_LONG).show();
			if (mSocket != null) {
				// 接続完了時の処理
				InterChangeTask ict = new InterChangeTask(mSocket, false, null);
				ict.setBlueToothReceiver(this);
				ict.execute(new Object[] { "X Wait=" + mSocket + ")" });
			}
			break;
		case ReceiveSuccess:
			Toast.makeText(this.getActivity(), result.toString(), Toast.LENGTH_LONG).show();

			hasPermissionToSet = true;
			int num = Integer.parseInt(result.resultMessage);
			int i = num / 10;
			int j = num % 10;
			log(String.format("setMaruOrBatsu(%d,%d)", i, j));
			setMaruOrBatsu(i, j, enemyType());

			// すべて埋まっているかのチェックを実施し、その後勝敗を決定する
			if (judgeWinner(enemyType())) {
				log("Loser");

				MyFragmentDialog dialog = MyFragmentDialog.newInstanceForNormalDilog("勝敗", "あなたの負けです。対戦を続ける場合は「はい」を押してください");
				dialog.setDialogListener(this);
				dialog.show(this.getFragmentManager(), "MyFragmentDialog");
				hasPermissionToSet = false;
				if(mMyKindsTextView != null)
					mMyKindsTextView.setText("終了！");
			}else{
				setmMyKindsTextView();
			}
			setmMyKindsTextView();

			break;
		case Exception:
			Toast.makeText(this.getActivity(), result.toString(), Toast.LENGTH_LONG).show();
			hasPermissionToSet = false;
			if(mMyKindsTextView != null)
				mMyKindsTextView.setText("異常終了しました");
			break;
		case Cancel:
			Toast.makeText(this.getActivity(), result.toString(), Toast.LENGTH_LONG).show();
			tv = (TextView) this.getView().findViewById(R.id.chatBoard);
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

		if (!KYUtils.DEBUG) {
			hasPermissionToSet = false;
			InterChangeTask ict = new InterChangeTask(mSocket, true, "" + i + j);
			ict.setBlueToothReceiver(this);
			ict.execute(new Object[] { "Send=" + mSocket + ")" });
		}
		// すべて埋まっているかのチェックを実施し、その後勝敗を決定する
		if (judgeWinner(mType)) {
			log("Winner");
			MyFragmentDialog dialog = MyFragmentDialog.newInstanceForNormalDilog("勝敗", "あなたの勝ちです。対戦を続ける場合は「はい」を押してください。");
			dialog.setDialogListener(this);
			dialog.show(getFragmentManager(), "MyFragmentDialog");
		}else{
			setmMyKindsTextView();
		}
		
		if (KYUtils.DEBUG) {
			mType = this.enemyType();
			hasPermissionToSet = true;
			setmMyKindsTextView();
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
		} else {
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
		} else if (mGameScreen[0][2] == kind && mGameScreen[1][1] == kind && mGameScreen[2][0] == kind) {
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
	
	private void setmMyKindsTextView(){
		if(mMyKindsTextView != null){
			String tmp = null;
			if(hasPermissionToSet){
				if(this.mType == MyType.Maru){
					tmp = "○";
				}else{
					tmp = "×";
				}
				mMyKindsTextView.setText(String.format("自分(%s)のターン",tmp));
			}else{
				if(this.enemyType() == MyType.Maru){
					tmp = "○";
				}else{
					tmp = "×";
				}
				mMyKindsTextView.setText(String.format("相手(%s)のターン",tmp));
			}
		}
	}

	/**
	 * 勝敗決定後のダイアログでボタンを押した際のイベント
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		log("onClick:" + dialog);
		switch (which){
		case DialogInterface.BUTTON_POSITIVE:
			// 対戦を続ける場合
			
			
			break;
		case DialogInterface.BUTTON_NEGATIVE:
			// 対戦を終了する場合
			// Socketを閉じる
			if(!KYUtils.DEBUG){
				if (mSocket != null) {
					try {
						mSocket.close();
						mSocket = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			this.getActivity().finish();
			break;
		default:
				break;
		}
	}
	
	private void log(String message) {
		Log.d(TAG, message);
	}

}