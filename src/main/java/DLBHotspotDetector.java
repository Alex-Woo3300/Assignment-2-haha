import java.util.*;

public class DLBHotspotDetector implements HotspotDetector {

      // DLB Trie Node
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
            this.child = null;
            this.sibling = null;
            this.isTerminal = false;
            this.freq = 0;
            this.docFreq = 0;
            this.beginCount = 0;
            this.middleCount = 0;
            this.endCount = 0;
        }
    }
    
    private DLBNode root;
    
    public DLBHotspotDetector() {
        root = null;
    }

    @Override
    public void addLeakedPassword(String leakedPassword, int minN, int maxN) {
        if (leakedPassword == null)
            throw new IllegalArgumentException("null leakedPassword");
        if (minN < 1 || maxN < minN)
            throw new IllegalArgumentException("invalid n-range");

      

        // Trim whitespace
        leakedPassword = leakedPassword.trim();
        if (leakedPassword.isEmpty()) {
            return;
        }
        
        int len = leakedPassword.length();
        
        // Track which hotspots we've seen in this password for docFreq
        Set<String> seen = new HashSet<>();
        
        // Extract all hotspots of length minN to maxN
        for (int n = minN; n <= maxN; n++) {
            for (int i = 0; i <= len - n; i++) {
                String hotspot = leakedPassword.substring(i, i + n);
                
                // Determine position: beginning, middle, or end
                boolean atBegin = (i == 0);
                boolean atEnd = (i + n == len);
                boolean atMiddle = !atBegin && !atEnd;
                
                // Check if first occurrence in this password
                boolean firstOccurrence = !seen.contains(hotspot);
                if (firstOccurrence) {
                    seen.add(hotspot);
                }
                
                // Insert into DLB and update stats
                if (hotspot.isEmpty()) {
                    return;
                }
        
                root = insertHelper(root, hotspot, 0, atBegin, atMiddle, atEnd, firstOccurrence);
            }
        }
    }

     private DLBNode insertHelper(DLBNode node, String hotspot, int index,
                                 boolean atBegin, boolean atMiddle, boolean atEnd,
                                 boolean firstOccurrence) {
        char ch = hotspot.charAt(index);
        
        // If node is null, create a new node
        if (node == null) {
            node = new DLBNode(ch);
        }
        
        // If current character matches
        if (node.ch == ch) {
            // If last character, mark as terminal and update stats
            if (index == hotspot.length() - 1) {
                if (!node.isTerminal) {
                    node.isTerminal = true;
                }
                
                // Update statistics
                node.freq++;
                if (firstOccurrence) {
                    node.docFreq++;
                }
                if (atBegin) {
                    node.beginCount++;
                }
                if (atMiddle) {
                    node.middleCount++;
                }
                if (atEnd) {
                    node.endCount++;
                }
            } else {
                // Continue to next character via child
                node.child = insertHelper(node.child, hotspot, index + 1, 
                                         atBegin, atMiddle, atEnd, firstOccurrence);
            }
        } else {
            // Try sibling if character doesn't match
            node.sibling = insertHelper(node.sibling, hotspot, index, 
                                       atBegin, atMiddle, atEnd, firstOccurrence);
        }
        
        return node;
    }

    @Override
    public Set<Hotspot> hotspotsIn(String candidatePassword) {
        if (candidatePassword == null)
            throw new IllegalArgumentException("null candidatePassword");

    
        candidatePassword = candidatePassword.trim();
        Map<String, HotspotBuilder> hotspotMap = new HashMap<>();
        int len = candidatePassword.length();
        
        // For each starting position
        for (int i = 0; i < len; i++) {
            // Search for all hotspots starting at position i
            searchFromPosition(candidatePassword, i, hotspotMap);
        }
        
        // Convert map to Hotspot objects
        Set<Hotspot> results = new LinkedHashSet<>();
        for (HotspotBuilder builder : hotspotMap.values()) {
            results.add(builder.build());
        }
        
        return results;
    }

    private void searchFromPosition(String candidate, int startPos, 
                                    Map<String, HotspotBuilder> hotspotMap) {
        DLBNode node = root;
        int len = candidate.length();
        
        for (int i = startPos; i < len && node != null; i++) {
            char ch = candidate.charAt(i);
            
            // Find matching child/sibling
            node = findNode(node, ch);
            
            if (node == null) {
                break; // No more matches possible
            }
            
            // If terminal node, found hotspot
            if (node.isTerminal) {
                String ngram = candidate.substring(startPos, i + 1);
                
                // Determine position in candidate
                boolean atBegin = (startPos == 0);
                boolean atEnd = (i + 1 == len);
                boolean atMiddle = !atBegin && !atEnd;
                
                // Get or create builder for this ngram
                HotspotBuilder builder = hotspotMap.get(ngram);
                if (builder == null) {
                    builder = new HotspotBuilder(ngram, node.freq, node.docFreq,
                                                node.beginCount, node.middleCount, 
                                                node.endCount);
                    hotspotMap.put(ngram, builder);
                }
                
                if (atBegin) {
                    builder.candidateAtBegin = true;
                }
                if (atMiddle) {
                    builder.candidateMiddleCount++;
                }
                if (atEnd) {
                    builder.candidateAtEnd = true;
                }
            }
            
            // Move to child for next character
            node = node.child;
        }
    }
    
    //Find a node with the given character among siblings.
    private DLBNode findNode(DLBNode node, char ch) {
        while (node != null) {
            if (node.ch == ch) {
                return node;
            }
            node = node.sibling;
        }
        return null;
    }
    

     // Helper class to build Hotspot objects
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
            this.candidateAtBegin = false;
            this.candidateMiddleCount = 0;
            this.candidateAtEnd = false;
        }
        
        Hotspot build() {
            return new Hotspot(ngram, freq, docFreq, beginCount, middleCount, endCount,
                             candidateAtBegin, candidateMiddleCount, candidateAtEnd);
        }
    }
}
