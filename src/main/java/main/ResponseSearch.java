package main;

import java.util.List;

/**
 *  Класс формирования структуры ответа на поисковый запрос
 */
public class ResponseSearch {
    private boolean result;
    private int count;
    private String error;
    private List<OneResult> data;

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<OneResult> getData() {
        return data;
    }

    public void setData(List<OneResult> data) {
        this.data = data;
    }
}
