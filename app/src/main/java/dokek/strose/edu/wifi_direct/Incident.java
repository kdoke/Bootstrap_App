package dokek.strose.edu.wifi_direct;

import java.io.Serializable;

public class Incident implements Serializable {

    private int id;
    private String description;
    private String source;
    private String url;

    public Incident(int id, String description, String source){
        this.id = id;
        this.description = description;
        this.source = source;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
