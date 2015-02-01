package jp.kdy.bluetooth.communication;

import static jp.kdy.partyapp.KYUtils.log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import jp.kdy.bluetooth.BlueToothMessageResultReceiver;
import android.bluetooth.BluetoothSocket;

public class CommunicationReceiverTask extends CommunicationTask {
	
	private static InputStream in = null;
	public CommunicationReceiverTask(BluetoothSocket socket, BlueToothMessageResultReceiver brr) {
		super(socket, brr);
		try {
			// 接続済みソケットからI/Oストリームを取得
			in = socket.getInputStream();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	protected BlueToothResult doInBackgroundCommunication() {
		BlueToothResult result = new BlueToothResult();
		byte[] buf = new byte[100];
		int len = 0;
		try {
			log("in.read");
			if(in != null){
				len = in.read(buf);
			}
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
	protected String onTaskCancel() {
		String result = null;
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				result = e.getMessage();
			}
		}
		return result;
	}

}
