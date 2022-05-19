package main;

/**
 * Этот класс нужен для разбиения страницы на блоки по их значимости в соответствии с таблицей field
 */
public class Block {
    private String blockString;
    private double blockWeight;

    public String getBlockString() {
        return blockString;
    }

    public void setBlockString(String blockString) {
        this.blockString = blockString;
    }

    public double getBlockWeight() {
        return blockWeight;
    }

    public void setBlockWeight(double blockWeight) {
        this.blockWeight = blockWeight;
    }
}
