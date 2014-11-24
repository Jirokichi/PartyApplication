package jp.kdy.partyapp.marubatsu;


public interface MyTouchListener {
	enum MyType{ Maru, Batsu, No_Permission};
	enum StatusOfRecord{ Maru, Batsu, Empty };
	/**
	 * アクションメソッド
	 * 実際に○や×をセットする権限があるときに呼び出されるメソッド
	 * @param result
	 */
	public void startAction(int i, int j);
	
	/**
	 * /**
	 * 指定したマスの権限をチェックするためのメソッド
	 * @param i
	 * @param j
	 * @return マスにセットすることが可能ならば0か1を返す(0なら○、1なら×)。不可能な場合は
	 */
	public MyType checkPermission(int i, int j);
	
}
