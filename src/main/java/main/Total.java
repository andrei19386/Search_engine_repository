package main;

/**
 * Вспомогательный класс для формирования информации о статистике сайта (в соответствии с техническим заданием)
 */
public class Total {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean isIndexing;

    public int getSites() {
        return sites;
    }

    public void setSites(int sites) {
        this.sites = sites;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int getLemmas() {
        return lemmas;
    }

    public void setLemmas(int lemmas) {
        this.lemmas = lemmas;
    }

    public boolean isIndexing() {
        return isIndexing;
    }

    public void setIndexing(boolean indexing) {
        isIndexing = indexing;
    }
}
