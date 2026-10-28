package br.senai.plataformadigital.portal.api.impact;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ImpactInputTests {
    @Test
    void nullVersionApprovalAndSnapshotInputsAreRejected() {
        assertThrows(NullPointerException.class, () -> new ImpactVersion(null, "v1", "source"));
        assertThrows(NullPointerException.class, () -> new ImpactVersion("impact", null, "source"));
        assertThrows(NullPointerException.class, () -> new ImpactVersion("impact", "v1", null));
        assertThrows(NullPointerException.class, () -> new ImpactApproval(null, "v1"));
        assertThrows(NullPointerException.class, () -> new ImpactApproval("impact", null));
        assertThrows(NullPointerException.class,
                () -> new ImpactPublicationSnapshot(null, Optional.empty(), false, false));
        assertThrows(NullPointerException.class,
                () -> new ImpactPublicationSnapshot(new ImpactVersion("impact", "v1", "source"), null, false, false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void blankReferencesCannotRepresentVersionsOriginsOrApprovals(String value) {
        assertThrows(IllegalArgumentException.class, () -> new ImpactVersion(value, "v1", "source"));
        assertThrows(IllegalArgumentException.class, () -> new ImpactVersion("impact", value, "source"));
        assertThrows(IllegalArgumentException.class, () -> new ImpactVersion("impact", "v1", value));
        assertThrows(IllegalArgumentException.class, () -> new ImpactApproval(value, "v1"));
        assertThrows(IllegalArgumentException.class, () -> new ImpactApproval("impact", value));
    }
}
