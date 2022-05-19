package main.model;

import javax.persistence.*;

@Entity
public class Lemma implements Comparable<Lemma> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String lemma;
    private int frequency;

    @Column(name = "site_id")
    private int siteId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    @Override
    public int compareTo(Lemma o) {
        if(this.getFrequency() > o.getFrequency()) {
            return 1;
        }
        if(this.getFrequency() < o.getFrequency()) {
            return -1;
        }
        return this.getLemma().compareTo(o.getLemma());
    }
}
