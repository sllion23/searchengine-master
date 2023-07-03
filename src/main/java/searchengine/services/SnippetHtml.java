package searchengine.services;

import java.util.*;
import java.util.stream.Collectors;

public class SnippetHtml {

    private final LemmaFinder lemmaFinder;

    private record WordPosition(String word, Integer position) {
    }

    public static final int MAX_LENGTH = 200;

    public SnippetHtml(LemmaFinder lemmaFinder){
        this.lemmaFinder = lemmaFinder;
    }
    
    public String getSnippet(String query, String content) {
        String text = content.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я])", " ")
                .replaceAll("\\s+", " ")
                .trim();
        List<WordPosition> wordPositions = getPositionList(query, text);
        String snippet = "";
        int minPosition = wordPositions.get(0).position();
        int maxPosition = wordPositions.get(wordPositions.size() - 1).position();
        int delta = maxPosition - minPosition;
        if (delta < MAX_LENGTH) {
            int addCharCount = (MAX_LENGTH - delta) / 2;
            int addToEnd = minPosition - addCharCount < 0 ? addCharCount - minPosition : 0;
            int addToStart = maxPosition + addCharCount > text.length() ? (maxPosition + addCharCount) - text.length() : 0;
            int start = Math.max(0, minPosition - addCharCount - addToStart);
            int end = Math.min(text.length(), maxPosition + addCharCount + addToEnd);
            snippet = text.substring(start, end);
        } else {
            int addCharCount = MAX_LENGTH / wordPositions.size() / 2;
            for (WordPosition p : wordPositions) {
                int start = Math.max(0, p.position() - addCharCount);
                int end = Math.min(text.length(), p.position() + addCharCount);
                String s = text.substring(start, end);
                s = end == text.length() ? s : s + "...";
                snippet = snippet + s;
            }
        }
        for (WordPosition p : wordPositions) {
            snippet = snippet.replaceAll(p.word(), "<b>" + p.word() + "</b>");
        }
        return snippet;
    }

    private  List<WordPosition> getPositionList(String query, String content) {
        Set<String> searchWords = Arrays.stream(query.split("\\s+")).collect(Collectors.toSet());
        String[] words = content.split("\\s+");
        List<WordPosition> position = new ArrayList<>();
        int len = 0;
        for (String word : words) {
            String s = lemmaFinder.getNormalWord(word);
            if (s != null && searchWords.contains(s)) {
                position.add(new WordPosition(word, len));
                searchWords.remove(s);
            }
            if (searchWords.size() == 0) {
                break;
            }
            len += word.length() + 1;
        }
        return position;
    }
}
