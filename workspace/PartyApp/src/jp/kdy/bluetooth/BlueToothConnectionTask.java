package jp.kdy.bluetooth;

import java.io.IOException;
import java.util.UUID;


import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.util.Log;

//BluetoothSocket���擾���邽�߂�AsyncTask
public class BlueToothConnectionTask extends AsyncTask<Object, Integer, BluetoothSocket> implements OnCancelListener {

	private static final String TAG = "BlueToothAsyncTask";
	Context mContext;
	private boolean isClient = true;
	
	//�N���C�A���g���̏���
    private BluetoothDevice mDevice;
    
    //�T�[�o���̏���
    private BluetoothServerSocket mServSock = null;
    
    //���ʏ���(UUID�̐����Ȃ�)
    public static final UUID TECHBOOSTER_BTSAMPLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    static BluetoothAdapter myBlueToothAdapter;
    public String myNumber;
    private BlueToothConnectionResultReceiver mReceiver;
    
    ProgressDialog progressDialog;

	public BlueToothConnectionTask(Context context, BluetoothAdapter btAdapter, boolean isClient, BluetoothDevice device, BlueToothConnectionResultReceiver receiver) {
		Log.d(TAG, "BlueToothAsyncTask()");
		this.mContext = context;
		mDevice = device;
		myBlueToothAdapter = btAdapter;
		this.isClient = isClient;
		this.mReceiver = receiver;
	}

	@Override
	protected void onPreExecute() {
		// This method is in UI Thread
		Log.d(TAG, "onPreExecute");
		
//		if(!isClient){
			progressDialog = new ProgressDialog(mContext);
			progressDialog.setTitle("Please wait");
			progressDialog.setMessage("Loading data...");
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setCancelable(true);
			progressDialog.setOnCancelListener(this);
			progressDialog.show();
//		}
		
		
	}

	@Override
	protected BluetoothSocket doInBackground(Object... params) {
		// This method is not in UI Thread
		BluetoothSocket bSocket;
		Log.d(TAG, "doInBackground:isClient? -- " + isClient);
		if(isClient){
			bSocket = doInBackgroundClient();
		}else{
			bSocket = doInBackgroundServer();
		}
		return bSocket;
	}
	
	private BluetoothSocket doInBackgroundClient(){
		Log.d(TAG, "doInBackgroundClient start");
		BluetoothSocket tmpSock = null;
		try{
            //���f�o�C�X��Bluetooth�N���C�A���g�\�P�b�g�̎擾
			Log.d(TAG, "start getting client socket");
            tmpSock = mDevice.createRfcommSocketToServiceRecord(TECHBOOSTER_BTSAMPLE_UUID);
            //�T�[�o�[���ɐڑ��v��
			Log.d(TAG, "start connecting server");
            tmpSock.connect();
			Log.d(TAG, "end connecting server - success");
		}catch(IOException e){
            e.printStackTrace();
            if(tmpSock !=null){
				try {
					tmpSock.close();
					tmpSock = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
            }
        }
		Log.d(TAG, "doInBackgroundClient end - " + tmpSock);
        return tmpSock;
	}

	private BluetoothSocket doInBackgroundServer(){
		Log.d(TAG, "doInBackgroundServer start");
		BluetoothSocket tmpSock = null;
		mServSock = null;
		try{
            //���f�o�C�X��Bluetooth�T�[�o�[�\�P�b�g�̎擾
			Log.d(TAG, "start getting server socket:"+myBlueToothAdapter.getState());
			mServSock = myBlueToothAdapter.listenUsingRfcommWithServiceRecord("BlueToothSample03", TECHBOOSTER_BTSAMPLE_UUID);
			Log.d(TAG, "mServSock:"+mServSock);
			
			//�N���C�A���g������̐ڑ��v���҂��B�\�P�b�g���Ԃ����B
 			Log.d(TAG, "accepting socket from client...");
            tmpSock = mServSock.accept();
  			Log.d(TAG, "accepted");
		}catch(IOException e){
            e.printStackTrace();
        }finally{
            /*
             *  BluetoothSocket������ۂɁABluetoothServerSocket�͕����Ȃ����߁A�����ŕ��Ă����Ȃ���΂Ȃ�Ȃ�
             *  �Ȃ��A�L�����Z���ŕ����ꍇ�͂��̏����͎��{����Ȃ�
             */
            try {
				if(mServSock != null)
					mServSock.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

		Log.d(TAG, "doInBackgroundServer end - " + tmpSock);
		return tmpSock;
	}
	@Override
	protected void onProgressUpdate(Integer... values) {
		Log.d(TAG, "onProgressUpdate");
	}

	@Override
	protected void onCancelled() {
		Log.d(TAG, "onCancelled");
		mReceiver.didBlueToothConnectionResultReceiver(null, isClient, true);
	}

	@Override
	protected void onPostExecute(BluetoothSocket result) {
		Log.d(TAG, "onPostExecute - " + result);
		
		progressDialog.dismiss();
		mReceiver.didBlueToothConnectionResultReceiver(result, isClient, false);
	}
	
	/**
	 * �v���O���X�_�C�A���O�̃L�����Z��
	 */
	@Override
	public void onCancel(DialogInterface dialog) {
		// TODO Auto-generated method stub
		if(!isClient){
			try {
				if(mServSock != null){
					mServSock.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				mServSock = null;
			}
		}
		this.cancel(true);
	}
}
