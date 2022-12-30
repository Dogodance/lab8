import java.net.MalformedURLException;
import java.net.URL;

public class URLDepthPair {
    private String url; // url ��������
    private int depth; // Ÿ �������
    public static final String URL_PREFIX = "http://"; // �������
    // ������������ ��������� �������(����� �� ������ �����, ���� ���-�� ������ �� ���)
    public static final int MAX_DEPTH_VALUE = 100;
    public URLDepthPair(String url, int depth) throws MalformedURLException {
        // ���� ������� ����� �� ������� �� ������� ������
        if (depth < 0 || depth > MAX_DEPTH_VALUE) {
            throw new IllegalArgumentException("Error limits of depth");
        }
        this.url = url; // ����� ������ url
        this.depth = depth; // � ������ �������
    }

    public String toString() {
        return "�������: " + this.depth + ", URL: " + this.url; // ����� ������ � ���������� ��������
    }
    // ��������� ����������
    // ��������� �����
    public String getHostName() {
        try {
            URL url = new URL(this.url);
            return url.getHost();
        }
        // ���� ��� ����� �� ����� ��������������, ��������� ������
        catch (MalformedURLException e) {
            System.err.println("MalformedURLException: " + e.getMessage());
            return null;
        }
    }
    // ��������� ���� �� ��������
    public String getPagePath() {
        try {
            URL url = new URL(this.url);
            return url.getPath();
        }
        // ���� ���� �� ����� ��������������, ��������� ������
        catch (MalformedURLException e) {
            System.err.println("MalformedURLException: " + e.getMessage());
            return null;
        }
    }
    // ��������� URL ��������
    public String getURL() {
        return this.url;
    }

    // ��������� �������
    public int getDepth() {
        return this.depth;
    }
}