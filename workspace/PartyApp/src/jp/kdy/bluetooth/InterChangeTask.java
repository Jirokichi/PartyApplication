package jp.kdy.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class InterChangeTask extends AsyncTask<Object, Object, InterChangeTask.BlueToothResult> {

	private static final String TAG = "InterChangeTask";

	public static InputStream in = null;
	public static OutputStream out = null;
	private BluetoothSocket mSocket;
	private BlueToothMessageResultReceiver mReceiver;
	private boolean isSender = false;

	private String sendMessage = null;

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
	 * �R���X�g���N�^�̒�`
	 * 
	 * @param context
	 * @param socket
	 * @param isSender
	 *            ���M����ꍇ
	 */
	public InterChangeTask(BluetoothSocket socket, boolean isSender, String sendMessage) {
		this.isSender = isSender;
		this.sendMessage = sendMessage;
		
		mSocket = socket;
		try {
			// �ڑ��ς݃\�P�b�g����I/O�X�g���[�����擾
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
		// Output�X�g���[���ւ̃f�[�^��������
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
		// �����炩����𑗐M����ꍇ
		if(mSocket == null || !mSocket.isConnected()){
			result = new BlueToothResult();
			result.type = ResultType.Exception;
			result.resultMessage = "Socket is invalie";
		}
		else if (isSender) {
			result = doInBackgroundSend();
		}
		// ��M�ҋ@���
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
		
		// �����Ō��ʂ̋L�q������
		this.mReceiver.didBlueToothMessageResultReceiver(result);
	}

	@Override
	public void onCancelled() {
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

	private void log(String message) {
		Log.d(TAG, message);
	}

}