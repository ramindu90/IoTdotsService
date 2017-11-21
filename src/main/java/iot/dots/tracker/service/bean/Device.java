package iot.dots.tracker.service.bean;

import java.io.Serializable;

/**
 * Created by ramindu on 3/18/17.
 */
public class Device implements Serializable {
    private String id;
    private String analyticsTableName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnalyticsTableName() {
        return analyticsTableName;
    }

    public void setAnalyticsTableName(String analyticsTableName) {
        this.analyticsTableName = analyticsTableName;
    }
}
