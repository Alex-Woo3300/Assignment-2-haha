import java.util.*;

public class DLBHotspotDetector implements HotspotDetector {

    // ----------------------------
    // Interface methods (TODO)
    // ----------------------------

    @Override
    public void addLeakedPassword(String leakedPassword, int minN, int maxN) {

    }

    @Override
    public Set<Hotspot> hotspotsIn(String candidatePassword) {
        return new LinkedHashSet<>();
    }
}
