package org.vatplanner.importer.postgis.status.entities;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import org.vatplanner.importer.postgis.status.DirtyEntityTracker;

@RunWith(DataProviderRunner.class)
public class RelationalFlightPlanTest {

    @DataProvider
    public static Object[][] dataProviderNormalizeAirportCodes() {
        return new Object[][]{
            // empty/null
            {null, null}, //
            {"", ""}, //

            // shorter than ICAO
            {"a", "A"}, //
            {"1", "1"}, //

            // exact ICAO,
            {"ABCD", "ABCD"}, //
            {"AB2D", "AB2D"}, //

            // mixed case
            {"aBCd", "ABCD"}, //

            // too long
            {"ABCDEFGH", "ABCDEFGH"}, //

            // trim
            {"  Ab2D ", "AB2D"}, //
            {"   ", ""}, //

            // removal after first special character
            {"EDDT SID ROUTE HERE", "EDDT"}, //
            {"EDDT/WHATEVER", "EDDT"}, //
            {"EDDT.ABCD", "EDDT"}, //

            // special characters at start
            {".WHAT", ""}, //
            {"/r/ this clearly is no airport code", ""}, //
        };
    }

    @Test
    @UseDataProvider("dataProviderNormalizeAirportCodes")
    public void testNormalizeAirportCode_definedInput_returnsExpectedResult(String input, String expectedResult) {
        // Arrange
        DirtyEntityTracker mockTracker = mock(DirtyEntityTracker.class);
        RelationalFlightPlan flightPlan = new RelationalFlightPlan(mockTracker, null, 0);

        // Act
        String result = flightPlan.normalizeAirportCode(input);

        // Assert
        assertThat(result, is(equalTo(expectedResult)));
    }
}
