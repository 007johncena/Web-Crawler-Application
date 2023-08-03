package Crawlers;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class invertedIndex {
	
	public class InvertedIndex {
	    private Map<String, Set<String>> invertedIndex;

	    public InvertedIndex() {
	        invertedIndex = new HashMap<>();
	    }

	    public void addToIndex(String token, String url) {
	        invertedIndex.computeIfAbsent(token, k -> new HashSet<>()).add(url);
	    }

	    public Set<String> getUrlsForToken(String token) {
	        return invertedIndex.getOrDefault(token, new HashSet<>());
	    }

}
}
