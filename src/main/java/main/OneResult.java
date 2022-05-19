package main;

/**
 * Данный класс содержит структуру одиночного результата поиска
 */
public class OneResult implements Comparable<OneResult> {
    private  String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public double getRelevance() {
        return relevance;
    }

    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }

    @Override
    public int compareTo(OneResult o) {
        if(this.getRelevance() > o.getRelevance()){
            return -1;
        }
        if(this.getRelevance() < o.getRelevance()) {
            return 1;
        }
        return 0;
    }
}
