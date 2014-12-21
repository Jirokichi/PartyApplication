package jp.kdy.partyapp.marubatsu;

import jp.kdy.partyapp.R;
import jp.kdy.partyapp.marubatsu.MyTouchListener.MyType;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * ○×ゲームの盤面
 * 
 * @author yuya
 * 
 */
public class MaruBatsuView extends View {

	private static final String TAG = "MaruBatsuView";

	private static int STROLE_WIDTH = 12;

	Path mPath;
	Paint mPaint;
	Bitmap mBitmap;
	Bitmap mBitmapTmp;
	Canvas mCanvas;
	Canvas mCavasTmp;

	boolean isTouching = false;

	int mWidth = 0;
	int mHeight = 0;

	MyTouchListener mListner;

	public MaruBatsuView(Context context) {
		super(context);
	}

	public MaruBatsuView(Context context, AttributeSet attrs) {
		super(context, attrs);
		log("MaruBatsuView");
		setFocusable(true);
		initPaint();
	}

	public MaruBatsuView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setMyTouchListener(MyTouchListener lister) {
		log("setMyTouchListener");
		mListner = lister;
	}

	public void removeMyTouchListener() {
		mListner = null;
	}

	/*
	 * 描画用Paintの初期化
	 */
	private void initPaint() {
		log("initPaint");
		mPath = new Path();

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(Color.BLACK);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(STROLE_WIDTH);
	}

	/**
	 * 画面サイズ変更時の通知
	 * 
	 * @param w
	 *            , h, oldw, oldh
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		log("onSizeChanged Width:" + w + ",Height:" + h);
		mWidth = w;
		mHeight = h;
		resetView();
	}

	private void drawMaru(Canvas canvas, Paint paint, int i, int j, int length) {
		int w = length;
		Bitmap bmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.maru);
		Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, w / 3, w / 3, false);
		canvas.drawBitmap(bmp2, j * w / 3, i * w / 3, paint);

	}

	private void drawBatsu(Canvas canvas, Paint paint, int i, int j, int length) {
		int w = length;
		Bitmap bmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.batsu);
		Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, w / 3, w / 3, false);
		canvas.drawBitmap(bmp2, j * w / 3, i * w / 3, paint);
	}

	public void drawBatsu(int i, int j) {
		Bitmap bmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.batsu);
		Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, mWidth / 3, mWidth / 3, false);
		mCanvas.drawBitmap(bmp2, j * mWidth / 3, i * mWidth / 3, mPaint);
	}

	public void drawMaru(int i, int j) {
		Bitmap bmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.maru);
		Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, mWidth / 3, mWidth / 3, false);
		mCanvas.drawBitmap(bmp2, j * mWidth / 3, i * mWidth / 3, mPaint);
	}

	private void resfreshTmpCanvas() {
		mCavasTmp.setBitmap(null);
		mBitmapTmp.recycle();
		mBitmapTmp = null;
		mBitmapTmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mCavasTmp.setBitmap(mBitmapTmp);
	}

	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			touch_start(x, y);
			invalidate();
			break;
		case MotionEvent.ACTION_MOVE:
			touch_move(x, y);
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			touch_up(x, y);
			invalidate();
			break;
		}
		return true;
	}

	/**
	 * 画面を初期状態に戻す
	 */
	public void resetView() {
		// キャンバス作成
		int w = mWidth;
		int h = mHeight;
		
		
		
		if(mCanvas != null){
			mCanvas.setBitmap(null);
			if(mBitmap != null){
				mBitmap.recycle();
				mBitmap = null;
			}
		}
		
		mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		
		if(mCanvas == null){
			mCanvas = new Canvas(mBitmap);
			mBitmapTmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			mCavasTmp = new Canvas(mBitmapTmp);
		}
		
		mCanvas.drawRect(new Rect(0 + STROLE_WIDTH / 2, 0 + STROLE_WIDTH / 2, w - STROLE_WIDTH / 2, w - STROLE_WIDTH / 2), mPaint);
		mCanvas.drawLine(w * 1 / 3, 0, w * 1 / 3, w, mPaint);
		mCanvas.drawLine(w * 2 / 3, 0, w * 2 / 3, w, mPaint);
		mCanvas.drawLine(0, w * 1 / 3, w, w * 1 / 3, mPaint);
		mCanvas.drawLine(0, w * 2 / 3, w, w * 2 / 3, mPaint);
	}

	/* 描画関数 */
	@Override
	protected void onDraw(Canvas canvas) {
		log("onDraw");
		canvas.drawBitmap(mBitmap, 0, 0, mPaint);
		canvas.drawPath(mPath, mPaint);

		if (isTouching)
			canvas.drawBitmap(mBitmapTmp, 0, 0, mPaint);
	}

	private int pointXY = -1;
	private static final float TOUCH_TOLERANCE = 4;// 最小移動量

	private void touch_start(float x, float y) {

		log("touch_start");
		int i = getIfromXorY(y);
		int j = getIfromXorY(x);

		if ((i < 3 && i >= 0) && (j < 3 && j >= 0)) {
			MyType type = mListner.checkPermission(i, j);
			if (type != MyType.No_Permission) {
				pointXY = i * 10 + j;
				isTouching = true;
				log(String.format("(%d, %d)", i, j));
				if (type == MyType.Batsu)
					this.drawBatsu(mCavasTmp, mPaint, i, j, mWidth);
				else if (type == MyType.Maru)
					this.drawMaru(mCavasTmp, mPaint, i, j, mWidth);
			}
		}
	}

	private void touch_move(float x, float y) {
		log("touch_move");
	}

	private void touch_up(float x, float y) {
		log("touch_up");
		if (isTouching) {
			isTouching = false;
			resfreshTmpCanvas();

			// タップ開始の枠と話した枠が同じかどうかチェックする
			int i = getIfromXorY(y);
			int j = getIfromXorY(x);
			int lpointXY = i * 10 + j;
			log(String.format("(%d, %d)", lpointXY, pointXY));
			if (lpointXY == pointXY) {
				mListner.startAction(i, j);
			} else {

			}
		}
		pointXY = -1;
	}

	private int getIfromXorY(float XorY) {
		int i = 0;
		int tmp = (int) XorY;
		while ((tmp -= mWidth / 3) > 0) {
			i++;
		}
		return i;
	}

	private void log(String message) {
		Log.d(TAG, message);
	}
}
