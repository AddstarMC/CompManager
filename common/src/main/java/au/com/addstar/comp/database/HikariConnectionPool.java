package au.com.addstar.comp.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

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
            config.setDriverClassName("com.mysql.jdbc.Driver");
            Logger.getLogger("CompManager").info(
                    "You should create a file to tune the connection for this database at: " +
                            directory.getAbsolutePath()+
                            "/hikari.properties");
            Logger.getLogger("CompManager").info("Further info can be found at " +
                    "https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration");
        }else{
            Logger.getLogger("CompManager").info("Using Hikari Properties file: "+propertiesFile.getAbsolutePath());
            try {
                config = new HikariConfig(propertiesFile.toString());
            }catch (RuntimeException e){
                Logger.getLogger("CompManager").warning(
                        "Your Hikari properties file has errors and has been ignored: " +
                                e.getMessage());
                config = new HikariConfig();
            }
        }
        config.setUsername(section.getString("username", "username"));
        config.setPassword(section.getString("password", "password"));
        config.setJdbcUrl(url);
        config.setPoolName("comp");
        try {
            this.dataSource = new HikariDataSource(config);
        }catch (Exception e){
            Logger.getLogger("CompManager").warning("Error initializing Datasource:" + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if(dataSource == null) throw new SQLException("No Connection Available") ;
        return dataSource.getConnection();
    }

    public void closeConnections(){
        if(dataSource != null)
            dataSource.close();
    }


}
