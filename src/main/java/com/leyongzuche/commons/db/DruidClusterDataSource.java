package com.leyongzuche.commons.db;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author pengqingsong
 * 09/09/2017
 */
public class DruidClusterDataSource extends AbstractRoutingDataSource {

    private String dbDriver;

    private int dbInitialSize;

    private int dbMinIdle;

    private int dbMaxActive;

    private String dbMasterConfig;

    private String dbSlaveConfig;

    private String separator;

    private DruidDataSource masterDataSource;

    private List<DruidDataSource> slaveDataSources;

    private AtomicLong currSlaveIndex;

    public DruidClusterDataSource(String dbDriver, int dbInitialSize,
                                  int dbMinIdle, int dbMaxActive,
                                  String dbMasterConfig, String dbSlaveConfig) {
        this(dbDriver, dbInitialSize, dbMinIdle, dbMaxActive, dbMasterConfig, dbSlaveConfig, " ");
    }

    public DruidClusterDataSource(String dbDriver, int dbInitialSize, int dbMinIdle,
                                  int dbMaxActive, String dbMasterConfig,
                                  String dbSlaveConfig, String separator) {
        if (StringUtils.isBlank(dbDriver)) {
            throw new IllegalArgumentException("dbDriver不能为空");
        }
        if (StringUtils.isBlank(dbMasterConfig)) {
            throw new IllegalArgumentException("dbMasterConfig不能为空");
        }
        if (StringUtils.isBlank(dbSlaveConfig)) {
            throw new IllegalArgumentException("dbSlaveConfig不能为空");
        }
        if (separator == null || separator.length() == 0) {
            throw new IllegalArgumentException("separator不能为空");
        }

        if (dbInitialSize <= 0) {
            dbInitialSize = 10;
        }

        if (dbMinIdle <= 0) {
            dbMinIdle = 10;
        }

        if (dbMaxActive <= 0) {
            dbMaxActive = 50;
        }

        this.dbDriver = dbDriver;
        this.dbInitialSize = dbInitialSize;
        this.dbMinIdle = dbMinIdle;
        this.dbMaxActive = dbMaxActive;
        this.dbMasterConfig = dbMasterConfig;
        this.dbSlaveConfig = dbSlaveConfig;
        this.separator = separator;
        this.currSlaveIndex = new AtomicLong(0);

        List<DruidDataSource> dataSources = resolveDbConfig(this.dbMasterConfig);
        if (dataSources.size() != 1) {
            throw new IllegalArgumentException("目前只支持一个master");
        }
        masterDataSource = dataSources.get(0);
        slaveDataSources = resolveDbConfig(this.dbSlaveConfig);


        Map<Object, Object> dataSourceMap = new HashMap<>(slaveDataSources.size() + 1);
        dataSourceMap.put(masterKey(), masterDataSource);
        for (int i = 0; i < slaveDataSources.size(); i++) {
            DataSource slaveDataSource = slaveDataSources.get(i);
            dataSourceMap.put(slaveKey(i), slaveDataSource);
        }
        setTargetDataSources(dataSourceMap);
    }

    private List<DruidDataSource> resolveDbConfig(String dbConfig) {
        String[] split = dbConfig.split(separator);

        if (split.length == 0 || (split.length % 3) != 0) {
            throw new IllegalArgumentException("[" + dbConfig + "]格式有误");
        }

        List<DruidDataSource> result = new ArrayList<>(split.length / 3);

        for (int i = 0; i < split.length; i += 3) {
            String dbUserName = split[i];
            String dbPasswd = split[i + 1];
            String dbUrl = split[i + 2];

            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setDriverClassName(this.dbDriver);
            dataSource.setInitialSize(this.dbInitialSize);
            dataSource.setMinIdle(this.dbMinIdle);
            dataSource.setMaxActive(this.dbMaxActive);
            dataSource.setUrl(dbUrl);
            dataSource.setUsername(dbUserName);
            dataSource.setPassword(dbPasswd);

            dataSource.setMaxWait(60000);
            dataSource.setTimeBetweenEvictionRunsMillis(60000);
            dataSource.setMinEvictableIdleTimeMillis(300000);
            dataSource.setValidationQuery("SELECT 'x'");
            dataSource.setTestWhileIdle(true);
            dataSource.setTestOnBorrow(false);
            dataSource.setTestOnReturn(false);

            result.add(dataSource);
        }

        return result;
    }

    private String masterKey() {
        return "master";
    }

    private String slaveKey(long slaveIndex) {
        return "slave" + slaveIndex;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String dataSouce = DynamicDataSourceHolder.getDataSouce();
        if (masterKey().equals(dataSouce)) {
            return masterKey();
        }

        if (slaveDataSources.size() == 1) {
            return slaveKey(0);
        }

        long currSlaveIndex = Math.abs(this.currSlaveIndex.getAndIncrement() % slaveDataSources.size());
        return slaveKey(currSlaveIndex);
    }


    private void init() throws SQLException {
        masterDataSource.init();

        for (DruidDataSource slaveDataSource : slaveDataSources) {
            slaveDataSource.init();
        }
    }

    private void close() {
        masterDataSource.close();

        for (DruidDataSource slaveDataSource : slaveDataSources) {
            slaveDataSource.close();
        }
    }
}
