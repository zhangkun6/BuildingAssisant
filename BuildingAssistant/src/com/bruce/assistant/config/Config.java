package com.bruce.assistant.config;

public class Config {

	/**
	 * The service address
	 */
	public static final String SMACK_SERVER = "q.jjiapp.com";

	/**
	 * 状态：在线、Q我、忙碌、离开、隐身、离线
	 */
	public static final int PRESENCE_STATE_AVAILABLE = 0;
	public static final int PRESENCE_STATE_CHAT = 1;
	public static final int PRESENCE_STATE_DND = 2;
	public static final int PRESENCE_STATE_AWAY = 3;
	public static final int PRESENCE_STATE_NOT_AVAILABLE_HIDE = 4;
	public static final int PRESENCE_STATE_NOT_AVAILABLE = 5;
	
	/**
	 * 注册返回结果
	 */
	public static final int REGISTER_RESULT_NO_RESULT = 0;
	public static final int REGISTER_RESULT_SUCCESS = 1;
	public static final int REGISTER_RESULT_EXIST = 2;
	public static final int REGISTER_RESULT_FAIL = 3;
	
}
