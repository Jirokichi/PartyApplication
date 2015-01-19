package jp.kdy.bluetooth.communication;

import jp.kdy.bluetooth.BlueToothMessageResultReceiver;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;

import static jp.kdy.partyapp.KYUtils.*;

public abstract class CommunicationTask extends AsyncTask<Object, Object, CommunicationTask.BlueToothResult> {

	private BluetoothSocket mSocket = null;
	private BlueToothMessageResultReceiver mReceiver = null;

	// 結果の種類
	public enum ResultType {
		SendSuccess, ReceiveSuccess, Exception, Cancel
	};

	public class BlueToothResult {
		public String resultMessage = null;
		public ResultType type;

		@Override
		public String toString() {
			String message = null;
			message = "Type(" + type + "):" + resultMessage;
			return message;
		}
	}

	public CommunicationTask(BluetoothSocket socket, BlueToothMessageResultReceiver brr) {
		mSocket = socket;
		mReceiver = brr;
	}

	@Override
	protected BlueToothResult doInBackground(Object... params) {
		log("doInBackground:" + params);
		BlueToothResult result = null;

		// こちらから情報を送信する場合
		if (mSocket == null /* || !mSocket.isConnected() */) {
			result = new BlueToothResult();
			result.type = ResultType.Exception;
			result.resultMessage = "Socket is invalie";
		}else{
			result = doInBackgroundCommunication();
		}
		log("doInBackground - fin");
		return result;
		
	}
	
	abstract protected BlueToothResult doInBackgroundCommunication(); 
	
	@Override
	protected void onPostExecute(BlueToothResult result) {
		if (this.isCancelled()) {
			return;
		}
		
		// ここで結果の記述をする
		this.mReceiver.didBlueToothMessageResultReceiver(result);
	}
	
	/**
	 * task.cancel(true)されたときに呼び出されるメソッド
	 */
	@Override
	public void onCancelled() {
		log("onCancelled()");
		BlueToothResult result = new BlueToothResult();
		result.resultMessage = "Cancel";
		result.type = ResultType.Cancel;
		String tmp = onTaskCancel();
		if(tmp != null){
			result.resultMessage = tmp;
		}
		
		this.mReceiver.didBlueToothMessageResultReceiver(result);
	}
	
	/**
	 * タスクのキャンセル時に実施しなければならない処理
	 */
	abstract protected String onTaskCancel();
}
