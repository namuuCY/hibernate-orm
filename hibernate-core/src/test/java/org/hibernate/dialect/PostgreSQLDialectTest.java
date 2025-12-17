/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;


import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@RequiresDialect(PostgreSQLDialect.class)
@DomainModel(annotatedClasses = {
		PostgreSQLDialectTest.BaseEntity.class,
		PostgreSQLDialectTest.InetEntity.class,
		PostgreSQLDialectTest.EmptyEntity.class
})
@SessionFactory
@ServiceRegistry(
		settings = @Setting(
				name = AvailableSettings.STATEMENT_INSPECTOR,
				value = "org.hibernate.dialect.PostgreSQLDialectTest$SqlSpy"
		)
)
public class PostgreSQLDialectTest {

	public static final List<String> SQL_LOG = new ArrayList<>();

	@BeforeEach
	protected void setupTest(SessionFactoryScope scope) {
		SQL_LOG.clear();
		scope.inTransaction(
				(session) -> {
					session.createNativeQuery(
									"insert " +
											"into inet_entity (id, ipAddress) " +
											"values (1, '192.168.0.1'::inet)"
							)
							.executeUpdate();
					session.persist( new EmptyEntity() );
				}
		);
		SQL_LOG.clear();
	}

	@Test
	@JiraKey(value = "HHH-19974")
	public void testCastNullString(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					String entityName = BaseEntity.class.getName();

					List<BaseEntity> results = session.createQuery(
							"select r from " + entityName + " r", BaseEntity.class
					).list();

					boolean foundCast = false;
					for ( String sql : SQL_LOG ) {
						if ( sql.contains( "cast(null as inet)" ) ) {
							foundCast = true;
							break;
						}
					}

					Assertions.assertTrue( foundCast, "must contains 'cast(null as inet)' clause." );
				}
		);
	}


	@Entity(name = "root_entity")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static abstract class BaseEntity {
		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity(name = "inet_entity")
	public static class InetEntity extends BaseEntity {

		@Column(columnDefinition = "inet")
		private String ipAddress;

		public InetEntity() {
		}
	}

	@Entity(name = "empty_entity")
	public static class EmptyEntity extends BaseEntity {
	}

	public static class SqlSpy implements StatementInspector {
		@Override
		public String inspect(String sql) {
			SQL_LOG.add( sql.toLowerCase() );
			return sql;
		}
	}

}
