package com.sxu.qq.model;

/**
 * 最近会话.
 *
 */
public class Recent {
	private long uin;
	// 0:好友、1:群、2:讨论组
	private int type;

	public long getUin() {
		return uin;
	}

	public void setUin(long uin) {
		this.uin = uin;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

}
