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

	// ダイアログタイプ用パラメー保存キー
	private static String bundle_key_dialog_type = "DIALOG_TYPE";

	// ツイート用パラメー保存キー
	private static String bundle_key_title = "TITLE";
	private static String bundle_key_message = "MESSAGE";

	// ダイアログタイプ
	public enum TYPE {
		JUST_CONFIRMATION_DIALOG, NORMAL_DIALOG, LIST_DIALOG
	};

	// ダイアログのyesボタン押下時のリスナー
	private DialogInterface.OnClickListener mButtonListner = null;

	/**
	 * ボタンのリスナーを追加する
	 * 
	 * @param buttonListner
	 */
	public void setDialogListener(DialogInterface.OnClickListener buttonListner) {
		this.mButtonListner = buttonListner;
	}

	/**
	 * ボタンのリスナーを削除する
	 */
	public void removeDialogListener() {
		this.mButtonListner = null;
	}
	

	// リストダイアログのパラメータ
	private String[] mItems = null;
	DialogInterface.OnClickListener mListListener;

	/**
	 * リストの表示文字とリスナー登録
	 * 
	 * @param items
	 * @param listListener
	 */
	public void setListParameter(final String[] items, DialogInterface.OnClickListener listListener) {
		this.mItems = items;
		this.mListListener = listListener;
	}

	/**
	 * フラグメントのインスタンス作成後に、show(getSupportFragmentManager(), "list_dialog")を実施すると呼び出される
	 */
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

	/**
	 * ボタン２つのダイアログ
	 * 
	 * @param savedInstanceState
	 * @return
	 */
	private Dialog createNormalDialog(Bundle savedInstanceState) {
		String title = getArguments().getString(bundle_key_title);
		String dialogMessage = getArguments().getString(bundle_key_message);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title);
		builder.setMessage(dialogMessage);
		if (this.mButtonListner == null) {
			log("Not Set this listner yet.");
		}
		builder.setPositiveButton(getString(R.string.Yes), this.mButtonListner);
		builder.setNegativeButton(getString(R.string.No), this.mButtonListner);
		return builder.create();
	}

	/**
	 * ボタンが一つだけの確認ダイアログ
	 * 
	 * @param savedInstanceState
	 * @return
	 */
	private Dialog createConfirmDialog(Bundle savedInstanceState) {
		String title = getArguments().getString(bundle_key_title);
		String dialogMessage = getArguments().getString(bundle_key_message);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title);
		builder.setMessage(dialogMessage);
		if (this.mButtonListner == null) {
			log("Not Set this listner yet.");
		}
		builder.setPositiveButton(getString(R.string.Yes), this.mButtonListner);
		return builder.create();
	}

	/**
	 * リストダイアログ
	 * 
	 * @param savedInstanceState
	 * @return
	 */
	private Dialog createListDialog(Bundle savedInstanceState) {
		String title = getArguments().getString(bundle_key_title);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title);
		if (this.mButtonListner == null) {
			log("Not Set this listner yet.");
		}

		if (this.mListListener != null) {
			builder.setItems(mItems, this.mListListener);
		}
		return builder.create();
	}

	private void log(String message) {
		Log.d(TAG, message);
	}
}