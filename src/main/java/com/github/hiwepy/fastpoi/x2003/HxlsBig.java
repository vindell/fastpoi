/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.hiwepy.fastpoi.x2003;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.apache.poi.poifs.filesystem.POIFSFileSystem;



public class HxlsBig extends HxlsAbstract{
	
	public static void main(String[] args) throws Exception {
		// XLS2CSVmra xls2csv = new XLS2CSVmra(args[0], minColumns);
		HxlsBig xls2csv = new HxlsBig("E:/up.xls","hxls_temp");
		xls2csv.process();
		xls2csv.close();
	}
	
	public HxlsBig(POIFSFileSystem fs, PrintStream output,String tableName)
			throws SQLException {
		super(fs);
		this.conn = getNew_Conn();
		this.statement = conn.createStatement();
		this.tableName = tableName;
	}

	public HxlsBig(String filename,String tableName) throws IOException,
			FileNotFoundException, SQLException {
		this(new POIFSFileSystem(new FileInputStream(filename)), System.out,tableName);
	}

	private Connection conn = null;
	private Statement statement = null;
	private PreparedStatement newStatement = null;

	private String tableName = "temp_table";
	private boolean create = true;
//	private int sheetIndex = 0;
	
	public void optRows(int sheetIndex,int curRow, List<String> rowlist) throws SQLException {
		if (curRow == 0 && sheetIndex == 0 ) {
			StringBuffer preSql = new StringBuffer("insert into " + tableName
					+ " values(");
			StringBuffer table = new StringBuffer("create table " + tableName
					+ "(");
			int c = rowlist.size();
			for (int i = 0; i < c; i++) {
				preSql.append("?,");
				table.append(rowlist.get(i));
				table.append("  varchar2(100) ,");
			}

			table.deleteCharAt(table.length() - 1);
			preSql.deleteCharAt(preSql.length() - 1);
			table.append(")");
			preSql.append(")");
			if (create) {
				statement = conn.createStatement();
				try{
					statement.execute("drop table "+tableName);
				}catch(Exception e){
					
				}finally{
					System.out.println("表 "+tableName+" 删除成功");
				}
				if (!statement.execute(table.toString())) {
					System.out.println("创建表 "+tableName+" 成功");
					// return;
				} else {
					System.out.println("创建表 "+tableName+" 失败");
					return;
				}
			}
			conn.setAutoCommit(false);
			newStatement = conn.prepareStatement(preSql.toString());

		}else if(curRow > 0) {
			// 一般行
			int col = rowlist.size();
			for (int i = 0; i < col; i++) {
				newStatement.setString(i + 1, rowlist.get(i).toString());
			}
			newStatement.addBatch();
			if (curRow % 1000 == 0) {
				newStatement.executeBatch();
				conn.commit();
			}
		}
	}
	
    private static Connection getNew_Conn() {
        Connection conn = null;
        Properties props = new Properties();
        FileInputStream fis = null;

        try {
            fis = new FileInputStream("D:/database.properties");
            props.load(fis);
            DriverManager.registerDriver(null);//new oracle.jdbc.driver.OracleDriver()
            StringBuffer jdbcURLString = new StringBuffer();
            jdbcURLString.append("jdbc:oracle:thin:@");
            jdbcURLString.append(props.getProperty("host"));
            jdbcURLString.append(":");
            jdbcURLString.append(props.getProperty("port"));
            jdbcURLString.append(":");
            jdbcURLString.append(props.getProperty("database"));
            conn = DriverManager.getConnection(jdbcURLString.toString(), props
                    .getProperty("user"), props.getProperty("password"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return conn;
    }
    
	public int close() {
		try {
			newStatement.executeBatch();
			conn.commit();
			System.out.println("数据写入完毕");
			this.newStatement.close();
			this.statement.close();
			this.conn.close();
			return 1;
		} catch (SQLException e) {
			return 0;
		}
	}

}
