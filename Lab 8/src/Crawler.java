import java.lang.Exception;
import java.net.MalformedURLException;
import java.net.*;
import java.io.*;
import java.util.LinkedList;

public class Crawler {
    public static final int HTTP_PORT = 80; // Порт подключения
    public static final String HOOK_REF = "<a href=\""; // Адрес рука
    public static final String BAD_REQUEST_LINE = "HTTP/1.1 400 Bad Request"; // Header HTTP файла
    public static final int NUM_OF_DEFAULT_THREADS = 4; // Дефолтовое количество потоков
    public static final String URL_STANDART = "http://www.gtp-tabs.ru/";
    int depth;    // Глубина поиска
    public int numThreads; // Количество потоков
    // Точка входа
    public static void main (String[] args) {
        Crawler crawler = new Crawler(); // Иницализируем объект класса
        crawler.numThreads = Crawler.NUM_OF_DEFAULT_THREADS; // Устанавливаем дефолтовое количество потоков
        URLDepthPair firstURL = crawler.setSite(URL_STANDART, 2); // Задаем сайт и глубину
        URLPool pool = new URLPool(crawler.depth);
        pool.put(firstURL);
        int initialActive = Thread.activeCount(); //
        //Работаем пока есть НЕ ожидающие потоки
        while (pool.getWaitThreads() != crawler.numThreads) {
            if (Thread.activeCount() - initialActive < crawler.numThreads) { // Проверяем количество потоков, и если можно, создаем еще один
                //Создаём Test с общим пулом
                Test crawlerTask = new Test(pool);
                // Создаем новый поток
                new Thread(crawlerTask).start();
            }
        }
        crawler.getSites(pool);
        System.exit(0);
    }
    public static LinkedList<URLDepthPair>  parsePage(URLDepthPair element) {
        LinkedList<URLDepthPair> listOfUrl = new LinkedList<URLDepthPair>(); //Создаем список, для хранения Url
        Socket socket = null; // Инициализируем сокет по которому подключаемся пустым значением
        try {
            // Открываем сокет установив имя хоста и порт 80
            socket = new Socket(element.getHostName(), HTTP_PORT);
            try {
                socket.setSoTimeout(5000); // Ставим таймер на 5 секунд и проверяем таким образом сокет на ошибки
            }
            catch (SocketException exc) {
                System.err.println("SocketException: " + exc.getMessage());
                return null;
            }
            // Для отправки запросов на сервер используем запись данных в сокет
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // Отправка запроса на получение html-страницы
            out.println("GET " + element.getPagePath() + " HTTP/1.1"); // Ставим метод GET и заголовок
            out.println("Host: " + element.getHostName()); // Устанавливаем хоста
            out.println("Connection: close"); // Пишем, что соединение закрыто
            out.println(""); // Перевод строки
            // Получение ответа, сохраняем через буферизированный считыватель
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Проверка на bad request
            String line = in.readLine();
            if (line.startsWith(BAD_REQUEST_LINE)) {
                System.out.println(line + "\n"); // Выводим проблему запроса в консоли
                return null; // Заканчиваем работу потока
            }
            int strCount = 0;
            // Чтение основного файла
            while(line != null) {
                try {
                    //Извлечнение строки из html-кода
                    line = in.readLine();
                    strCount++;
                    // Извлечение ссылки из тэга, если она там есть, если нет, идём к следующей строке
                    String url = getURLFromHTMLTag(line);
                    // Если ссылки нет, то заканчиваем шаг цикла
                    if (url == null) continue;
                    //Пропускаем https
                    if (url.startsWith("https://")) {
                        continue;
                    }
                    //Добавляем http
                    if (url.startsWith("http://")){
                        String newURL = cut(url);
                        addNewURL(newURL, element.getDepth() + 1, listOfUrl);
                    } else if (url.startsWith("/")){
                        //Обрезаем и добавляем всё остальное
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
        // Обработка неопознанного хоста
        catch (UnknownHostException e) {
            System.out.println("UnknownHostException in: " + element.getURL());
        }
        // Обработка ошибок ввода/Вывода
        catch (IOException e) {
            e.printStackTrace();
        }
        //
    return listOfUrl;
    }


    // Парсер URL из HTML тега
    public static String getURLFromHTMLTag(String line) {
        if (line.indexOf(Crawler.HOOK_REF) == -1) return null; // Если тег начинается не на a link, то не парсим
        // Иначе берем все после  <a link до слеша закрытия
        int indexStart = line.indexOf(Crawler.HOOK_REF) + Crawler.HOOK_REF.length();
        int indexEnd = line.indexOf("\"", indexStart);
        // Если слеша закрытия нет, не парсим
        if (indexEnd == -1) return null;
        return line.substring(indexStart, indexEnd); // Берем подстроку без окончания тега и начал
    }
    public static String cut(String url) {
        int index = url.lastIndexOf("#");
        if (index == -1) return url;
        return url.substring(0, index);
    }
    // Добавлеие нового URL
    private static void  addNewURL(String url, int depth, LinkedList<URLDepthPair> listOfUrl) {
        URLDepthPair newURL = null; // Инициализируем URL
        try{
            newURL = new URLDepthPair(url, depth); // Пытаемся инициализировать новый URL
        } catch (MalformedURLException e) { // Если произошла ошибка, выведем её сообщение
            e.printStackTrace();
        }
        listOfUrl.addLast(newURL); // Если ошибок не было, то добавим URL в список не посещенных
    }
    //Вывод результата
    public void getSites(URLPool pool) {
        System.out.println("---------------------------------------\n Посещенные сайты:");
        LinkedList<URLDepthPair> list = pool.getWatchedList();
        // Обходим все посещенные сайты и печатаем их, увеличивая счетчик на 1
        for (URLDepthPair page : list) {
            System.out.println(page.toString());
        }

        list = pool.getBlockedList();
        System.out.println("\nНе посещенные сайты: ");
        // Обходим все не посещенные сайты и печатаем их, увеличивая счетчик на 1
        for (URLDepthPair page : list) {
            System.out.println(page.toString());
        }
    }
    // Задание первого сайта
    public URLDepthPair setSite(String url, int depth){
        // Максимальная глубина
        this.depth = depth;
        // Переменная для корневого сайта
        URLDepthPair firstOne;
        try {
            // Передаем сайт как первый в глубине и ставим её 0
            firstOne = new URLDepthPair(url, 0);
        }catch (MalformedURLException e){ // Обрабатываем ошибку, если URL не смог сформироваться
            e.printStackTrace(); // Выводим ошибку
            return null;
        }
        return firstOne; // Добавляем в посещенные сайты главную страницу
    }

    public static void showResults(URLDepthPair element, LinkedList<URLDepthPair> listOfUrl) {
        System.out.println("---Выполнение потока---");
        System.out.println("Страница посещения: " + element.getURL());

        System.out.println("Найденные страницы:");
        for (URLDepthPair pair : listOfUrl) {
            System.out.println(pair.toString());
        }
        System.out.println("-----Конец потока-----");
    }

}