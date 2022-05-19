package main;

import main.model.SiteType;

/**
 * Этот класс нужен для формирования структуры ответа для страницы статистики
 */
public class Detailed {
    private String url;
    private String name;
    private SiteType status;
    private long statusTime;
    private String error;
    private int code;
    private int pages;
    private  int lemmas;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public SiteType getStatus() {
        return status;
    }

    public void setStatus(SiteType status) {
        this.status = status;
    }

    public long getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(long statusTime) {
        this.statusTime = statusTime;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
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
}
