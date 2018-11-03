package com.sxu.qq;

import com.sxu.qq.callback.MessageCallback;
import com.sxu.qq.client.QQClient;
import com.sxu.qq.model.DiscussMessage;
import com.sxu.qq.model.GroupMessage;
import com.sxu.qq.model.Message;

public class Application {
	public static void main(String[] args) {
		//创建一个新对象需要扫描二维码登录，并传一个接收消息的回调，至于接收了消息怎么处理，看你想怎么处理
		QQClient client = new QQClient(new MessageCallback() {
			
			@Override
			public void onMessage(Message message) {
				System.out.println(message.getContent());
				
			}
			
			@Override
			public void onGroupMessage(GroupMessage message) {
				message.getContent();
				
			}
			
			@Override
			public void onDiscussMessage(DiscussMessage message) {
				message.getContent();
			}
		});
		//登录完成后可编写你想要的业务逻辑 TODO
	}
}
