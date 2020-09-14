
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class TestNGTest {

	@Test
	public void name0a() {
		assertTrue(true);
		assertTrue(true, "true");
		Assert.assertTrue(true, "true");
		assertFalse(false);
		assertFalse(false, "false");
		Assert.assertFalse(false, "false");
		assertEquals(new Object[][] {}, new Object[][] {}); //TODO
		assertEquals(new Object[] {}, new Object[] {}, "text");
		assertEquals(25, 28);
		assertEquals(25, 28, "text");
		assertNotEquals(25, 28);
		assertNotEquals(25, 28, "text");
		assertSame("text", 25);
		assertNotSame("text", 25);
		assertNull(this);
		assertNotNull(this);
		Assert.assertNotNull(this);
		assertNotNull(this, "text");
		fail();
		fail("message");
		fail("message", new NullPointerException());
		Assert.fail();
		Assert.fail("message");
		Assert.fail("message", new NullPointerException());
	}

	@Test()
	public void name1() {

	}

	@Test(timeOut = 20L)
	public void name2() {
		name21();
	}

	@Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*invalid.*")
	public void name21() {
		name2();
	}

	@Test(enabled = false)
	public void name01() {

	}


	@BeforeMethod
	public void before() {

	}

	@BeforeClass
	public static void before1() {

	}

	@AfterMethod
	public void after() {

	}

	@AfterClass
	public static void after1() {

	}
}
