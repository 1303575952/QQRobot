package com.sxu.qq.model;

/**
 * 讨论组成员.
 */
public class DiscussUser {
	private long uin;
	private String nick;
	private int clientType;
	private String status;

	@Override
	public String toString() {
		return "DiscussUser{" + "uin=" + uin + ", nick='" + nick + '\'' + ", clientType='" + clientType + '\''
				+ ", status='" + status + '\'' + '}';
	}

	public long getUin() {
		return uin;
	}

	public void setUin(long uin) {
		this.uin = uin;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public int getClientType() {
		return clientType;
	}

	public void setClientType(int clientType) {
		this.clientType = clientType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
