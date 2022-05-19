package main;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для полной структуры статистики (в соответствии с техническим заданием)
 */
public class StatisticsInfo {
    private Total total;
    private List<Detailed> detailed = new ArrayList<>();

    public Total getTotal() {
        return total;
    }

    public void setTotal(Total total) {
        this.total = total;
    }

    public List<Detailed> getDetailed() {
        return detailed;
    }

    public void setDetailed(List<Detailed> detailed) {
        this.detailed = detailed;
    }
}
