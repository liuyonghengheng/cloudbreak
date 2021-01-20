package com.sequenceiq.cloudbreak.service.image;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class AdvertisedImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdvertisedImageFilter.class);

    public ImageFilterResult getAdvertisedImages(List<Image> availableImages) {
        List<Image> images = availableImages.stream().filter(Image::isAdvertised).collect(toList());
        LOGGER.debug("{} images found by the advertised flag", images.size());
        String message = String.format("%d images found by the advertised flag", images.size());

        return new ImageFilterResult(new Images(null, images, null), message);
    }
}
