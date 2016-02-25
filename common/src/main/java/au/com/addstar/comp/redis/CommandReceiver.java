package au.com.addstar.comp.redis;

public interface CommandReceiver {
	/**
	 * Called to receive the data send to this server
	 * @param serverId The sending server id
	 * @param command The command being sent
	 */
	public void onReceive(String serverId, String command);
}
