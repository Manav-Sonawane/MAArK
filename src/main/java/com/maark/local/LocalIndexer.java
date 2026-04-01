package com.maark.local;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.List;

public class LocalIndexer {

    private final Directory directory;
    private final StandardAnalyzer analyzer;

    public LocalIndexer(Directory directory) {
        this.directory = directory;
        this.analyzer = new StandardAnalyzer();
    }

    public static Directory createInMemoryDirectory() {
        return new ByteBuffersDirectory();
    }

    public void indexDocuments(List<LocalDocument> documents) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE); // Overwrite on each call

        try (IndexWriter writer = new IndexWriter(directory, config)) {
            for (LocalDocument doc : documents) {
                Document luceneDoc = new Document();
                luceneDoc.add(new TextField("title", doc.getTitle(), Field.Store.YES));
                luceneDoc.add(new TextField("content", doc.getContent(), Field.Store.YES));
                luceneDoc.add(new StoredField("url", doc.getUrl()));
                writer.addDocument(luceneDoc);
            }
            writer.commit();
        }
    }

    public Directory getDirectory() {
        return directory;
    }

    public StandardAnalyzer getAnalyzer() {
        return analyzer;
    }
}
