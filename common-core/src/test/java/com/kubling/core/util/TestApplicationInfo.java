/*
 * This file was modified as part of the Kubling project.
 */

package com.kubling.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestApplicationInfo {

    @Test
    public void testReleaseVersionComponents() {
        ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
        String[] components = applicationInfo.getReleaseNumber().split("\\.");

        assertEquals(Integer.parseInt(components[0]), applicationInfo.getMajorReleaseVersion());
        assertEquals(Integer.parseInt(components[1]), applicationInfo.getMinorReleaseVersion());
    }
}
