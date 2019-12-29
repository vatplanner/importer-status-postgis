package org.vatplanner.importer.postgis.status.utils;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.time.Instant;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class TimeHelpersTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @DataProvider
    public static Object[][] dataProviderIsBetween() {
        return new Object[][]{
            // start = end
            {Instant.ofEpochSecond(1000000), Instant.ofEpochSecond(1000000), Instant.ofEpochSecond(1000000), true}, //
            {Instant.ofEpochSecond(1000001), Instant.ofEpochSecond(1000000), Instant.ofEpochSecond(1000000), false}, //
            {Instant.ofEpochSecond(999999), Instant.ofEpochSecond(1000000), Instant.ofEpochSecond(1000000), false}, //

            // before start
            {Instant.ofEpochSecond(1000000), Instant.ofEpochSecond(2000000), Instant.ofEpochSecond(3000000), false}, //

            // at start
            {Instant.ofEpochSecond(2000000), Instant.ofEpochSecond(2000000), Instant.ofEpochSecond(3000000), true}, //

            // in between (exclusive)
            {Instant.ofEpochSecond(2000001), Instant.ofEpochSecond(2000000), Instant.ofEpochSecond(3000000), true}, //
            {Instant.ofEpochSecond(2999999), Instant.ofEpochSecond(2000000), Instant.ofEpochSecond(3000000), true}, //

            // at end
            {Instant.ofEpochSecond(3000000), Instant.ofEpochSecond(2000000), Instant.ofEpochSecond(3000000), true}, //

            // after end
            {Instant.ofEpochSecond(3000001), Instant.ofEpochSecond(2000000), Instant.ofEpochSecond(3000000), false}, //
        };
    }

    @Test
    @UseDataProvider("dataProviderIsBetween")
    public void testIsBetween_validInput_returnsExpectedResult(Instant x, Instant start, Instant end, boolean expectedResult) {
        // Act
        boolean result = TimeHelpers.isBetween(x, start, end);

        // Assert
        assertThat(result, is(expectedResult));
    }

    @Test
    public void testIsBetween_nullForX_throwsIllegalArgumentException() {
        // Assert
        thrown.expect(IllegalArgumentException.class);

        // Act
        TimeHelpers.isBetween(null, Instant.ofEpochSecond(123), Instant.ofEpochSecond(321));
    }

    @Test
    public void testIsBetween_nullForStart_throwsIllegalArgumentException() {
        // Assert
        thrown.expect(IllegalArgumentException.class);

        // Act
        TimeHelpers.isBetween(Instant.ofEpochSecond(100), null, Instant.ofEpochSecond(321));
    }

    @Test
    public void testIsBetween_nullForEnd_throwsIllegalArgumentException() {
        // Assert
        thrown.expect(IllegalArgumentException.class);

        // Act
        TimeHelpers.isBetween(Instant.ofEpochSecond(100), Instant.ofEpochSecond(123), null);
    }

    @Test
    public void testIsBetween_endBeforeStart_throwsIllegalArgumentException() {
        // Assert
        thrown.expect(IllegalArgumentException.class);

        // Act
        TimeHelpers.isBetween(Instant.ofEpochSecond(100), Instant.ofEpochSecond(300), Instant.ofEpochSecond(200));
    }

    @Test
    @DataProvider({
        "10, 20, 10",
        "20, 20, 20",
        "30, 20, 20"
    })
    public void testMin_nonNull_returnsExpectedResult(int timestampA, int timestampB, int expectedTimestamp) {
        // Arrange
        Instant a = Instant.ofEpochSecond(timestampA);
        Instant b = Instant.ofEpochSecond(timestampB);
        Instant expectedResult = Instant.ofEpochSecond(expectedTimestamp);

        // Act
        Instant result = TimeHelpers.min(a, b);

        // Assert
        assertThat(result, is(equalTo(expectedResult)));
    }

    @Test
    @DataProvider({"10", "20"})
    public void testMin_firstParameterNull_returnsSecondParameter(int expectedTimestamp) {
        // Arrange
        Instant expectedResult = Instant.ofEpochSecond(expectedTimestamp);

        // Act
        Instant result = TimeHelpers.min(null, expectedResult);

        // Assert
        assertThat(result, is(equalTo(expectedResult)));
    }

    @Test
    @DataProvider({"10", "20"})
    public void testMin_secondParameterNull_returnsFirstParameter(int expectedTimestamp) {
        // Arrange
        Instant expectedResult = Instant.ofEpochSecond(expectedTimestamp);

        // Act
        Instant result = TimeHelpers.min(expectedResult, null);

        // Assert
        assertThat(result, is(equalTo(expectedResult)));
    }

    @Test
    public void testMin_bothParameterNull_returnsNull() {
        // Act
        Instant result = TimeHelpers.min(null, null);

        // Assert
        assertThat(result, is(nullValue()));
    }
}
