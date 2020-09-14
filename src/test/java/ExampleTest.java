import org.junit.jupiter.api.Assertions;
import org.testng.annotations.Test;

public class ExampleTest {

	@Test(expectedExceptions = NullPointerException.class)
	public void test() {
		Assertions.assertEquals("expected", "actual");
		Assertions.assertEquals("expected", "actual", "assertion with message");
		Assertions.assertNotNull("notNull");
		System.out.println("hello");
	}

	@Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*invalid.*")
	public void testExpectedMessage() {
		Assertions.assertEquals("expected", "actual");
		Assertions.assertEquals("expected", "actual", "assertion with message");
		System.out.println("hello");
	}

	@Test(expectedExceptionsMessageRegExp = ".*invalid.*")
	public void testExpectedMessageOnly() {
		Assertions.assertEquals("expected", "actual");
		Assertions.assertEquals("expected", "actual", "assertion with message");
		System.out.println("hello");
	}

	@Test(expectedExceptions = {NullPointerException.class, IllegalArgumentException.class})
	public void testManyExpected() {
		Assertions.assertEquals("expected", "actual");
		Assertions.assertEquals("expected", "actual", "assertion with message");
		System.out.println("hello");
	}
}
