package jp.kdy.util;

import jp.kdy.partyapp.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.Toast;

// 参考サイト:http://d.hatena.ne.jp/sakura_bird1/20130207/1360193574
public class MyFragmentDialog extends DialogFragment {
	protected static final String TAG = "MyFragmentDialog";
	private DialogInterface.OnClickListener listener = null;

	// ダイアログタイプ用パラメー保存キー
	private static String bundle_key_dialog_type = "DIALOG_TYPE";

	// ツイート用パラメー保存キー
	private static String bundle_key_title = "TITLE";
	private static String bundle_key_message = "MESSAGE";

	// ダイアログタイプ
	public enum TYPE {
		JUST_CONFIRMATION_DIALOG, NORMAL_DIALOG, LIST_DIALOG
	};

	/**
	 * 確認用のダイアログを作成する。作成されるダイアログはボタンを一つした持たず、onNegativeClickのみ利用する。
	 * 
	 * @param title
	 *            　タイトル
	 * @param diaogMessage
	 *            　メッセージ
	 * @return
	 */
	public static MyFragmentDialog newInstanceForJustConfirmationDilog(String title, String diaogMessage) {
		MyFragmentDialog frag = new MyFragmentDialog();
		Bundle bundle = new Bundle();
		bundle.putString(bundle_key_dialog_type, TYPE.JUST_CONFIRMATION_DIALOG.name());
		bundle.putString(bundle_key_title, title);
		bundle.putString(bundle_key_message, diaogMessage);
		frag.setArguments(bundle);
		return frag;
	}

	/**
	 * ダイアログを作成する。作成されるダイアログはボタンを2つもつ
	 * 
	 * @param title
	 *            　タイトル
	 * @param diaogMessage
	 *            　メッセージ
	 * @return
	 */
	public static MyFragmentDialog newInstanceForNormalDilog(String title, String diaogMessage) {
		MyFragmentDialog frag = new MyFragmentDialog();
		Bundle bundle = new Bundle();
		bundle.putString(bundle_key_dialog_type, TYPE.NORMAL_DIALOG.name());
		bundle.putString(bundle_key_title, title);
		bundle.putString(bundle_key_message, diaogMessage);
		frag.setArguments(bundle);
		return frag;
	}

	/**
	 * リストがあるダイアログを作成する。
	 * 
	 * @param title
	 *            　タイトル
	 * @param diaogMessage
	 *            　メッセージ
	 * @return
	 */
	public static MyFragmentDialog newInstanceForListDilog(String title, String diaogMessage) {
		MyFragmentDialog frag = new MyFragmentDialog();
		Bundle bundle = new Bundle();
		bundle.putString(bundle_key_dialog_type, TYPE.LIST_DIALOG.name());
		bundle.putString(bundle_key_title, title);
		bundle.putString(bundle_key_message, diaogMessage);
		frag.setArguments(bundle);
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		TYPE dialogType = TYPE.valueOf(getArguments().getString(bundle_key_dialog_type));
		switch (dialogType) {
		case NORMAL_DIALOG:
			return createNormalDialog(savedInstanceState);
		case JUST_CONFIRMATION_DIALOG:
			return createConfirmDialog(savedInstanceState);
		case LIST_DIALOG:
			return createListDialog(savedInstanceState);
		default:
			return null;
		}
	}

	// ボタン２つのダイアログ
	private Dialog createNormalDialog(Bundle savedInstanceState) {
		String title = getArguments().getString(bundle_key_title);
		String dialogMessage = getArguments().getString(bundle_key_message);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title);
		builder.setMessage(dialogMessage);
		if (this.listener == null) {
			log("Not Set this listner yet.");
		}
		builder.setPositiveButton(getString(R.string.Yes), this.listener);
		builder.setNegativeButton(getString(R.string.No), this.listener);
		return builder.create();
	}

	// ボタンが一つだけ
	private Dialog createConfirmDialog(Bundle savedInstanceState) {
		String title = getArguments().getString(bundle_key_title);
		String dialogMessage = getArguments().getString(bundle_key_message);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title);
		builder.setMessage(dialogMessage);
		if (this.listener == null) {
			log("Not Set this listner yet.");
		}
		builder.setPositiveButton(getString(R.string.Yes), this.listener);
		return builder.create();
	}

	// ボタンが一つだけ
	private Dialog createListDialog(Bundle savedInstanceState) {
		String title = getArguments().getString(bundle_key_title);
		String dialogMessage = getArguments().getString(bundle_key_message);

		CharSequence[] items = {"使い方", "よくある質問", "メール", "閉じる"};
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title);
		//builder.setMessage(dialogMessage);
		if (this.listener == null) {
			log("Not Set this listner yet.");
		}
		final Activity activity = getActivity();
		builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Toast.makeText(activity, "使い方が押された", Toast.LENGTH_LONG).show();
                        break;
                    case 1:
                        Toast.makeText(activity, "よくある質問が押された", Toast.LENGTH_LONG).show();
                        break;
                    case 2:
                        Toast.makeText(activity, "メールが押された", Toast.LENGTH_LONG).show();
                        break;
                    case 3:
                        Toast.makeText(activity, "閉じる", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        break;
                }
            }
        });
//		builder.setPositiveButton(getString(R.string.Yes), this.listener);
		return builder.create();
	}

	/**
	 * リスナーを追加する
	 * 
	 * @param listener
	 */
	public void setDialogListener(DialogInterface.OnClickListener listener) {
		this.listener = listener;
	}

	/**
	 * リスナーを削除する
	 */
	public void removeDialogListener() {
		this.listener = null;
	}

	private void log(String message) {
		Log.d(TAG, message);
	}
}