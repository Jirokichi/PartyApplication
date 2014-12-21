package jp.kdy.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;

import static jp.kdy.partyapp.KYUtils.*;

public class InterChangeTask extends AsyncTask<Object, Object, InterChangeTask.BlueToothResult> {

	public static InputStream in = null;
	public static OutputStream out = null;
	private BluetoothSocket mSocket;
	private BlueToothMessageResultReceiver mReceiver;
	private boolean isSender = false;

	private String sendMessage = null;

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

	/***
	 * コンストラクタの定義
	 * 
	 * @param context
	 * @param socket
	 * @param isSender
	 *            送信する場合
	 */
	public InterChangeTask(BluetoothSocket socket, boolean isSender, String sendMessage) {
		this.isSender = isSender;
		this.sendMessage = sendMessage;
		
		mSocket = socket;
		try {
			// 接続済みソケットからI/Oストリームを取得
			out = socket.getOutputStream();
			in = socket.getInputStream();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void setBlueToothReceiver(BlueToothMessageResultReceiver brr) {
		this.mReceiver = brr;
	}

	public void write(byte[] buf) {
		// Outputストリームへのデータ書き込み
		try {
			out.write(buf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected BlueToothResult doInBackground(Object... params) {
		// TODO Auto-generated method stub
		log("doInBackground:" + params);

		BlueToothResult result = null;
		// こちらから情報を送信する場合
		if(mSocket == null /*|| !mSocket.isConnected()*/){
			result = new BlueToothResult();
			result.type = ResultType.Exception;
			result.resultMessage = "Socket is invalie";
		}
		else if (isSender) {
			result = doInBackgroundSend();
		}
		// 受信待機状態
		else {
			result = doInBackgroundReceive();
		}
		log("doInBackground - fin");
		return result;
	}

	private BlueToothResult doInBackgroundSend() {
		BlueToothResult result = new BlueToothResult();

		// Send
		try {
			log("writer");
			write(sendMessage.getBytes("UTF-8"));
			log("writer - end");
		} catch (UnsupportedEncodingException e) {
			result.resultMessage = e.getMessage();
			result.type = ResultType.Exception;
			return result;
		}
		
		result.resultMessage = "Send Success";
		result.type = ResultType.SendSuccess;
		return result;
	}

	private BlueToothResult doInBackgroundReceive() {
		BlueToothResult result = new BlueToothResult();
		byte[] buf = new byte[100];
		int len = 0;
		try {
			log("in.read");
			len = in.read(buf);
			log("in.read - fin");
		} catch (IOException e) {
			result.resultMessage = e.getMessage();
			result.type = ResultType.Exception;
			return result;
		}
		if (len != 0) {
			try {
				result.resultMessage = new String(buf, 0, len, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				result.resultMessage = e.getMessage();
				result.type = ResultType.Exception;
				return result;
			}
		}
		result.type = ResultType.ReceiveSuccess;
		return result;
	}
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

		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				result.resultMessage = e.getMessage();
			}
		}
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				result.resultMessage = e.getMessage();
			}
		}

		this.mReceiver.didBlueToothMessageResultReceiver(result);
	}

}
