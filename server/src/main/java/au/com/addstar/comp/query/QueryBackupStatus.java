package au.com.addstar.comp.query;

import au.com.addstar.comp.redis.RedisQueryHandler;
import au.com.addstar.comp.services.PlotBackupService;

/**
 * Query handler for checking if a backup operation is currently in progress.
 */
public class QueryBackupStatus implements RedisQueryHandler {
	private final PlotBackupService backupService;
	
	public QueryBackupStatus(PlotBackupService backupService) {
		this.backupService = backupService;
	}
	
	@Override
	public String onQuery(String command, String[] arguments) {
		return String.valueOf(backupService.isBackupInProgress());
	}
}
