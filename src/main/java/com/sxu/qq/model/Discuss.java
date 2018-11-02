package com.sxu.qq.model;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 讨论组.
 *
 */
public class Discuss {
	@JSONField(name = "did")
	private long id;
	private String name;

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
}