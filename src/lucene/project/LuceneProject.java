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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.store.RAMDirectory;

import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

/**
 *
 * @author rober
 */
public class LuceneProject {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        Analyzer analyzer = new StandardAnalyzer();

        // 1. create the index
//        Directory index = new SimpleFSDirectory(new File("test.lucene").toPath());
        Directory index = new RAMDirectory();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        IndexWriter w = new IndexWriter(index, config);

        String fn = "E:\\ClueWeb09_English_Sample.warc";
        File file = new File(fn);
        try {
            InputStream in = new FileInputStream(file);

            int records = 0;
            int errors = 0;

            WarcReader reader = WarcReaderFactory.getReader(in);
            WarcRecord record;

            while ((record = reader.getNextRecord()) != null) {

                String payload = IOUtils.toString(record.getPayload().getInputStreamComplete());

                payload = payload.replaceAll("\\s+", " ");
                Pattern p = Pattern.compile("<title>(.*?)</title>");
                Matcher m = p.matcher(payload);

                String title = m.find() ? m.group(1) : "NO TITLE";

                addDoc(w, title, payload);

                ++records;
            }

            System.out.println("--------------");
            System.out.println("       Records: " + records + " added");
            System.out.println("        Errors: " + errors + " found");
            reader.close();
            in.close();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

//        if (true) {
//            return;
//        }
//        addDoc(w, "Lucene in Action", "193398817");
//        addDoc(w, "Lucene for Dummies", "55320055Z");
//        addDoc(w, "Managing Gigabytes", "55063554A");
//        addDoc(w, "The Art of Computer Science", "9900333X");
        w.close();
        // 2. query
        String querystr = args.length > 0 ? args[0] : "test";

        // the "title" arg specifies the default field to use
        // when no field is explicitly specified in the query.
        Query q = new QueryParser("payload", analyzer).parse(querystr);

        // 3. search
        int hitsPerPage = 10;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        // 4. display results
        System.out.println("Found " + hits.length + " hits.");
        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
//            System.out.println((i + 1) + ". " + d.get("isbn") + "\t" + d.get("title") + "   " + d.toString());
            System.out.println(d.get("title"));
        }

        // reader can only be closed when there
        // is no need to access the documents any more.
        reader.close();
    }

    private static void addDoc(IndexWriter w, String title, String payload) throws IOException {
        Document doc = new Document();

        doc.add(new StringField("title", title, Field.Store.YES));
        doc.add(new TextField("payload", payload, Field.Store.YES));

        w.addDocument(doc);
    }

}
