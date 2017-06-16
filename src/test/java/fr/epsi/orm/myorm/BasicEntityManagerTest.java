package fr.epsi.orm.myorm;

import com.zaxxer.hikari.HikariDataSource;
import fr.epsi.orm.myorm.annotation.Entity;
import fr.epsi.orm.myorm.annotation.Id;
import fr.epsi.orm.myorm.persistence.BasicEntityManager;
import fr.epsi.orm.myorm.persistence.EntityManager;
import fr.epsi.orm.myorm.lib.sample.User;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Created by fteychene on 14/05/17.
 */
public class BasicEntityManagerTest {

    static DataSource embeddedDatasource;
    EntityManager em;

    @BeforeClass
    public static void beforeClass() {
        embeddedDatasource =  new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.HSQL)
                    .addScripts("init-db.sql")
                    .build();
    }

    @Before
    public void beforeTest() {
        HikariDataSource hikariDatasource = new HikariDataSource();
        hikariDatasource.setDataSource(embeddedDatasource);
        em = BasicEntityManager.create(hikariDatasource, Stream.of(User.class).collect(Collectors.toSet()));
    }

    @Test
    public void testFind() {
        User expected = new User();
        expected.setId(2L);
        expected.setFirstName("Robert");
        expected.setLastName("Martin");
        expected.setEmail("uncle@bob.com");
        expected.setBirthDate(LocalDate.of(1962, 4,17));

        Optional<User> actual = null;
        try {
            actual = em.find(User.class, 2L);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (!actual.isPresent()) {
            fail("User not found in db");
        }

        assertEquals("User is not coherent with db", expected, actual.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindReject() {
        try {
            em.find(InvalidUserNoEntity.class, null);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFindAll() {
        User expected = new User();
        expected.setId(0L);
        expected.setFirstName("Linus");
        expected.setLastName("Torvald");
        expected.setEmail("linux.torvald@linux.org");
        expected.setBirthDate(LocalDate.of(1969,12, 28));
        User expected1 = new User();
        expected1.setId(1L);
        expected1.setFirstName("Brian");
        expected1.setLastName("Goetz");
        expected1.setEmail("brian.goetz@oracle.com");
        expected1.setBirthDate(LocalDate.of(1970, 11,22));
        User expected2 = new User();
        expected2.setId(2L);
        expected2.setFirstName("Robert");
        expected2.setLastName("Martin");
        expected2.setEmail("uncle@bob.com");
        expected2.setBirthDate(LocalDate.of(1962,4, 17));

        List<User> expectedStream = Arrays.asList(expected, expected1, expected2);
        try {
            assertEquals(expectedStream, em.findAll(User.class));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindAllReject() {
        try {
            em.findAll(InvalidUserNoEntity.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInsert() {
        User user = new User();
        user.setId(0L);
        user.setFirstName("Test");
        user.setLastName(null);
        user.setEmail("test.null@gmail.com");
        user.setBirthDate(LocalDate.of(1990, 4,23));

        Optional<User> persistedUser = null;
        try {
            persistedUser = em.save(user);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        assertTrue(persistedUser.isPresent());
        user = persistedUser.get();
        assertNotEquals(Long.valueOf(0), user.getId());
        assertEquals(Integer.valueOf(0), user.getConnectionCount());

        User actual = null;
        try {
            actual = em.find(User.class, user.getId()).orElseThrow(() -> new AssertionError("Error while retrieving new User inserted"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        assertEquals(user, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertReject() {
        try {
            em.save(new InvalidUserNoEntity());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDelete() {
        User user = new User();
        user.setFirstName("Francois");
        user.setLastName("Teychene");
        user.setEmail("francois.teychene@gmail.com");

        Optional<User> persistedUser = null;
        try {
            persistedUser = em.save(user);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (!persistedUser.isPresent()) fail("Error during insertion");

        boolean deleted = em.delete(persistedUser.get());
        assertTrue(deleted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteReject() {
        em.delete(new InvalidUserNoEntity());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheck1() {
        HikariDataSource hikariDatasource = new HikariDataSource();
        hikariDatasource.setDataSource(embeddedDatasource);
        BasicEntityManager.create(hikariDatasource, Stream.of(InvalidUserNoEntity.class).collect(Collectors.toSet()));
    }
    private static class InvalidUserNoEntity {

    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheck2() {
        HikariDataSource hikariDatasource = new HikariDataSource();
        hikariDatasource.setDataSource(embeddedDatasource);
        BasicEntityManager.create(hikariDatasource, Stream.of(InvalidUserNoId.class).collect(Collectors.toSet()));
    }
    @Entity
    private static class InvalidUserNoId {

    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheck3() {
        HikariDataSource hikariDatasource = new HikariDataSource();
        hikariDatasource.setDataSource(embeddedDatasource);
        BasicEntityManager.create(hikariDatasource, Stream.of(InvalidUserTooManyId.class).collect(Collectors.toSet()));
    }
    @Entity
    private static class InvalidUserTooManyId {
        @Id
        private Integer id1;
        @Id
        private Integer id2;
    }
}
