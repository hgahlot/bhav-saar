/**
 * 
 */
package org.hgahlot.sa.jdbc;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author hgahlot
 *
 */
public class TemplateAccessor {
	private static final String PATH_TO_BEANS_XML = "beans.xml";
	private static JdbcTemplate jdbcTemplate;

	public static JdbcTemplate getJdbcTemplateForBean(String beanId) {
		Resource resource = new ClassPathResource(PATH_TO_BEANS_XML);
		BeanFactory factory = new XmlBeanFactory(resource);
		JdbcDataSource bean = (JdbcDataSource)factory.getBean(beanId);
		jdbcTemplate = new JdbcTemplate(bean.getDataSource());
		
		return jdbcTemplate;
	}
}
