package dokek.strose.edu.wifi_direct;


import java.io.Serializable;

public class Incident implements Serializable {
    private int incidentId;
    private String timeOfEvent;
    private int userId;
    private String title;
    private String description;
    private int incidentTypeId;
    private int severityTypeId;
    private String source;
    private String url;
    private String SourceId;
    private String Category;
//    private  int incidentTypeId;


    public Incident(){

    }

    public Incident(int incidentId, String description, String url, String source, int incidentTypeId, int severityTypeId, String timeOfEvent, String title, String SourceId, String Category){
        this.incidentId= incidentId;
        this.description=description;
        this.url = url;
        this.source=source;
        this.incidentTypeId = incidentTypeId;
        this.timeOfEvent=timeOfEvent;
        this.title=title;
        this.SourceId=SourceId;
        this.Category=Category;
        this.severityTypeId = severityTypeId;


    }
    public Incident(int incidentId){
        this.incidentId=incidentId;
    }

    //Getters and setters
    public int getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(int incidentId) {
        this.incidentId = incidentId;
    }

    public int getIncidentTypeId() {
        return incidentTypeId;
    }
    public void setIncidentTypeId(int incidentTypeId){
       this.incidentTypeId = incidentTypeId;
    }

    public int getSeverityTypeId() {
        return severityTypeId;
    }

    public void setSeverityTypeId(int severityTypeId) {
        this.severityTypeId = severityTypeId;
    }
//    public String getIncidentType() {
//        return incidentType;
//    }
//
//    public void setIncidentType(String incidentType) {
//        this.incidentType = incidentType;
//    }
//
//    public void setIncidentTypeId(int incidentTypeId) {
//        this.incidentTypeId = incidentTypeId;
//    }

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

    public String getTimeOfEvent() { return timeOfEvent; }

    public void setTimeOfEvent(String timeOfEvent) { this.timeOfEvent = timeOfEvent; }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceId() {
        return SourceId;
    }

    public void setSourceId(String sourceId) {
        SourceId = sourceId;
    }

    public String getCategory() {
        return Category;
    }

    public void setCategory(String category) {
        Category = category;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "Incident{" +
                "incidentId=" + incidentId +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", source='" + source + '\'' +
                ", IncidentTypeId='" + incidentTypeId + '\'' +
                ", category='" + Category + '\'' +
                ", url='" + url + '\'' +
                ", timeOfEvent='" + timeOfEvent + '\'' +
                ", sourceId='" + SourceId + '\'' +
                '}';
    }


}