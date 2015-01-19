package jp.kdy.bluetooth.communication;

import static jp.kdy.partyapp.KYUtils.log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import jp.kdy.bluetooth.BlueToothMessageResultReceiver;
import android.bluetooth.BluetoothSocket;

public class CommunicationSenderTask extends CommunicationTask {


	private static OutputStream out = null;
	private String sendMessage = null;
	private final String SEND_SUCCESS_MESSAGE = "Send_Success";
	
	public CommunicationSenderTask(BluetoothSocket socket, BlueToothMessageResultReceiver brr, String sendMessage) {
		super(socket, brr);
		this.sendMessage = sendMessage;
		
		try {
			// 接続済みソケットからI/Oストリームを取得
			out = socket.getOutputStream();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
	}

	@Override
	protected BlueToothResult doInBackgroundCommunication() {
		
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
		
		result.resultMessage = SEND_SUCCESS_MESSAGE;
		result.type = ResultType.SendSuccess;
		return result;
	}

	@Override
	protected String onTaskCancel() {
		String result = null;
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				result = e.getMessage();
			}
		}
		return result;
	}
	
	private void write(byte[] buf) {
		// Outputストリームへのデータ書き込み
		try {
			out.write(buf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
