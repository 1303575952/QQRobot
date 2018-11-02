package com.sxu.qq.model;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 群.
 * 
 */
public class Group {
	@JSONField(name = "gid")
	private long id;
	private String name;
	private long flag;
	private long code;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getFlag() {
		return flag;
	}

	public void setFlag(long flag) {
		this.flag = flag;
	}

	public long getCode() {
		return code;
	}

	public void setCode(long code) {
		this.code = code;
	}

}
