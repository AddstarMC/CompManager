package au.com.addstar.comp.lobby.entry;

import java.util.UUID;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;

import au.com.addstar.comp.confirmations.Confirmable;
import au.com.addstar.comp.entry.EntryDeniedException;
import au.com.addstar.comp.entry.EntryDeniedException.Reason;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.redis.QueryException;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.comp.util.Messages;

public class RemoteEnterFuture extends AbstractFuture<Confirmable> implements FutureCallback<String> {
	private final CompServer server;
	private final UUID playerId;
	private final RedisManager redisManager;
	private final Messages messages;
	
	public RemoteEnterFuture(CompServer server, UUID playerId, RedisManager redisManager, Messages messages) {
		this.server = server;
		this.playerId = playerId;
		this.redisManager = redisManager;
		this.messages = messages;
	}
	
	@Override
	public void onSuccess(String returnValue) {
		String[] parts = returnValue.split(",");
		switch (parts[0]) {
		case "true":
			// Entry is ok
			set(new RemoteEnterHandler(server, playerId, redisManager, messages));
			break;
		case "false":
			// Was blocked from entering
			Reason reason = Reason.valueOf(parts[1]);
			setException(new EntryDeniedException(reason, ""));
			break;
		default:
			// I dont know what this is
			setException(new QueryException("Unknown return value"));
			break;
		}
	}
	
	@Override
	public void onFailure(Throwable error) {
		setException(error);
	}
}
