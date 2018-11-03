package com.sxu.qq.client;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sxu.qq.model.DiscussMessage;
import com.sxu.qq.model.GroupMessage;
import com.sxu.qq.model.Message;
import com.sxu.qq.model.FriendStatus;
import com.sxu.qq.model.UserInfo;
import com.sxu.qq.callback.MessageCallback;
import com.sxu.qq.constant.ApiURL;

import net.dongliu.requests.Client;
import net.dongliu.requests.HeadOnlyRequestBuilder;
import net.dongliu.requests.Response;
import net.dongliu.requests.Session;
import net.dongliu.requests.exception.RequestException;
import net.dongliu.requests.struct.Cookie;

public class QQClient implements Closeable {

	private static final Logger LOGGER = Logger.getLogger(QQClient.class);
	// Nginx错误重试次数
	private static int retryTimesOnFailed = 3;
	// 消息ID
	private static long MESSAGE_ID = 43690001;
	// 客户端ID
	private static final long Client_ID = 53999199;
	// 消息发送失败重试次数
	private static final long RETRY_TIMES = 5;
	// 客户端
	private Client client;
	// 会话
	private Session session;
	// 二维码令牌
	private String qrsig;
	// 鉴权参数
	private String ptwebqq;
	private String vfwebqq;
	private long uin;
	private String psessionid;
	// 线程开关
	private volatile boolean pollStarted;

	public QQClient(final MessageCallback callback) {
		this.client = Client.pooled().maxPerRoute(5).maxTotal(10).build();
		this.session = client.session();
		login();
		if (callback != null) {
			this.pollStarted = true;
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (true) {
						if (!pollStarted) {
							return;
						}
						try {
							pollMessage(callback);
						} catch (RequestException e) {
							// 忽略SocketTimeoutException
							if (!(e.getCause() instanceof SocketTimeoutException)) {
								LOGGER.error(e.getMessage());
							}
						} catch (Exception e) {
							LOGGER.error(e.getMessage());
						}
					}
				}
			}).start();
		}
	}

	private void login() {
		getQRCode();
		String url = verifyQRCode();
		getPtwebqq(url);
		getVfwebqq();
		getUinAndPsessionid();
		getFriendStatus();
		// 登陆成功欢迎
		UserInfo userInfo = getAccountInfo();
		LOGGER.info(userInfo.getNick() + "，欢迎！");
	}

	// 登录流程1：获取二维码
	private void getQRCode() {
		LOGGER.debug("开始获取二维码");
		// 本地存储二维码图片
		String filePath;

		try {
			String curPath = System.getProperty("user.dir")+"//file//";
			filePath = new File(curPath+"qrcode.png").getCanonicalPath();
			LOGGER.debug("filePath:" + filePath);
		} catch (IOException e) {
			throw new IllegalStateException("二维码保存失败");
		}
		Response response = session.get(ApiURL.GET_QR_CODE.getUrl()).addHeader("User-Agent", ApiURL.USER_AGENT)
				.file(filePath);
		for (Cookie cookie : response.getCookies()) {
			if (Objects.equals(cookie.getName(), "qrsig")) {
				qrsig = cookie.getValue();
				break;
			}
		}
		LOGGER.info("二维码保存在：" + filePath + "文件中，请打开手机扫描二维码");
	}

	// 登录流程2：校验二维码
	private String verifyQRCode() {
		LOGGER.debug("等待扫描二维码");
		// 阻塞直到二维码认证成功
		while (true) {
			sleep(1);
			Response<String> response = get(ApiURL.VERIFY_QR_CODE, hash33(qrsig));
			String result = response.getBody();
			if (result.contains("成功")) {
				for (String content : result.split("','")) {
					if (content.startsWith("http")) {
						LOGGER.info("正在登录，请稍后");
						return content;
					}
				}
			} else if (result.contains("已失效")) {
				LOGGER.info("二维码已失效，尝试重新获取二维码");
				getQRCode();
			}
		}
	}

	// 登录流程3：获取ptwebqq
	private void getPtwebqq(String url) {
		LOGGER.debug("开始获取ptwebqq");
		Response<String> response = get(ApiURL.GET_PTWEBQQ, url);
		this.ptwebqq = response.getCookies().get("ptwebqq").iterator().next().getValue();
	}

	// 登录流程4：获取vfwebqq
	private void getVfwebqq() {
		LOGGER.debug("开始获取vfwebqq");
		Response<String> response = get(ApiURL.GET_VFWEBQQ, ptwebqq);
		int retryTimes4Vfwebqq = retryTimesOnFailed;
		while (response.getStatusCode() == 404 && retryTimes4Vfwebqq > 0) {
			response = get(ApiURL.GET_VFWEBQQ, ptwebqq);
			retryTimes4Vfwebqq--;
		}
		this.vfwebqq = getJsonObjectResult(response).getString("vfwebqq");
	}

	// 登录流程5：获取uin和psessionid
	private void getUinAndPsessionid() {
		LOGGER.debug("开始获取uin和psessionid");

		JSONObject r = new JSONObject();
		r.put("ptwebqq", ptwebqq);
		r.put("clientid", Client_ID);
		r.put("psessionid", "");
		r.put("status", "online");

		Response<String> response = post(ApiURL.GET_UIN_AND_PSESSIONID, r);
		JSONObject result = getJsonObjectResult(response);
		this.psessionid = result.getString("psessionid");
		this.uin = result.getLongValue("uin");
	}

	// 获得好友状态
	public List<FriendStatus> getFriendStatus() {
		LOGGER.debug("开始获取好友状态");
		Response<String> response = get(ApiURL.GET_FRIEND_STATUS, vfwebqq, psessionid);
		return JSON.parseArray(getJsonArrayResult(response).toJSONString(), FriendStatus.class);
	}

	// 获取登录用户信息
	public UserInfo getAccountInfo() {
		LOGGER.debug("开始获取登录用户信息");

		Response<String> response = get(ApiURL.GET_ACCOUNT_INFO);
		int retryTimes4AccountInfo = retryTimesOnFailed;
		while (response.getStatusCode() == 404 && retryTimes4AccountInfo > 0) {
			response = get(ApiURL.GET_ACCOUNT_INFO);
			retryTimes4AccountInfo--;
		}
		return JSON.parseObject(getJsonObjectResult(response).toJSONString(), UserInfo.class);
	}

	// 拉取消息
	private void pollMessage(MessageCallback callback) {
		LOGGER.debug("开始接收消息");

		JSONObject r = new JSONObject();
		r.put("ptwebqq", ptwebqq);
		r.put("clientid", Client_ID);
		r.put("psessionid", psessionid);
		r.put("key", "");

		Response<String> response = post(ApiURL.POLL_MESSAGE, r);
		JSONArray array = getJsonArrayResult(response);
		for (int i = 0; array != null && i < array.size(); i++) {
			JSONObject message = array.getJSONObject(i);
			String type = message.getString("poll_type");
			if ("message".equals(type)) {
				callback.onMessage(new Message(message.getJSONObject("value")));
			} else if ("group_message".equals(type)) {
				callback.onGroupMessage(new GroupMessage(message.getJSONObject("value")));
			} else if ("discu_message".equals(type)) {
				callback.onDiscussMessage(new DiscussMessage(message.getJSONObject("value")));
			}
		}
	}

	// 用于生成ptqrtoken的哈希函数
	private static int hash33(String s) {
		int e = 0, n = s.length();
		for (int i = 0; n > i; ++i)
			e += (e << 5) + s.charAt(i);
		return 2147483647 & e;
	}

	// 发送get请求
	private Response<String> get(ApiURL url, Object... params) {
		HeadOnlyRequestBuilder request = session.get(url.buildUrl(params)).addHeader("User-Agent", ApiURL.USER_AGENT);
		if (url.getReferer() != null) {
			request.addHeader("Referer", url.getReferer());
		}
		return request.text(StandardCharsets.UTF_8);
	}

	// 发送post请求
	private Response<String> post(ApiURL url, JSONObject r) {
		return session.post(url.getUrl()).addHeader("User-Agent", ApiURL.USER_AGENT)
				.addHeader("Referer", url.getReferer()).addHeader("Origin", url.getOrigin())
				.addForm("r", r.toJSONString()).text(StandardCharsets.UTF_8);
	}

	// 检验Json返回结果
	private static JSONObject getResponseJson(Response<String> response) {
		if (response.getStatusCode() != 200) {
			throw new RequestException(String.format("请求失败，Http返回码[%d]", response.getStatusCode()));
		}
		JSONObject json = JSON.parseObject(response.getBody());
		Integer retCode = json.getInteger("retcode");
		if (retCode == null) {
			throw new RequestException(String.format("请求失败，Api返回异常", retCode));
		} else if (retCode != 0) {
			switch (retCode) {
			case 103: {
				LOGGER.error("请求失败，Api返回码[103]。你需要进入http://w.qq.com，检查是否能正常接收消息。如果可以的话点击[设置]->[退出登录]后查看是否恢复正常");
				break;
			}
			case 100100: {
				LOGGER.debug("请求失败，Api返回码[100100]");
				break;
			}
			default: {
				throw new RequestException(String.format("请求失败，Api返回码[%d]", retCode));
			}
			}
		}
		return json;
	}

	// 获取返回json的result字段（JSONObject类型）
	private static JSONObject getJsonObjectResult(Response<String> response) {
		return getResponseJson(response).getJSONObject("result");
	}

	// 获取返回json的result字段（JSONArray类型）
	private static JSONArray getJsonArrayResult(Response<String> response) {
		return getResponseJson(response).getJSONArray("result");
	}

	private static void sleep(long seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			// skip
		}
	}

	@Override
	public void close() throws IOException {
		this.pollStarted = false;
		if (this.client != null) {
			this.client.close();
		}
	}

}
