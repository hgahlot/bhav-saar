/**
 * 
 */
package org.hgahlot.sa.jdbc;

import javax.sql.DataSource;

/**
 * @author hgahlot
 *
 */
public class JdbcDataSource {
	private DataSource dataSource;

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
}
