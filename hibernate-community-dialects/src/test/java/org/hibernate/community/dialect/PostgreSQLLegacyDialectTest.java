/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;

// Even though we are testing PostgreSQLLegacyDialect, the test environment runs a standard PostgreSQL instance,
// which is detected as PostgreSQLDialect. To prevent the test from being skipped, we require PostgreSQLDialect.
// The actual PostgreSQLLegacyDialect is enforced internally via the @Setting annotation.
@RequiresDialect(org.hibernate.dialect.PostgreSQLDialect.class)
@DomainModel(annotatedClasses = {
		PostgreSQLLegacyDialectTest.BaseEntity.class,
		PostgreSQLLegacyDialectTest.InetEntity.class,
		PostgreSQLLegacyDialectTest.EmptyEntity.class
})
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(
		settings = {
				@Setting(
						name = AvailableSettings.DIALECT,
						value = "org.hibernate.community.dialect.PostgreSQLLegacyDialect"
				)
		}

)
public class PostgreSQLLegacyDialectTest {

	@BeforeEach
	protected void setupTest(SessionFactoryScope scope) {
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
				}
		);

		SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		assertThat( inspector.getSqlQueries() )
				.as("must contains 'cast(null as inet)' clause." )
				.anyMatch(sql -> sql.toLowerCase().contains("cast(null as inet)"));
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

		@JdbcTypeCode(SqlTypes.INET)
		private String ipAddress;

		public InetEntity() {
		}
	}

	@Entity(name = "empty_entity")
	public static class EmptyEntity extends BaseEntity {
	}


}
