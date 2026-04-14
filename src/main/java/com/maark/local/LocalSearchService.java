package com.maark.local;

import com.maark.model.SearchResult;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;

import java.util.ArrayList;
import java.util.List;

public class LocalSearchService {

    private final Directory directory;
    private final StandardAnalyzer analyzer;

    public LocalSearchService(Directory directory, StandardAnalyzer analyzer) {
        this.directory = directory;
        this.analyzer = analyzer;
    }

    public List<SearchResult> search(String queryText) {
        List<SearchResult> results = new ArrayList<>();
        try {
            if (!DirectoryReader.indexExists(directory)) {
                return results;
            }

            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                String[] fields = {"title", "content"};
                MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
                parser.setDefaultOperator(QueryParser.Operator.OR);

                String safeQuery = QueryParser.escape(queryText);
                Query query = parser.parse(safeQuery);

                TopDocs topDocs = searcher.search(query, 10);
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    org.apache.lucene.document.Document doc = searcher.storedFields().document(scoreDoc.doc);
                    String title = doc.get("title");
                    String content = doc.get("content");
                    String url = doc.get("url");

                    String snippet = content != null && content.length() > 150
                            ? content.substring(0, 150) + "..."
                            : content;

                    results.add(new SearchResult(title, url != null ? url : "", snippet != null ? snippet : "", "Local"));
                }
            }
        } catch (Exception e) {
            System.err.println("LocalSearchService error: " + e.getMessage());
        }
        return results;
    }
}
