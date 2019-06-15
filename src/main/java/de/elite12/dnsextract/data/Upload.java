package de.elite12.dnsextract.data;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class Upload {
    @NonNull
    private String filename;
    @NonNull
    private int fullsize;

    private Map<Integer, byte[]> parts = new HashMap<>();
    private int currentsize = 0;
}
