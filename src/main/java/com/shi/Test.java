package com.shi;

import cn.smallbun.screw.core.Configuration;
import cn.smallbun.screw.core.engine.EngineConfig;
import cn.smallbun.screw.core.engine.EngineFileType;
import cn.smallbun.screw.core.engine.EngineTemplateType;
import cn.smallbun.screw.core.execute.DocumentationExecute;
import cn.smallbun.screw.core.process.ProcessConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 史杰
 */
public class Test {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        //documentGeneration("C:\\Users\\史杰\\Desktop\\test");

        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:mysql://192.168.2.81:3306/ebmp_equipment_management", "ebmp_equipment_management", "123456789");

        List<Map<String, Object>> tableList = getTableList(conn,"ebmp_equipment_management");
        conn.close();

        FtUtil ftUtil = new FtUtil();
        Map<String, List> map = new HashMap<>(15);
        map.put("table", tableList);

        ftUtil.generateFile("/", "moban.xml", map, "E:\\test", "test.doc");

    }

    /**
     * 文档生成
     */
    static void documentGeneration(String fileOutputDir) {
        //数据源
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl("jdbc:mysql://192.168.2.81:3306/ebmp_equipment_management");
        hikariConfig.setUsername("ebmp_equipment_management");
        hikariConfig.setPassword("123456789");
        //设置可以获取tables remarks信息
        hikariConfig.addDataSourceProperty("useInformationSchema", "true");
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setMaximumPoolSize(5);
        DataSource dataSource = new HikariDataSource(hikariConfig);
        //生成配置
        EngineConfig engineConfig = EngineConfig.builder()
                //生成文件路径
                .fileOutputDir(fileOutputDir)
                //打开目录
                .openOutputDir(true)
                //文件类型
                .fileType(EngineFileType.WORD)
                //生成模板实现
                .produceType(EngineTemplateType.freemarker)
                //自定义文件名称
                .fileName("装备").build();

        //忽略表
        ArrayList<String> ignoreTableName = new ArrayList<>();
        ignoreTableName.add("test_user");
        ignoreTableName.add("test_group");
        //忽略表前缀
        ArrayList<String> ignorePrefix = new ArrayList<>();
        ignorePrefix.add("test_");
        //忽略表后缀
        ArrayList<String> ignoreSuffix = new ArrayList<>();
        ignoreSuffix.add("_test");
        ProcessConfig processConfig = ProcessConfig.builder()
                //指定生成逻辑、当存在指定表、指定表前缀、指定表后缀时，将生成指定表，其余表不生成、并跳过忽略表配置
                //根据名称指定表生成
                .designatedTableName(new ArrayList<>())
                //根据表前缀生成
                .designatedTablePrefix(new ArrayList<>())
                //根据表后缀生成
                .designatedTableSuffix(new ArrayList<>())
                //忽略表名
                .ignoreTableName(ignoreTableName)
                //忽略表前缀
                .ignoreTablePrefix(ignorePrefix)
                //忽略表后缀
                .ignoreTableSuffix(ignoreSuffix).build();
        //配置
        Configuration config = Configuration.builder()
                //版本
                .version("1.0.0")
                //描述
                .description("数据库设计文档生成")
                //数据源
                .dataSource(dataSource)
                //生成配置
                .engineConfig(engineConfig)
                //生成配置
                .produceConfig(processConfig)
                .build();
        //执行生成
        new DocumentationExecute(config).execute();
    }

    /**
     * 获取数据库中所有表的表名，并添加到列表结构中。
     * @param conn 数据库链接
     * @param s SCHEMA
     * @return 表 列
     */
    private static List<Map<String, Object>> getTableList(Connection conn,String s) throws SQLException {
        List<Map<String, Object>> tableList = new ArrayList<>();

        String sql =
                "select * from information_schema.`TABLES` where TABLE_SCHEMA = '"+s+"'";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Map<String, Object> map = new HashMap<>(15);
            String TABLE_NAME = rs.getString("TABLE_NAME");
            String COMMENTS = rs.getString("TABLE_COMMENT");
            map.put("TABLE_NAME", TABLE_NAME.toUpperCase());
            map.put("COMMENTS", COMMENTS == null ? "" : COMMENTS);

            //获取列
            List<Map<String, String>> columnList = getColumnList(conn, TABLE_NAME,s);
            map.put("COLUMNS", columnList);

            //这里是过滤特殊的表，比如只生成SYS开头的表
            // if (TABLE_NAME.startsWith("SYS")) {
            //     tableList.add(map);
            // }
            tableList.add(map);
            System.out.println("TABLE_NAME ==>" + TABLE_NAME + "  COMMENTS==>" + COMMENTS);
        }
        rs.close();
        ps.close();
        return tableList;
    }

    /**
     * 获取数据表中所有列的列名，并添加到列表结构中。
     * @param conn 数据库链接
     * @param tableName 表名
     * @param schemaData schema
     * @return 列
     * @throws SQLException 异常
     */
    private static List<Map<String, String>> getColumnList(Connection conn, String tableName, String schemaData)
            throws SQLException {

        List<Map<String, String>> columnList = new ArrayList<>();

        String sql;
        sql = "SELECT\n" +
                "\tC.TABLE_SCHEMA ,\n" +
                "\tT.TABLE_NAME,\n" +
                "\tT.TABLE_COMMENT,\n" +
                "\tC.COLUMN_NAME,\n" +
                "\tC.COLUMN_COMMENT,\n" +
                "\tC.COLUMN_DEFAULT,\n" +
                "\tC.IS_NULLABLE,\n" +
                "\tC.DATA_TYPE,\n" +
                "\tC.CHARACTER_MAXIMUM_LENGTH,\n" +
                "\tC.NUMERIC_SCALE,\n" +
                "\tC.COLUMN_TYPE,\n" +
                "\tC.COLUMN_KEY,C.NUMERIC_PRECISION,\n" +
                "\tC.EXTRA\n" +
                "FROM\n" +
                "\tinformation_schema.`TABLES` T\n" +
                "LEFT JOIN information_schema.`COLUMNS` C ON T.TABLE_NAME = C.TABLE_NAME\n" +
                "AND T.TABLE_SCHEMA = C.TABLE_SCHEMA\n" +
                "WHERE\n" +
                "\tT.TABLE_SCHEMA = '"+schemaData+"'" +
                "\t\tand T.TABLE_NAME = '"+tableName+"'" +
                ";";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Map<String, String> map = new HashMap<>(15);
            // 列名
            String COLUMN_NAME = rs.getString("COLUMN_NAME");
            // 类型 varchar(32)
            String DATA_TYPE = rs.getString("COLUMN_TYPE");
            // 长度
            String DATA_LENGTH = rs.getString("CHARACTER_MAXIMUM_LENGTH");
            // 默认值
            String DATA_DEFAULT = rs.getString("COLUMN_DEFAULT");
            // 是否可为空
            String NULLABLE = rs.getString("IS_NULLABLE");
            // 注释
            String COMMENTS = rs.getString("COLUMN_COMMENT");
            //主键
            String PRIMARY_KEY = rs.getString("COLUMN_KEY");
            // 备注
            String EXTRA = rs.getString("EXTRA");
            // 精度
            String NUMERIC_SCALE = rs.getString("NUMERIC_SCALE");

            // 类型 varchar
            String DATA_TYPE_FLAG = rs.getString("DATA_TYPE");
            String NUMERIC_PRECISION = rs.getString("NUMERIC_PRECISION");
            map.put("COLUMN_NAME", COLUMN_NAME.toUpperCase());
            map.put("DATA_TYPE", DATA_TYPE.toUpperCase());
            if("varchar".equals(DATA_TYPE_FLAG)){
                map.put("CHARACTER_MAXIMUM_LENGTH", DATA_LENGTH==null?"":DATA_LENGTH);
            }else{
                map.put("CHARACTER_MAXIMUM_LENGTH", NUMERIC_PRECISION==null?"":NUMERIC_PRECISION);
            }
            map.put("COLUMN_DEFAULT", DATA_DEFAULT == null || "NULL".equals(DATA_DEFAULT)? "" : DATA_DEFAULT);
            map.put("NULLABLE", "NO".equals(NULLABLE) ? "" : "是");
            map.put("COLUMN_COMMENT", COMMENTS == null ? "" : COMMENTS);
            map.put("COLUMN_KEY", "PRI".equals(PRIMARY_KEY) ? "是" : "");
            map.put("EXTRA", EXTRA == null ? "" : EXTRA);
            map.put("NUMERIC_SCALE", NUMERIC_SCALE == null ? "" : NUMERIC_SCALE);
            // 约束，暂时没有查询
            map.put("CONSTRAINT", "");
            columnList.add(map);

            System.out.println("COLUMN_NAME ==>" + COLUMN_NAME + "  DATA_TYPE==>" + DATA_TYPE + "  DATA_LENGTH==>" + DATA_LENGTH + " NULLABLE==>" + NULLABLE + "  COMMENTS==>" + COMMENTS + " PRIMARY_KEY==>" + PRIMARY_KEY);
        }
        rs.close();
        ps.close();
        return columnList;
    }
}
