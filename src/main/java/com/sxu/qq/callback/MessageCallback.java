package com.sxu.qq.callback;

import com.sxu.qq.model.DiscussMessage;
import com.sxu.qq.model.GroupMessage;
import com.sxu.qq.model.Message;

public interface MessageCallback {
	void onMessage(Message message);

	void onGroupMessage(GroupMessage message);

	void onDiscussMessage(DiscussMessage message);
}
