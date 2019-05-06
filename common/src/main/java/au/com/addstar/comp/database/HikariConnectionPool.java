package au.com.addstar.comp.database;

import com.zaxxer.hikari.HikariDataSource;

import java.util.Properties;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 6/05/2019.
 */
public class HikariConnectionPool {
    private HikariDataSource dataSource;

    public HikariConnectionPool() {
        this.dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("");
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUsername("");
        dataSource.setPassword("");
        Properties properties = new Properties();
        properties.put()
        dataSource.cachePrepStmts=true
        dataSource.prepStmtCacheSize=250
        dataSource.prepStmtCacheSqlLimit=2048
        dataSource.useServerPrepStmts=true
        dataSource.useLocalSessionState=true
        dataSource.rewriteBatchedStatements=true
        dataSource.cacheResultSetMetadata=true
        dataSource.cacheServerConfiguration=true
        dataSource.elideSetAutoCommits=true
        dataSource.maintainTimeStats=false
        dataSource.set;
    }
}
