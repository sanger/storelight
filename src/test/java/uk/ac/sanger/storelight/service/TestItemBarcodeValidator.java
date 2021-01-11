package uk.ac.sanger.storelight.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link ItemBarcodeValidator}
 * @author dr6
 */
public class TestItemBarcodeValidator {
    ItemBarcodeValidator itemBarcodeValidator;

    @BeforeEach
    void setup() {
        itemBarcodeValidator = new ItemBarcodeValidator();
    }

    @ParameterizedTest
    @MethodSource("validateItemBarcodesArguments")
    public void testValidateItemBarcodes(List<String> barcodes, String expectedErrorMessage) {
        if (expectedErrorMessage==null) {
            itemBarcodeValidator.validateItemBarcodes(barcodes.stream());
        } else {
            assertThat(assertThrows(IllegalArgumentException.class, () -> itemBarcodeValidator.validateItemBarcodes(barcodes.stream())))
                    .hasMessage(expectedErrorMessage);
        }
    }

    static Stream<Arguments> validateItemBarcodesArguments() {
        return Arrays.stream(new String[][] {
                { null },
                { "CGAP-ABC", "X11", "123boop", null },
                { null, "CGAP-ABC", "Null given as item barcode." },
                { "CGAP-ABC", "X11", "cgap-abc", "123boop", "123BOOP", "Barcodes repeated: [\"cgap-abc\", \"123BOOP\"]."},
                { "X11", " CGAP-ABC", "123boop ", "Barcodes have surrounding whitespace: [\" CGAP-ABC\", \"123boop \"]."},
                { "", "X", "CGAP-ABC", "Barcodes too short: [\"\", \"X\"]." },
                { "CGAP-ABC", "HappyBirthdayToYouHappyBirthdayToYouHappyBirthdayDearAngelinaJolieHappyBirthdayToYou", "ForShesAJollyGoodFellowForShesAJollyGoodFellowForShesAJollyGoodFellow-AndSoSayAllOfUs",
                  "Barcodes too long: [\"HappyBirthdayToYouHappyBirthdayToYouHappyBirthdayDearAngelinaJolieHappyBirthdayToYou\", \"ForShesAJollyGoodFellowForShesAJollyGoodFellowForShesAJollyGoodFellow-AndSoSayAllOfUs\"]."},
                { "CGAP-ABC", "STO-ABC", "sto-123", "Barcodes cannot start with STO-: [\"STO-ABC\", \"sto-123\"]."},
                { null, "CGAP-ABC", "cgap-abc", " XYZ ", "X", "AndIfOneGreenBottleShouldAccidentallyFallTherellBeNineGreenBottlesHangingOnTheWall", "sto-x",
                  "Null given as item barcode. Barcode repeated: [\"cgap-abc\"]. Barcode too short: [\"X\"]. " +
                          "Barcode too long: [\"AndIfOneGreenBottleShouldAccidentallyFallTherellBeNineGreenBottlesHangingOnTheWall\"]. " +
                          "Barcode has surrounding whitespace: [\" XYZ \"]. Barcodes cannot start with STO-: [\"sto-x\"]."},
        }).map(strings ->
            Arguments.of(Arrays.asList(Arrays.copyOfRange(strings, 0, strings.length-1)),
                    strings[strings.length-1])
        );
    }
}
