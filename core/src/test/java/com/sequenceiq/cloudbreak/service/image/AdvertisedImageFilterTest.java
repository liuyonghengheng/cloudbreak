package com.sequenceiq.cloudbreak.service.image;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdvertisedImageFilterTest {

    private static final String ADVERTISED_IMAGE_ID = "16ad7759-83b1-42aa-aadf-0e3a6e7b5444";

    private static final String NON_ADVERTISED_IMAGE_ID = "36cbacf7-f7d4-4875-61f9-548a0acd3512";

    private AdvertisedImageFilter victim;

    @BeforeEach
    public void initTests() {
        victim = new AdvertisedImageFilter();
    }

    @Test
    public void shouldReturnImagesWithAdvertisedFlag() {
        List<Image> images = Arrays.asList(createImage(ADVERTISED_IMAGE_ID, true), createImage(NON_ADVERTISED_IMAGE_ID, false));
        List<Image> actual = victim.getAdvertisedImages(images).getAvailableImages().getCdhImages();

        assertTrue(actual.stream().anyMatch(i -> i.getUuid().equals(ADVERTISED_IMAGE_ID)));
        assertFalse(actual.stream().anyMatch(i -> i.getUuid().equals(NON_ADVERTISED_IMAGE_ID)));
    }

    private Image createImage(String imageId, boolean advertised) {
        return new Image(null, null, null, null, imageId, null, null, null, null, null, null, null, null, null, advertised);
    }
}
