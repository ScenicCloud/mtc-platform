package com.mtc.sample;

import com.mtc.sample.dto.SpaceVO;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Service
public class SampleService {

    // 占位数据，硬编码，不查库
    private static final List<SpaceVO> SPACES = Arrays.asList(
            new SpaceVO(1L, "示例空间 A", OffsetDateTime.of(2026, 9, 20, 10, 0, 0, 0, ZoneOffset.ofHours(8))),
            new SpaceVO(2L, "示例空间 B", OffsetDateTime.of(2026, 9, 25, 14, 30, 0, 0, ZoneOffset.ofHours(8)))
    );

    public List<SpaceVO> getSpaces() {
        return SPACES;
    }

    public boolean spaceExists(Long spaceId) {
        return SPACES.stream().anyMatch(s -> s.getId().equals(spaceId));
    }
}
