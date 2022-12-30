import java.lang.Exception;
import java.net.MalformedURLException;
import java.net.*;
import java.io.*;
import java.util.LinkedList;

public class Crawler {
    public static final int HTTP_PORT = 80; // ���� �����������
    public static final String HOOK_REF = "<a href=\""; // ����� ����
    public static final String BAD_REQUEST_LINE = "HTTP/1.1 400 Bad Request"; // Header HTTP �����
    public static final int NUM_OF_DEFAULT_THREADS = 4; // ���������� ���������� �������
    public static final String URL_STANDART = "http://www.gtp-tabs.ru/";
    int depth;    // ������� ������
    public int numThreads; // ���������� �������
    // ����� �����
    public static void main (String[] args) {
        Crawler crawler = new Crawler(); // ������������� ������ ������
        crawler.numThreads = Crawler.NUM_OF_DEFAULT_THREADS; // ������������� ���������� ���������� �������
        URLDepthPair firstURL = crawler.setSite(URL_STANDART, 2); // ������ ���� � �������
        URLPool pool = new URLPool(crawler.depth);
        pool.put(firstURL);
        int initialActive = Thread.activeCount(); //
        //�������� ���� ���� �� ��������� ������
        while (pool.getWaitThreads() != crawler.numThreads) {
            if (Thread.activeCount() - initialActive < crawler.numThreads) { // ��������� ���������� �������, � ���� �����, ������� ��� ����
                //������ Test � ����� �����
                Test crawlerTask = new Test(pool);
                // ������� ����� �����
                new Thread(crawlerTask).start();
            }
        }
        crawler.getSites(pool);
        System.exit(0);
    }
    public static LinkedList<URLDepthPair>  parsePage(URLDepthPair element) {
        LinkedList<URLDepthPair> listOfUrl = new LinkedList<URLDepthPair>(); //������� ������, ��� �������� Url
        Socket socket = null; // �������������� ����� �� �������� ������������ ������ ���������
        try {
            // ��������� ����� ��������� ��� ����� � ���� 80
            socket = new Socket(element.getHostName(), HTTP_PORT);
            try {
                socket.setSoTimeout(5000); // ������ ������ �� 5 ������ � ��������� ����� ������� ����� �� ������
            }
            catch (SocketException exc) {
                System.err.println("SocketException: " + exc.getMessage());
                return null;
            }
            // ��� �������� �������� �� ������ ���������� ������ ������ � �����
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // �������� ������� �� ��������� html-��������
            out.println("GET " + element.getPagePath() + " HTTP/1.1"); // ������ ����� GET � ���������
            out.println("Host: " + element.getHostName()); // ������������� �����
            out.println("Connection: close"); // �����, ��� ���������� �������
            out.println(""); // ������� ������
            // ��������� ������, ��������� ����� ���������������� �����������
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // �������� �� bad request
            String line = in.readLine();
            if (line.startsWith(BAD_REQUEST_LINE)) {
                System.out.println(line + "\n"); // ������� �������� ������� � �������
                return null; // ����������� ������ ������
            }
            int strCount = 0;
            // ������ ��������� �����
            while(line != null) {
                try {
                    //����������� ������ �� html-����
                    line = in.readLine();
                    strCount++;
                    // ���������� ������ �� ����, ���� ��� ��� ����, ���� ���, ��� � ��������� ������
                    String url = getURLFromHTMLTag(line);
                    // ���� ������ ���, �� ����������� ��� �����
                    if (url == null) continue;
                    //���������� https
                    if (url.startsWith("https://")) {
                        continue;
                    }
                    //��������� http
                    if (url.startsWith("http://")){
                        String newURL = cut(url);
                        addNewURL(newURL, element.getDepth() + 1, listOfUrl);
                    } else if (url.startsWith("/")){
                        //�������� � ��������� �� ���������
                        int index = element.getURL().lastIndexOf("/");
                        String newURL = element.getURL().substring(0, index) + cut(url);
                        addNewURL(newURL, element.getDepth() + 1, listOfUrl);
                    }
                }
                catch (Exception e) {
                    break;
                }
            }
        }
        // ��������� ������������� �����
        catch (UnknownHostException e) {
            System.out.println("UnknownHostException in: " + element.getURL());
        }
        // ��������� ������ �����/������
        catch (IOException e) {
            e.printStackTrace();
        }
        //
    return listOfUrl;
    }


    // ������ URL �� HTML ����
    public static String getURLFromHTMLTag(String line) {
        if (line.indexOf(Crawler.HOOK_REF) == -1) return null; // ���� ��� ���������� �� �� a link, �� �� ������
        // ����� ����� ��� �����  <a link �� ����� ��������
        int indexStart = line.indexOf(Crawler.HOOK_REF) + Crawler.HOOK_REF.length();
        int indexEnd = line.indexOf("\"", indexStart);
        // ���� ����� �������� ���, �� ������
        if (indexEnd == -1) return null;
        return line.substring(indexStart, indexEnd); // ����� ��������� ��� ��������� ���� � �����
    }
    public static String cut(String url) {
        int index = url.lastIndexOf("#");
        if (index == -1) return url;
        return url.substring(0, index);
    }
    // ��������� ������ URL
    private static void  addNewURL(String url, int depth, LinkedList<URLDepthPair> listOfUrl) {
        URLDepthPair newURL = null; // �������������� URL
        try{
            newURL = new URLDepthPair(url, depth); // �������� ���������������� ����� URL
        } catch (MalformedURLException e) { // ���� ��������� ������, ������� � ���������
            e.printStackTrace();
        }
        listOfUrl.addLast(newURL); // ���� ������ �� ����, �� ������� URL � ������ �� ����������
    }
    //����� ����������
    public void getSites(URLPool pool) {
        System.out.println("---------------------------------------\n ���������� �����:");
        LinkedList<URLDepthPair> list = pool.getWatchedList();
        // ������� ��� ���������� ����� � �������� ��, ���������� ������� �� 1
        for (URLDepthPair page : list) {
            System.out.println(page.toString());
        }

        list = pool.getBlockedList();
        System.out.println("\n�� ���������� �����: ");
        // ������� ��� �� ���������� ����� � �������� ��, ���������� ������� �� 1
        for (URLDepthPair page : list) {
            System.out.println(page.toString());
        }
    }
    // ������� ������� �����
    public URLDepthPair setSite(String url, int depth){
        // ������������ �������
        this.depth = depth;
        // ���������� ��� ��������� �����
        URLDepthPair firstOne;
        try {
            // �������� ���� ��� ������ � ������� � ������ � 0
            firstOne = new URLDepthPair(url, 0);
        }catch (MalformedURLException e){ // ������������ ������, ���� URL �� ���� ��������������
            e.printStackTrace(); // ������� ������
            return null;
        }
        return firstOne; // ��������� � ���������� ����� ������� ��������
    }

    public static void showResults(URLDepthPair element, LinkedList<URLDepthPair> listOfUrl) {
        System.out.println("---���������� ������---");
        System.out.println("�������� ���������: " + element.getURL());

        System.out.println("��������� ��������:");
        for (URLDepthPair pair : listOfUrl) {
            System.out.println(pair.toString());
        }
        System.out.println("-----����� ������-----");
    }

}