package jp.kdy.partyapp.marubatsu;


public interface MyTouchListener {
	enum MyType{ Maru, Batsu, No_Permission};
	enum StatusOfRecord{ Maru, Batsu, Empty };
	/**
	 * �A�N�V�������\�b�h
	 * ���ۂɁ���~���Z�b�g���錠��������Ƃ��ɌĂяo����郁�\�b�h
	 * @param result
	 */
	public void startAction(int i, int j);
	
	/**
	 * /**
	 * �w�肵���}�X�̌������`�F�b�N���邽�߂̃��\�b�h
	 * @param i
	 * @param j
	 * @return �}�X�ɃZ�b�g���邱�Ƃ��\�Ȃ��0��1��Ԃ�(0�Ȃ灛�A1�Ȃ�~)�B�s�\�ȏꍇ��
	 */
	public MyType checkPermission(int i, int j);
	
}
