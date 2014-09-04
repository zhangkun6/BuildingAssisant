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
	 * ��ǰ��������JID
	 */
	public static String chatWith = "";

	/**
	 * ���Ӽ�ʱͨѶ������
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
	 * ��¼��ʱͨѶ������
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
	 * �޸�����
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
	 * ע���û�
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
			Log.d(TAG, entity.StudentId + "ע��ɹ�");
		} catch (Exception e) {
			Log.d(TAG, entity.StudentId + "�û��Ѵ���");
			return false;
		}
		return true;
	}

	/**
	 * �жϺ����Ƿ�����
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
	 * �����û�״̬
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
			Log.v("state", "��������");
			break;
		case Config.PRESENCE_STATE_CHAT:
			presence = new Presence(Presence.Type.available);
			presence.setMode(Presence.Mode.chat);
			connection.sendPacket(presence);
			Log.v("state", "����Q�Ұ�");
			System.out.println(presence.toXML());
			break;
		case Config.PRESENCE_STATE_DND:
			presence = new Presence(Presence.Type.available);
			presence.setMode(Presence.Mode.dnd);
			connection.sendPacket(presence);
			Log.v("state", "����æµ");
			System.out.println(presence.toXML());
			break;
		case Config.PRESENCE_STATE_AWAY:
			presence = new Presence(Presence.Type.available);
			presence.setMode(Presence.Mode.away);
			connection.sendPacket(presence);
			Log.v("state", "�����뿪");
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
			// ��ͬһ�û��������ͻ��˷�������״̬
			presence = new Presence(Presence.Type.unavailable);
			presence.setPacketID(Packet.ID_NOT_AVAILABLE);
			presence.setFrom(connection.getUser());
			presence.setTo(StringUtils.parseBareAddress(connection.getUser()));
			connection.sendPacket(presence);
			Log.v("state", "��������");
			break;
		case Config.PRESENCE_STATE_NOT_AVAILABLE:
			presence = new Presence(Presence.Type.unavailable);
			connection.sendPacket(presence);
			Log.v("state", "��������");
			break;
		default:
			break;
		}
	}

	/**
	 * ��ȡ������
	 * 
	 * @param roster
	 * @return �����鼯��
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
	 * ����һ������
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
	 * ��ȡĳ������������к���
	 * 
	 * @param roster
	 * @param groupName
	 *            ����
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
	 * ��ȡ���к�����Ϣ
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
	 * �������ֻ�ȡ�����б�����
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
		// ������������
		Collections.sort(onlineFriends, new RosterNameComparator());
		Collections.sort(offlineFriends, new RosterNameComparator());
		friends.add(onlineFriends);
		friends.add(offlineFriends);
		return friends;
	}

	/**
	 * ��ѯ�û�
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
		System.out.println("��ѯ��ʼ..............." + connection.getHost()
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
			// �����ڣ����з���,UserNameһ���ǿգ����������������裬һ���ǿ�
		}
		return results;
	}

	/**
	 * ��ȡ�û�VCard��Ϣ
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
	 * ��ȡ�û�ͷ����Ϣ
	 * 
	 * @param connection
	 * @param user
	 * @return
	 */
	public static Drawable getUserImage(XMPPConnection connection, String user) {
		ByteArrayInputStream bais = null;
		try {
			VCard vcard = new VCard();
			// ���������룬���No VCard for
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
	 * �޸��û�ͷ��
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
	 * ��Ӻ��� �޷���
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
	 * ��Ӻ��� �з���
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
	 * ɾ������
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
			System.out.println("ɾ�����ѣ�" + userName);
			System.out.println("User." + roster.getEntry(userName) == null);
			roster.removeEntry(entry);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * ע����ǰ�û�
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
