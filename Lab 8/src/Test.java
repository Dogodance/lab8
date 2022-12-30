import java.util.*;

public class Test implements Runnable {
    private URLDepthPair element;
    private URLPool myPool;
    public Test(URLPool pool) {
        this.myPool = pool;
    }
    public void run() {
        element = myPool.get();
        LinkedList<URLDepthPair> linksList = new LinkedList<URLDepthPair>();
        linksList = Crawler.parsePage(element);
        Crawler.showResults(element, linksList);
        for (URLDepthPair pair: linksList) {
            myPool.put(pair);
        }

    }
}