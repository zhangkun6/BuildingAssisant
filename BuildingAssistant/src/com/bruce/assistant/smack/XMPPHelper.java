package com.bruce.assistant.smack;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.AndroidConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.ReportedData;
import org.jivesoftware.smackx.ReportedData.Row;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.search.UserSearchManager;

import android.graphics.drawable.Drawable;
import android.util.Log;

import com.bruce.assistant.app.MyApplication;
import com.bruce.assistant.config.Config;
import com.bruce.assistant.util.FormatTools;

public class XMPPHelper {

	private static final String TAG = "XMPPHelper";
	public static XMPPConnection conn;

	/**
	 * 当前聊天对象的JID
	 */
	public static String chatWith = "";

	/**
	 * 连接即时通讯服务器
	 * 
	 * @throws XMPPException
	 */
	public static void connect() throws XMPPException {
		SmackAndroid.init(MyApplication.getContext());
		AndroidConnectionConfiguration connConfig;
		connConfig = new AndroidConnectionConfiguration(Config.SMACK_SERVER);
		connConfig
				.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
		conn = new XMPPConnection(connConfig);
		conn.connect();
	}

	/**
	 * 登录即时通讯服务器
	 * 
	 * @param username
	 * @param password
	 * @throws XMPPException
	 */
	public static void login(XMPPConnection connection, String username,
			String password) throws XMPPException {
		try {
			connection.login(username, password);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 修改密码
	 * 
	 * @return
	 */
	public static boolean changePassword(XMPPConnection connection, String pwd) {
		try {
			connection.getAccountManager().changePassword(pwd);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 注册用户
	 * 
	 * @param entity
	 * @return
	 */
	public static boolean register(FriendsEntity entity) {
		try {
			AccountManager accountMgr = conn.getAccountManager();
			Map<String, String> attributes = new HashMap<String, String>();
			attributes.put("name", entity.Name);
			accountMgr.createAccount(entity.StudentId, "123", attributes);
			Log.d(TAG, entity.StudentId + "注册成功");
		} catch (Exception e) {
			Log.d(TAG, entity.StudentId + "用户已存在");
			return false;
		}
		return true;
	}

	/**
	 * 判断好友是否在线
	 * 
	 * @param user
	 * @return
	 */
	public static boolean isOnline(XMPPConnection connection, String user) {
		Roster roster = connection.getRoster();
		Presence presence = roster.getPresence(user);
		if (Presence.Type.available.equals(presence.getType())) {
			return true;
		}
		return false;
	}

	/**
	 * 更改用户状态
	 * 
	 * @param code
	 */
	public void setPresence(XMPPConnection connection, int code) {
		if (connection == null)
			return;
		Presence presence;
		switch (code) {
		case Config.PRESENCE_STATE_AVAILABLE:
			presence = new Presence(Presence.Type.available);
			connection.sendPacket(presence);
			Log.v("state", "设置在线");
			break;
		case Config.PRESENCE_STATE_CHAT:
			presence = new Presence(Presence.Type.available);
			presence.setMode(Presence.Mode.chat);
			connection.sendPacket(presence);
			Log.v("state", "设置Q我吧");
			System.out.println(presence.toXML());
			break;
		case Config.PRESENCE_STATE_DND:
			presence = new Presence(Presence.Type.available);
			presence.setMode(Presence.Mode.dnd);
			connection.sendPacket(presence);
			Log.v("state", "设置忙碌");
			System.out.println(presence.toXML());
			break;
		case Config.PRESENCE_STATE_AWAY:
			presence = new Presence(Presence.Type.available);
			presence.setMode(Presence.Mode.away);
			connection.sendPacket(presence);
			Log.v("state", "设置离开");
			System.out.println(presence.toXML());
			break;
		case Config.PRESENCE_STATE_NOT_AVAILABLE_HIDE:
			Roster roster = connection.getRoster();
			Collection<RosterEntry> entries = roster.getEntries();
			for (RosterEntry entry : entries) {
				presence = new Presence(Presence.Type.unavailable);
				presence.setPacketID(Packet.ID_NOT_AVAILABLE);
				presence.setFrom(connection.getUser());
				presence.setTo(entry.getUser());
				connection.sendPacket(presence);
				System.out.println(presence.toXML());
			}
			// 向同一用户的其他客户端发送隐身状态
			presence = new Presence(Presence.Type.unavailable);
			presence.setPacketID(Packet.ID_NOT_AVAILABLE);
			presence.setFrom(connection.getUser());
			presence.setTo(StringUtils.parseBareAddress(connection.getUser()));
			connection.sendPacket(presence);
			Log.v("state", "设置隐身");
			break;
		case Config.PRESENCE_STATE_NOT_AVAILABLE:
			presence = new Presence(Presence.Type.unavailable);
			connection.sendPacket(presence);
			Log.v("state", "设置离线");
			break;
		default:
			break;
		}
	}

	/**
	 * 获取所有组
	 * 
	 * @param roster
	 * @return 所有组集合
	 */
	public static List<RosterGroup> getGroups(Roster roster) {
		List<RosterGroup> grouplist = new ArrayList<RosterGroup>();
		Collection<RosterGroup> rosterGroup = roster.getGroups();
		Iterator<RosterGroup> i = rosterGroup.iterator();
		while (i.hasNext()) {
			grouplist.add(i.next());
		}
		return grouplist;
	}

	/**
	 * 创建一个分组
	 * 
	 * @param roster
	 * @param groupName
	 * @return
	 */
	public static boolean addGroup(Roster roster, String groupName) {
		try {
			roster.createGroup(groupName);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 获取某个组里面的所有好友
	 * 
	 * @param roster
	 * @param groupName
	 *            组名
	 * @return
	 */
	public static List<RosterEntry> getEntriesByGroup(Roster roster,
			String groupName) {
		List<RosterEntry> Entrieslist = new ArrayList<RosterEntry>();
		RosterGroup rosterGroup = roster.getGroup(groupName);
		Collection<RosterEntry> rosterEntry = rosterGroup.getEntries();
		Iterator<RosterEntry> i = rosterEntry.iterator();
		while (i.hasNext()) {
			Entrieslist.add(i.next());
		}
		return Entrieslist;
	}

	/**
	 * 获取所有好友信息
	 * 
	 * @param roster
	 * @return
	 */
	public static List<RosterEntry> getAllEntries(Roster roster) {
		List<RosterEntry> Entrieslist = new ArrayList<RosterEntry>();
		Collection<RosterEntry> rosterEntry = roster.getEntries();
		Iterator<RosterEntry> i = rosterEntry.iterator();
		while (i.hasNext()) {
			Entrieslist.add(i.next());
		}
		return Entrieslist;
	}

	/**
	 * 根据名字获取好友列表数据
	 * 
	 * @return
	 */
	public static List<List<RosterEntry>> getAllEntriesByName(
			XMPPConnection connection, String name) {
		Log.d(TAG, "get friends from XMPP server...");
		List<List<RosterEntry>> friends = new ArrayList<List<RosterEntry>>();
		List<RosterEntry> onlineFriends = new ArrayList<RosterEntry>();
		List<RosterEntry> offlineFriends = new ArrayList<RosterEntry>();
		Roster roster = connection.getRoster();
		Collection<RosterEntry> entries = roster.getEntries();
		Log.d(TAG, "Friends size:" + entries.size());
		Iterator<RosterEntry> iterator = entries.iterator();
		while (iterator.hasNext()) {
			RosterEntry entry = iterator.next();
			if (isOnline(connection, entry.getUser())) {
				if (entry.getName() != null && entry.getName().contains(name)) {
					onlineFriends.add(entry);
				}
			} else {
				if (entry.getName() != null && entry.getName().contains(name)) {
					offlineFriends.add(entry);
				}
			}
		}
		// 按照名字排序
		Collections.sort(onlineFriends, new RosterNameComparator());
		Collections.sort(offlineFriends, new RosterNameComparator());
		friends.add(onlineFriends);
		friends.add(offlineFriends);
		return friends;
	}

	/**
	 * 查询用户
	 * 
	 * @param connection
	 * @param serverDomain
	 * @param userName
	 * @return
	 * @throws XMPPException
	 */
	public static List<User> searchUsers(XMPPConnection connection,
			String serverDomain, String userName) throws XMPPException {
		List<User> results = new ArrayList<User>();
		System.out.println("查询开始..............." + connection.getHost()
				+ connection.getServiceName());

		UserSearchManager usm = new UserSearchManager(connection);

		Form searchForm = usm.getSearchForm(serverDomain);
		Form answerForm = searchForm.createAnswerForm();
		answerForm.setAnswer("userAccount", true);
		answerForm.setAnswer("userPhote", userName);
		ReportedData data = usm.getSearchResults(answerForm, serverDomain);

		Iterator<Row> it = data.getRows();
		Row row = null;
		User user = null;
		while (it.hasNext()) {
			user = new User();
			row = it.next();
			user.setUserAccount(row.getValues("userAccount").next().toString());
			user.setUserPhote(row.getValues("userPhote").next().toString());

			System.out.println(row.getValues("userAccount").next());
			System.out.println(row.getValues("userPhote").next());
			results.add(user);
			// 若存在，则有返回,UserName一定非空，其他两个若是有设，一定非空
		}
		return results;
	}

	/**
	 * 获取用户VCard信息
	 * 
	 * @param connection
	 * @param user
	 * @return
	 * @throws XMPPException
	 */
	public static VCard getUserVCard(XMPPConnection connection, String user)
			throws XMPPException {
		VCard vcard = new VCard();
		vcard.load(connection, user);
		return vcard;
	}

	/**
	 * 获取用户头像信息
	 * 
	 * @param connection
	 * @param user
	 * @return
	 */
	public static Drawable getUserImage(XMPPConnection connection, String user) {
		ByteArrayInputStream bais = null;
		try {
			VCard vcard = new VCard();
			// 加入这句代码，解决No VCard for
			ProviderManager.getInstance().addIQProvider("vCard", "vcard-temp",
					new org.jivesoftware.smackx.provider.VCardProvider());

			vcard.load(connection, user + "@" + connection.getServiceName());

			if (vcard == null || vcard.getAvatar() == null)
				return null;
			bais = new ByteArrayInputStream(vcard.getAvatar());

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (bais == null)
			return null;
		return FormatTools.getInstance().InputStream2Drawable(bais);
	}

	/**
	 * 修改用户头像
	 * 
	 * @param connection
	 * @param f
	 * @throws XMPPException
	 * @throws IOException
	 */
	public static void changeImage(XMPPConnection connection, File f)
			throws XMPPException, IOException {

		VCard vcard = new VCard();
		vcard.load(connection);

		byte[] bytes = new byte[12];

		// TODO
		// bytes = getFileBytes(f);
		String encodedImage = StringUtils.encodeBase64(bytes);
		vcard.setAvatar(bytes, encodedImage);
		vcard.setEncodedImage(encodedImage);
		vcard.setField("PHOTO", "<TYPE>image/jpg</TYPE><BINVAL>" + encodedImage
				+ "</BINVAL>", true);

		ByteArrayInputStream bais = new ByteArrayInputStream(vcard.getAvatar());
		FormatTools.getInstance().InputStream2Bitmap(bais);

		vcard.save(connection);
	}

	/**
	 * 添加好友 无分组
	 * 
	 * @param roster
	 * @param userName
	 * @param name
	 * @return
	 */
	public static boolean addUser(Roster roster, String userName, String name) {
		try {
			roster.createEntry(userName, name, null);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 添加好友 有分组
	 * 
	 * @param roster
	 * @param userName
	 * @param name
	 * @param groupName
	 * @return
	 */
	public static boolean addUser(Roster roster, String userName, String name,
			String groupName) {
		try {
			roster.createEntry(userName, name, new String[] { groupName });
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 删除好友
	 * 
	 * @param roster
	 * @param userName
	 * @return
	 */
	public static boolean removeUser(Roster roster, String userName) {
		try {
			if (userName.contains("@")) {
				userName = userName.split("@")[0];
			}

			RosterEntry entry = roster.getEntry(userName);
			System.out.println("删除好友：" + userName);
			System.out.println("User." + roster.getEntry(userName) == null);
			roster.removeEntry(entry);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 注销当前用户
	 * 
	 * @return
	 */
	public static boolean deleteAccount(XMPPConnection connection) {
		try {
			connection.getAccountManager().deleteAccount();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	static class RosterNameComparator implements Comparator<RosterEntry> {

		private Collator collator = Collator.getInstance(Locale.CHINESE);

		@Override
		public int compare(RosterEntry lhs, RosterEntry rhs) {
			CollationKey key1 = collator.getCollationKey(lhs.getName());
			CollationKey key2 = collator.getCollationKey(rhs.getName());
			if (key1 == null && key2 == null) {
				return 0;
			} else if (key1 == null && key2 != null) {
				return 1;
			} else if (key1 != null && key2 == null) {
				return -1;
			} else {
				return key1.compareTo(key2);
			}
		}

	}

}
