import java.net.URI;


public class TestUriParser {

    public static void main(String[] args) throws Exception {
        String confirmCode ="fake code";
        String scheme = "http";
        String host = "10.0.2.2";

        URI uri = new URI(
                scheme,
                null,
                host,
                8080,
                "/helloapp/customer/api/apply-campaign",
                String.format("userId=%d&campaignId=%d&confirmerCode=%s", 12L, 10L, confirmCode),
                null
        );
        String url = uri.toURL().toString();
        System.out.println("url = " + url);
    }
}
