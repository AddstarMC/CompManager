package au.com.addstar.comp.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 6/05/2019.
 */
public class HikariConnectionPool {
    private HikariDataSource dataSource;
    private File propertiesFile;

    public HikariConnectionPool(ConfigurationSection section, File directory) {

        this.propertiesFile = new File(directory,"hikari.properties");
        String url = String.format(
                "jdbc:mysql://%s:%d/%s",
                section.getString("host", "localhost"),
                section.getInt("port", 3306),
                section.getString("database", "comp")
        );
        HikariConfig config;
        if(!propertiesFile.exists()) {
            config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setDriverClassName("com.mysql.jdbc.Driver");
            config.setUsername(section.getString("username", "username"));
            config.setPassword(section.getString("password", "password"));
            config.addDataSourceProperty("useSSL",false);
            config.addDataSourceProperty("cachePrepStmts",true);
            config.addDataSourceProperty("prepStmtCacheSize",250);
            config.addDataSourceProperty("prepStmtCacheSqlLimit",2048);
            config.addDataSourceProperty("useServerPrepStmts",true);
            config.addDataSourceProperty("useLocalSessionState",true);
            config.addDataSourceProperty("rewriteBatchedStatements",true);
            config.addDataSourceProperty("cacheResultSetMetadata",true);
            config.addDataSourceProperty("cacheServerConfiguration",true);
            config.addDataSourceProperty("elideSetAutoCommits",true);
            config.addDataSourceProperty("maintainTimeStats",true);
            saveProperties();
        }else{
            config = new HikariConfig(propertiesFile.toString());
        }
        this.dataSource = new HikariDataSource(config);
        try{
            dataSource.getConnection();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void saveProperties(){
        try {
            FileOutputStream out = new FileOutputStream(propertiesFile);
            dataSource.getDataSourceProperties().store(out,"Hikari Properties");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closeConnections(){
        dataSource.close();
    }


}
