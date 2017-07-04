/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lucene.project;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.store.SimpleFSDirectory;

import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

/**
 *
 * @author rober
 */
public class LuceneProject {

    private static void readWarc(File file, IndexWriter w) {
        try {
            InputStream in = new FileInputStream(file);

            WarcReader reader = WarcReaderFactory.getReader(in);
            WarcRecord record;

            while ((record = reader.getNextRecord()) != null) {

                String payload = IOUtils.toString(record.getPayload().getInputStreamComplete());

                payload = payload.replaceAll("\\s+", " ");
                Pattern p = Pattern.compile("<title>(.*?)</title>");
                Matcher m = p.matcher(payload);

                String title = m.find() ? m.group(1) : "NO TITLE";

                addDocToIndex(w, title, payload);
            }
            reader.close();
            in.close();
        } catch (Exception e) {
        }
    }

    private static Document getDocumentById(int id, Directory index) {
        try {
            IndexReader reader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);
            return searcher.doc(id);
        } catch (IOException ex) {
            Logger.getLogger(LuceneProject.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private static void addDocToIndex(IndexWriter w, String title, String payload) throws IOException {
        Document doc = new Document();

        doc.add(new StringField("title", title, Field.Store.YES));
        doc.add(new TextField("payload", payload, Field.Store.YES));

        w.addDocument(doc);
    }

    private static ScoreDoc[] query(String query, Analyzer analyzer, Directory index) {
        try {

            // the "title" arg specifies the default field to use
            // when no field is explicitly specified in the query.
            Query q = new QueryParser("payload", analyzer).parse(query);

            // 3. search
            int hitsPerPage = 10;
            IndexReader reader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs docs = searcher.search(q, hitsPerPage);
            reader.close();
            return docs.scoreDocs;

            // reader can only be closed when there
            // is no need to access the documents any more.
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {

        if (args.length == 2) {

            Analyzer analyzer = new StandardAnalyzer();
            Directory index;
            index = new SimpleFSDirectory(new File("lucene.index").toPath());

            switch (args[0]) {
                case "addwarc":
                    try {
                        IndexWriterConfig config = new IndexWriterConfig(analyzer);
                        IndexWriter w = new IndexWriter(index, config);
                        String fn = args[1];
                        File file = new File(fn);
                        readWarc(file, w);
                        w.close();
                        System.out.println("Success");
                    } catch (Exception e) {
                        Logger.getLogger(LuceneProject.class.getName()).log(Level.SEVERE, null, e);
                        System.err.println("Error");
                    }
                    break;
                case "query":
                    ScoreDoc[] hits = query(args[1], analyzer, index);
                    if(hits != null){
                        System.out.println("Found " + hits.length + " hits.");
                        for (int i = 0; i < hits.length; ++i) {
                            ScoreDoc d = hits[i];
                            Document doc = getDocumentById(i, index);
                            System.out.println(d.toString() + " - " + doc.get("title"));
                        }
                    }else{
                        System.out.println("No Results");
                    }
                    break;
                case "getDoc":
                    Document doc = getDocumentById(Integer.parseInt(args[1]), index);
                    System.out.println(doc.get("payload"));
                    break;
                default:
                    System.out.println("possible commands:"
                            + "\naddwarc [warc file]"
                            + "\nquery [query string]"
                            + "\ntry again...");
                    break;
            }
            
            index.close();
            analyzer.close();
        } else {
            return;
        }

        if (true) {
            return;
        }
    }

}
