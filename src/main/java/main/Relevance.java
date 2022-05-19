package main;

/**
 *  Вспомогательный класс для определения релевантности страницы для поискового запроса
 */
public class Relevance {
    private int pageId;
    private double relevance;

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public double getRelevance() {
        return relevance;
    }

    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }

    @Override
    public String toString() {
        return "Relevance{" +
                "pageId=" + pageId +
                ", relevance=" + relevance +
                '}';
    }
}
