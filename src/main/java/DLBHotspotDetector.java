import java.util.*;

public class DLBHotspotDetector implements HotspotDetector {

    private class DLBNode {
        char ch;
        DLBNode child;
        DLBNode sibling;
        boolean isTerminal;

        int freq;
        int docFreq;
        int beginCount;
        int middleCount;
        int endCount;

        DLBNode(char ch) {
            this.ch = ch;
        }
    }

    private DLBNode root;

    public DLBHotspotDetector() {
        root = null;
    }

    @Override
    public void addLeakedPassword(String leakedPassword, int minN, int maxN) {

        if (leakedPassword == null)
            throw new IllegalArgumentException();

        if (minN < 1 || maxN < minN)
            throw new IllegalArgumentException();

        leakedPassword = leakedPassword.trim();
        if (leakedPassword.length() == 0) return;

        int len = leakedPassword.length();

        Set<String> seen = new HashSet<>();

        int n = minN;

        while (n <= maxN) {

            int i = 0;

            while (i <= len - n) {

                String hotspot = leakedPassword.substring(i, i + n);

                boolean atBegin = false;
                boolean atEnd = false;
                boolean atMiddle = false;

                if (i == 0) {
                    atBegin = true;
                } else if (i + n == len) {
                    atEnd = true;
                } else {
                    atMiddle = true;
                }

                boolean firstOccurrence = false;

                if (!seen.contains(hotspot)) {
                    seen.add(hotspot);
                    firstOccurrence = true;
                }

                root = insertHelper(root, hotspot, 0,
                        atBegin, atMiddle, atEnd, firstOccurrence);

                i++;
            }

            n++;
        }
    }

    private DLBNode insertHelper(DLBNode node, String hotspot, int index,
                                 boolean atBegin, boolean atMiddle, boolean atEnd,
                                 boolean firstOccurrence) {

        char ch = hotspot.charAt(index);

        if (node == null) {
            node = new DLBNode(ch);
        }

        if (node.ch == ch) {

            if (index == hotspot.length() - 1) {

                node.isTerminal = true;

                node.freq++;

                if (firstOccurrence) {
                    node.docFreq++;
                }

                if (atBegin) node.beginCount++;
                if (atMiddle) node.middleCount++;
                if (atEnd) node.endCount++;

            } else {

                node.child = insertHelper(node.child, hotspot, index + 1,
                        atBegin, atMiddle, atEnd, firstOccurrence);
            }

        } else {

            node.sibling = insertHelper(node.sibling, hotspot, index,
                    atBegin, atMiddle, atEnd, firstOccurrence);
        }

        return node;
    }

    @Override
    public Set<Hotspot> hotspotsIn(String candidatePassword) {

        if (candidatePassword == null)
            throw new IllegalArgumentException();

        candidatePassword = candidatePassword.trim();

        Map<String, HotspotBuilder> hotspotMap = new HashMap<>();

        int start = 0;

        while (start < candidatePassword.length()) {

            searchFromPosition(candidatePassword, start, hotspotMap);

            start++;
        }

        Set<Hotspot> results = new LinkedHashSet<>();

        for (HotspotBuilder b : hotspotMap.values()) {
            results.add(b.build());
        }

        return results;
    }

    private void searchFromPosition(String candidate, int startPos,
                                    Map<String, HotspotBuilder> hotspotMap) {

        DLBNode node = root;

        int i = startPos;

        while (i < candidate.length() && node != null) {

            char ch = candidate.charAt(i);

            node = findNode(node, ch);

            if (node == null) {
                break;
            }

            if (node.isTerminal) {

                String ngram = candidate.substring(startPos, i + 1);

                boolean atBegin = false;
                boolean atEnd = false;
                boolean atMiddle = false;

                if (startPos == 0) {
                    atBegin = true;
                } else if (i + 1 == candidate.length()) {
                    atEnd = true;
                } else {
                    atMiddle = true;
                }

                HotspotBuilder builder = hotspotMap.get(ngram);

                if (builder == null) {

                    builder = new HotspotBuilder(
                            ngram,
                            node.freq,
                            node.docFreq,
                            node.beginCount,
                            node.middleCount,
                            node.endCount
                    );

                    hotspotMap.put(ngram, builder);
                }

                if (atBegin) builder.candidateAtBegin = true;

                if (atMiddle) builder.candidateMiddleCount++;

                if (atEnd) builder.candidateAtEnd = true;
            }

            node = node.child;

            i++;
        }
    }

    private DLBNode findNode(DLBNode node, char ch) {

        DLBNode current = node;

        while (current != null) {

            if (current.ch == ch) {
                return current;
            }

            current = current.sibling;
        }

        return null;
    }

    private static class HotspotBuilder {

        String ngram;

        int freq;
        int docFreq;

        int beginCount;
        int middleCount;
        int endCount;

        boolean candidateAtBegin;
        int candidateMiddleCount;
        boolean candidateAtEnd;

        HotspotBuilder(String ngram, int freq, int docFreq,
                       int beginCount, int middleCount, int endCount) {

            this.ngram = ngram;
            this.freq = freq;
            this.docFreq = docFreq;

            this.beginCount = beginCount;
            this.middleCount = middleCount;
            this.endCount = endCount;
        }

        Hotspot build() {

            return new Hotspot(
                    ngram,
                    freq,
                    docFreq,
                    beginCount,
                    middleCount,
                    endCount,
                    candidateAtBegin,
                    candidateMiddleCount,
                    candidateAtEnd
            );
        }
    }
}
