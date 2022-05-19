package main.model;

import javax.persistence.*;
import java.util.Objects;

@Entity
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "page_id")
    private int pageId;

    @Column(name = "lemma_id")
    private int lemmaId;

    private double rank;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public int getLemmaId() {
        return lemmaId;
    }

    public void setLemmaId(int lemmaId) {
        this.lemmaId = lemmaId;
    }

    public double getRank() {
        return rank;
    }

    public void setRank(double rank) {
        this.rank = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Index)) return false;
        Index index = (Index) o;
        return getPageId() == index.getPageId() && getLemmaId() == index.getLemmaId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getPageId(), getLemmaId(), getRank());
    }
}
