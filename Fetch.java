/**
 * Module to support the fetching of HTML files of the given endpoints.
 * Supports HTTP and HTTPS requests.
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.lang.StringBuilder;
import java.util.Scanner;

public class Fetch {
    private static final String PRE_ERROR = "Error encountered when parsing %s: %s";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private InputStream stream;
    private URLConnection urlConnection;
    private boolean wantsMetadata;

    /**
     * Will accept in constructor any modifying arguments to fetch. Supports metadata.
     * 
     * @param wantsMetadata supported command line argument to get metadata
     */
    public Fetch(boolean wantsMetadata) {
        this.wantsMetadata = wantsMetadata;
    }

    /**
     * Creates supported directories to save html files.
     */
    private static void createDirectories() {
        File http = new File("http");
        File https = new File("https");
        if (!http.exists()) {
            http.mkdirs();
        }
        if (!https.exists()) {
            https.mkdirs();
        }
    }

    /**
     * helper function that is meant to get the file name based on protocol.
     *
     * @param endpoint the URL specified by the user.
     * 
     * @return The file name based on the protocol.
     */
    private static String getUrlFileName(String endpoint) {
        String urlFileName = endpoint + ".html";
        // parse what protocol the user has indicated.
        if (endpoint.startsWith(HTTP)) {
            urlFileName = "http" + File.separator + urlFileName.substring(HTTP.length());
        } else if (endpoint.startsWith(HTTPS)) {
            urlFileName = "https" + File.separator + urlFileName.substring(HTTPS.length());
        }
        return urlFileName;
    }

    /**
     * helper function that is meant to get the site name based on protocol
     *
     * @param endpoint the URL specified by the user.
     * 
     * @return The site name based on the protocol.
     */
    private static String getSite(String endpoint) {
        String site = endpoint;
        if (endpoint.startsWith(HTTP)) {
            site = endpoint.substring(HTTP.length());
        } else if (endpoint.startsWith(HTTPS)) {
            site = endpoint.substring(HTTPS.length());
        }
        return site;
    }

    /**
     * helper function that will print out metadata when requested.
     *
     * @param htmlDoc A string representation of a HTML document
     * @param lastFetch the UTC based local date and time.
     * @param site the site name based on protocol
     */
    private void printMetadata(String htmlDoc, LocalDateTime lastFetch, String site) {
        if (this.wantsMetadata) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM yyyy HH:mm");
            Document doc = Jsoup.parse(htmlDoc);
            int links = doc.select("a").size(); // count a tags
            int images = doc.select("img").size(); // count img tags
            System.out.println(String.format("site: %s", site));
            System.out.println(String.format("num_links: %d", links));
            System.out.println(String.format("images: %d", images));
            System.out.println(String.format("last_fetch: %s UTC", lastFetch.format(formatter)));
        }
    }

    /**
     * helper function that is meant to get the file name based on protocol.
     *
     * @param endpoint the URL specified by the user.
     * 
     * @return Whether or not it was successful at reading from archives.
     */
    private boolean readFromArchives(String endpoint) {
        // parse what protocol the user has indicated.
        String urlFileName = getUrlFileName(endpoint);
        File archivedFile = new File(urlFileName);
        if (archivedFile.exists()) {
            // get the time the call was made in case the user wants metadata
            LocalDateTime lastFetch = LocalDateTime.now(ZoneOffset.UTC);
            try {
                // Attempt to build and read the contents of the HTML file.
                StringBuilder htmlContents = new StringBuilder((int) archivedFile.length());        
                try (Scanner scanner = new Scanner(archivedFile)) {
                    while(scanner.hasNextLine()) {
                        htmlContents.append(scanner.nextLine() + System.lineSeparator());
                    }
                    printMetadata(htmlContents.toString(), lastFetch, getSite(endpoint));
                    System.out.println(String.format("Successfully read file %s!\n", urlFileName));
                    return true;
                }
            } catch (IOException ioe) {
                System.out.println(String.format(
                    "Error encountered when parsing archived file %s: %s", urlFileName, ioe.getMessage()));
            }
        }
        return false;
    }
    
    /**
     * Primary function that handles the retrieval of the HTML files.
     *
     * @param endpoint a URL to retrieve the HTML file from
     */
    private void getHtmlFile(String endpoint) {
        // parse what protocol the user has indicated.
        String urlFileName = getUrlFileName(endpoint);
        String site = getSite(endpoint);
        try {
            urlConnection = new URL(endpoint).openConnection();
            // get the time the call was made in case the user wants metadata
            LocalDateTime lastFetch = LocalDateTime.now(ZoneOffset.UTC);
            stream = urlConnection.getInputStream();
            // read the contents of the data retrieved.
            int curr = 0;
            StringBuffer htmlContents = new StringBuffer();
            while ((curr = stream.read()) != -1) {
                htmlContents.append((char) curr);
            }
            // write the read contents into file.
            File htmlFile = new File(urlFileName);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(htmlFile))) {
                writer.write(htmlContents.toString());
            }
            // Print out the metadata per requirements.
            printMetadata(htmlContents.toString(), lastFetch, getSite(endpoint));
        } catch (MalformedURLException mee) {
            System.out.println(String.format(PRE_ERROR, endpoint, mee.getMessage()));
        } catch (IOException ioe) {
            System.out.println(String.format(PRE_ERROR, endpoint, ioe.getMessage()));
        }
        System.out.println(String.format("Successfully wrote file %s!\n", urlFileName));
    }
    
    /**
     * The primary driver of the application.
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            // Create directories if not created already.
            Fetch.createDirectories();
            int arg = 0;
            boolean enableArg = false;
            if (args[0].startsWith("--")) {
                if (args[0].equals("--metadata")) {
                    enableArg = true;
                } else {
                    System.out.println("Unrecognized argument, will process URLs with default configuration.");
                }
                // only one argument supported.
                arg = 1;
            }
            Fetch fetch = new Fetch(enableArg);
            for (; arg < args.length; arg++) {
                String endpoint = args[arg];
                System.out.println(String.format("Getting HTML file at %s...", endpoint));
                // Attempt to read from disk if endpoint already exists.
                if (!fetch.readFromArchives(endpoint)) {
                    fetch.getHtmlFile(endpoint);
                }
            }
        } else {
            System.out.println("Error running fetch: Add URLs to fetch! e.g. java Fetch <url1> <url2> ...");
        }
    }
}
